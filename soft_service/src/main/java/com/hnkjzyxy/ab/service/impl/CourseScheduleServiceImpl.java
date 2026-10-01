package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.client.OaApiClient;
import com.hnkjzyxy.ab.config.ScheduleSyncProperties;
import com.hnkjzyxy.ab.mapper.CourseScheduleMapper;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.model.CourseScheduleSyncResult;
import com.hnkjzyxy.ab.service.CourseScheduleService;
import com.hnkjzyxy.ab.utils.RedisLockUtils;
import com.hnkjzyxy.ab.utils.TransactionalMysqlLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 课程安排Service实现类
 * <p>
 * 课表同步采用「先拉取 → 再校验 → 最后事务内整体替换」的顺序，
 * 任何一步失败都不会破坏正式表数据，替代原先「先 TRUNCATE 再拉取」的危险实现。
 * </p>
 */
@Slf4j
@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements CourseScheduleService {

    /**
     * 同步分布式锁 key
     */
    private static final String SYNC_LOCK_KEY = "lock:schedule:sync";

    /**
     * 最近一次同步结果 key（供运维排查，保留 7 天）
     */
    private static final String SYNC_STATE_KEY = "schedule:sync:last";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CourseScheduleMapper courseScheduleMapper;
    private final ScheduleSyncProperties syncProperties;
    private final RedisLockUtils redisLockUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OaApiClient oaApiClient;
    /**
     * 保护课表替换事务直到提交或回滚完成的数据库会话锁
     */
    private final TransactionalMysqlLock databaseLock;

    /**
     * 进程内锁，避免同一实例重复发起同步；跨实例由 Redis 租约和数据库事务锁保护。
     */
    private final AtomicBoolean localRunning = new AtomicBoolean(false);

    /**
     * 创建课表服务并配置整体替换事务
     *
     * @param courseScheduleMapper 课表数据访问接口
     * @param syncProperties       课表同步配置
     * @param redisLockUtils       分布式锁工具
     * @param stringRedisTemplate  同步状态缓存操作接口
     * @param transactionManager   数据库事务管理器
     * @param oaApiClient          OA 数据客户端
     * @param databaseLock         数据库事务锁工具
     */
    public CourseScheduleServiceImpl(CourseScheduleMapper courseScheduleMapper,
                                     ScheduleSyncProperties syncProperties,
                                     RedisLockUtils redisLockUtils,
                                     StringRedisTemplate stringRedisTemplate,
                                     PlatformTransactionManager transactionManager,
                                     OaApiClient oaApiClient,
                                     TransactionalMysqlLock databaseLock) {
        this.courseScheduleMapper = courseScheduleMapper;
        this.syncProperties = syncProperties;
        this.redisLockUtils = redisLockUtils;
        this.stringRedisTemplate = stringRedisTemplate;
        // 显式使用 TransactionTemplate，避免同类方法自调用导致 @Transactional 失效
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.oaApiClient = oaApiClient;
        this.databaseLock = databaseLock;
    }

    @Override
    public List<CourseSchedule> getList(CourseSchedule courseSchedule) {
        return courseScheduleMapper.selectListByCondition(courseSchedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCourseSchedule(CourseSchedule courseSchedule) {
        return courseScheduleMapper.insert(courseSchedule) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseSchedule(CourseSchedule courseSchedule) {
        return courseScheduleMapper.updateById(courseSchedule) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        return courseScheduleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return courseScheduleMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public boolean refresh() {
        return sync("manual").isSuccess();
    }

    /**
     * 执行带自动续期租约保护的课表同步
     * <p>
     * 分布式锁不可用时放弃本轮同步；先关闭租约并释放锁，再允许本实例开始下一轮。
     * </p>
     *
     * @param source 同步触发来源
     * @return 本次同步的执行状态与入库结果
     */
    @Override
    public CourseScheduleSyncResult sync(String source) {
        if (!syncProperties.isEnabled()) {
            log.info("[课表同步][{}] 同步开关已关闭（schedule.sync.enabled=false），本次跳过", source);
            return CourseScheduleSyncResult.skipped("同步开关已关闭");
        }

        if (!localRunning.compareAndSet(false, true)) {
            return CourseScheduleSyncResult.skipped("本实例已有同步任务在执行，本次跳过");
        }
        CourseScheduleSyncResult result;
        try (RedisLockUtils.LockLease lease = acquireLock(source)) {
            if (lease == null) {
                return CourseScheduleSyncResult.skipped("其他实例正在同步，本次跳过");
            }
            result = doSync(source, lease);
        } catch (Exception e) {
            log.error("[课表同步][{}] 无法安全持有分布式锁，本次未执行", source, e);
            result = CourseScheduleSyncResult.skipped("分布式锁不可用，本次同步放弃");
        } finally {
            localRunning.set(false);
        }
        recordStatus(result);
        return result;
    }

    /**
     * 获取同步锁
     * <p>
     * 获取自动续期的 Redis 租约，Redis 不可用时放弃同步，不进行单机降级。
     * </p>
     *
     * @param source 触发来源
     * @return 锁租约，未抢到返回 null
     */
    private RedisLockUtils.LockLease acquireLock(String source) {
        RedisLockUtils.LockLease lease = redisLockUtils.tryLease(SYNC_LOCK_KEY, syncProperties.getLockTtlSeconds());
        if (lease == null) {
            log.warn("[课表同步][{}] 其他实例正在同步，本次跳过", source);
        }
        return lease;
    }

    /**
     * 执行同步主流程
     *
     * @param source 触发来源
     * @param lease  当前实例持有的分布式锁租约
     * @return 同步结果
     */
    private CourseScheduleSyncResult doSync(String source, RedisLockUtils.LockLease lease) {
        long start = System.currentTimeMillis();
        CourseScheduleSyncResult result = new CourseScheduleSyncResult();
        result.setExecuted(true);

        try {
            lease.requireOwned();
            // ========== 1. 拉取数据（失败会抛 OaApiException，绝不触碰正式表） ==========
            log.info("[课表同步][{}] 开始，正在拉取 OA 班牌数据…", source);
            String dataJson = oaApiClient.getClassBoardData("", "");
            if (dataJson == null || dataJson.trim().isEmpty()) {
                throw new IllegalStateException("OA 未返回数据，本次同步放弃");
            }

            JsonNode root = objectMapper.readTree(dataJson);
            if (!root.isArray()) {
                throw new IllegalStateException("OA 返回结构不是数组，本次同步放弃");
            }

            int totalRows = root.size();
            result.setTotalRows(totalRows);

            // ========== 2. 分页截断保护 ==========
            if (totalRows >= syncProperties.getPageSize()) {
                throw new IllegalStateException("OA 返回条数 " + totalRows + " 已达单页上限 "
                        + syncProperties.getPageSize() + "，数据疑似被截断，本次同步放弃");
            }

            // ========== 3. 逐行解析与字段校验 ==========
            List<CourseSchedule> schedules = new ArrayList<>(totalRows);
            int invalidRows = 0;
            for (JsonNode node : root) {
                CourseSchedule courseSchedule = convertToCourseSchedule(node);
                if (courseSchedule == null) {
                    invalidRows++;
                    continue;
                }
                schedules.add(courseSchedule);
            }
            result.setValidRows(schedules.size());
            if (invalidRows > 0) {
                log.warn("[课表同步][{}] 共 {} 条记录因缺少必要字段被跳过", source, invalidRows);
            }

            int previousRows = replaceAtomically(schedules, lease);
            result.setPreviousRows(previousRows);
            result.setSavedRows(schedules.size());
            result.setSuccess(true);
            result.setMessage("同步成功");
            log.info("[课表同步][{}] 完成：替换前 {} 条 → 拉取 {} 条 → 入库 {} 条", source,
                    previousRows, totalRows, schedules.size());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            log.error("[课表同步][{}] 执行失败，正式表数据保持不变：{}", source, e.getMessage(), e);
        }

        result.setCostMillis(System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 校验本次数据量是否可信
     *
     * @param newRows      本次有效记录数
     * @param previousRows 替换前记录数
     */
    private void validateRows(int newRows, int previousRows) {
        if (newRows == 0) {
            throw new IllegalStateException("解析后有效课程数据为 0 条，疑似接口异常，本次同步放弃");
        }
        if (newRows < syncProperties.getMinRows()) {
            throw new IllegalStateException("本次有效数据 " + newRows + " 条，低于最小阈值 "
                    + syncProperties.getMinRows() + " 条，疑似接口异常，本次同步放弃");
        }
        if (previousRows > 0) {
            double ratio = (double) newRows / previousRows;
            if (ratio < syncProperties.getShrinkGuardRatio()) {
                throw new IllegalStateException(String.format(
                        "本次有效数据 %d 条，仅为上次 %d 条的 %.1f%%，低于收缩保护比例 %.0f%%，疑似接口异常，本次同步放弃",
                        newRows, previousRows, ratio * 100, syncProperties.getShrinkGuardRatio() * 100));
            }
        }
    }

    /**
     * 在单个事务内完成「清空 + 批量写入」
     * <p>
     * 使用 DELETE 而非 TRUNCATE：TRUNCATE 是 DDL 会隐式提交、无法回滚；
     * DELETE 受事务保护，写入失败时旧数据自动恢复；同时 InnoDB 的多版本并发控制
     * 保证事务提交前其他会话仍能读到旧数据，不存在「表为空的窗口期」。
     * 数据量校验在数据库会话锁保护下进行；删除前、每批写入前和提交前验证租约有效性。
     * </p>
     *
     * @param schedules 待写入数据
     * @param lease     当前实例持有的分布式锁租约
     * @return 替换前的课表行数
     */
    private int replaceAtomically(List<CourseSchedule> schedules, RedisLockUtils.LockLease lease) {
        int batchSize = syncProperties.getBatchSize() > 0 ? syncProperties.getBatchSize() : 500;
        return transactionTemplate.execute(status -> {
            databaseLock.acquire("assessment:course_schedule:sync");
            lease.requireOwned();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * 提交前验证租约，失效时阻止提交并回滚替换事务。
                 *
                 * @param readOnly 当前事务是否只读
                 */
                @Override
                public void beforeCommit(boolean readOnly) {
                    lease.requireOwned();
                }
            });
            int previousRows = courseScheduleMapper.countAll();
            validateRows(schedules.size(), previousRows);
            int deleted = courseScheduleMapper.deleteAll();
            log.info("[课表同步] 已清除旧数据 {} 条，开始批量写入 {} 条", deleted, schedules.size());
            for (int i = 0; i < schedules.size(); i += batchSize) {
                lease.requireOwned();
                int end = Math.min(i + batchSize, schedules.size());
                List<CourseSchedule> batch = new ArrayList<>(schedules.subList(i, end));
                if (!saveBatch(batch, batchSize)) {
                    throw new IllegalStateException("课表批量写入失败，本次替换已回滚");
                }
            }
            return previousRows;
        });
    }

    /**
     * 记录最近一次同步状态到 Redis，便于运维排查（失败不影响主流程）
     *
     * @param result 同步结果
     */
    private void recordStatus(CourseScheduleSyncResult result) {
        try {
            String value = String.format(
                    "{\"time\":\"%s\",\"executed\":%s,\"success\":%s,\"totalRows\":%d,\"validRows\":%d,"
                            + "\"savedRows\":%d,\"previousRows\":%d,\"costMillis\":%d,\"message\":\"%s\"}",
                    LocalDateTime.now().format(TIME_FORMATTER), result.isExecuted(), result.isSuccess(),
                    result.getTotalRows(), result.getValidRows(), result.getSavedRows(),
                    result.getPreviousRows(), result.getCostMillis(),
                    result.getMessage() == null ? "" : result.getMessage().replace("\"", "'"));
            stringRedisTemplate.opsForValue().set(SYNC_STATE_KEY, value, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("[课表同步] 写入同步状态到 Redis 失败，不影响同步结果", e);
        }
    }

    /**
     * 将 JsonNode 转换为 CourseSchedule 对象
     *
     * @param node OA 返回的课程节点
     * @return 课程对象，缺少必要字段时返回 null
     */
    private CourseSchedule convertToCourseSchedule(JsonNode node) {
        CourseSchedule course = new CourseSchedule();

        // 映射字段
        course.setCourseName(node.path("KCMC").asText(null));           // 课程名称
        course.setAcademicYear(node.path("KKXND").asText(null));        // 学年
        course.setSemester(node.path("KKXQM").asText(null));            // 学期
        course.setWeek(node.path("ZC").asText(null));                   // 周次
        course.setDayOfWeek(node.path("XQJ").asText(null));             // 星期几
        course.setClassPeriod(node.path("SKJC").asText(null));          // 上课节次
        course.setClassroomNumber(node.path("JSH").asText(null));       // 教室号
        course.setTeachingLocation(node.path("SKDD").asText(null));     // 上课地点
        course.setCampus(node.path("XQ").asText(null));                 // 校区
        course.setBuildingName(node.path("JZWMC").asText(null));        // 建筑物名称
        course.setTeacherId(node.path("JGH").asText(null));             // 教师工号
        course.setTeacherName(node.path("JSXM").asText(null));          // 教师姓名
        course.setDepartmentName(node.path("SZDWMC").asText(null));     // 所在单位
        course.setClassName(node.path("BJMC").asText(null));            // 班级名称
        course.setCounselorName(node.path("FDYXM").asText(null));       // 辅导员姓名


        // 数值类型字段
        if (node.has("JXBRS") && !node.path("JXBRS").isNull()) {
            course.setClassSize(node.path("JXBRS").asInt(0));
        }
        if (node.has("QJRS") && !node.path("QJRS").isNull()) {
            course.setLeaveCount(node.path("QJRS").asInt(0));
        }

        course.setHasLeave(node.path("SFYQJRS").asText("0"));           // 是否有请假
        course.setClassDate(node.path("SKRQ").asText(null));            // 上课日期

        // 验证必要字段
        if (isBlank(course.getCourseName()) || isBlank(course.getClassName())
                || isBlank(course.getClassDate())) {
            log.debug("[课表同步] 跳过无效数据：缺少课程名称 / 班级名称 / 上课日期");
            return null;
        }

        return course;
    }

    /**
     * 判断必要字段是否为空或仅包含空白字符
     *
     * @param value 待校验字段值
     * @return 字段为空或仅包含空白字符时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
