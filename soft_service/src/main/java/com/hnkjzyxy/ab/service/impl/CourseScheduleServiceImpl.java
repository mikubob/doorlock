package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.client.OaApiClient;
import com.hnkjzyxy.ab.config.ScheduleSyncProperties;
import com.hnkjzyxy.ab.mapper.CourseScheduleMapper;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.service.CourseScheduleService;
import com.hnkjzyxy.ab.service.ScheduleWriteCoordinator;
import com.hnkjzyxy.ab.service.CoursePeriodResolver;
import com.hnkjzyxy.ab.mapper.ClassroomMapper;
import com.hnkjzyxy.ab.model.Classroom;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.hnkjzyxy.ab.utils.RedisLockUtils;
import com.hnkjzyxy.ab.utils.TransactionalMysqlLock;
import com.hnkjzyxy.ab.vo.CourseScheduleSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    /**
     * 课表时间字符串格式化器
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * JSON 序列化及反序列化工具
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 课表数据访问接口
     */
    private final CourseScheduleMapper courseScheduleMapper;
    /**
     * 课表同步开关、时间及数据校验配置
     */
    private final ScheduleSyncProperties syncProperties;
    /**
     * Redis 分布式锁工具
     */
    private final RedisLockUtils redisLockUtils;
    /**
     * 字符串 Redis 数据操作模板
     */
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 编程式事务模板
     */
    private final TransactionTemplate transactionTemplate;
    /**
     * OA 接口客户端
     */
    private final OaApiClient oaApiClient;
    /**
     * 保护课表替换事务直到提交或回滚完成的数据库会话锁
     */
    private final TransactionalMysqlLock databaseLock;
    /**
     * 统一排程事务及校验。
     */
    @Autowired
    private ScheduleWriteCoordinator coordinator;
    /**
     * 教室身份访问。
     */
    @Autowired
    private ClassroomMapper classroomMapper;
    /**
     * 统一作息解析。
     */
    @Autowired
    private CoursePeriodResolver periodResolver;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CourseSchedule> getList(CourseSchedule courseSchedule) {
        return courseScheduleMapper.selectListByCondition(courseSchedule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCourseSchedule(CourseSchedule courseSchedule) {
        coordinator.lock();
        if (courseSchedule.getId() != null) throw new IllegalArgumentException("新增课程不能指定ID");
        courseSchedule.setSourceType("LOCAL"); courseSchedule.setCourseKey(UUID.randomUUID().toString());
        courseSchedule.setLocalAdjusted(1); courseSchedule.setEffective(1); courseSchedule.setRowVersion(0L);
        coordinator.validateCourseWrite(courseSchedule);
        if (courseScheduleMapper.insert(courseSchedule) != 1) throw new IllegalStateException("新增课程失败，已回滚");
        coordinator.changed("COURSE_ADD", courseSchedule.toString());
        coordinator.reconcileConflicts();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseSchedule(CourseSchedule courseSchedule) {
        coordinator.lock();
        CourseSchedule old = courseScheduleMapper.selectById(courseSchedule.getId());
        if (old == null) throw new IllegalArgumentException("课程不存在");
        if (courseSchedule.getRowVersion() == null || !courseSchedule.getRowVersion().equals(old.getRowVersion())) throw new IllegalArgumentException("课程已被修改，请刷新后重试");
        if (courseSchedule.getChangeReason() == null || courseSchedule.getChangeReason().trim().isEmpty()) throw new IllegalArgumentException("独立停课/调课必须填写理由");
        CourseSchedule merged = new CourseSchedule();
        BeanUtils.copyProperties(old, merged);
        java.beans.PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(CourseSchedule.class);
        Set<String> editable = new HashSet<>(Arrays.asList("courseName", "academicYear", "semester", "week", "dayOfWeek", "classPeriod", "classroomNumber", "teachingLocation", "campus", "buildingName", "teacherId", "teacherName", "departmentName", "className", "counselorName", "classSize", "leaveCount", "hasLeave", "classDate", "classroomId", "effective"));
        try {
            for (java.beans.PropertyDescriptor descriptor : descriptors) {
                if (!editable.contains(descriptor.getName())) continue;
                Object value = descriptor.getReadMethod().invoke(courseSchedule);
                if (value != null) descriptor.getWriteMethod().invoke(merged, value);
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("课程合并失败", e); }
        if (courseSchedule.getClassroomId() == null && (courseSchedule.getClassroomNumber() != null || courseSchedule.getCampus() != null || courseSchedule.getBuildingName() != null)) merged.setClassroomId(null);
        if (merged.getEffective() != null && merged.getEffective() != 0 && merged.getEffective() != 1) throw new IllegalArgumentException("课程有效状态不合法");
        merged.setLocalAdjusted(1); merged.setRowVersion(old.getRowVersion() + 1);
        if (!Objects.equals(old.getClassName(), merged.getClassName())) merged.setClassSize(null);
        if (!Objects.equals(old.getClassName(), merged.getClassName()) || !Objects.equals(old.getClassDate(), merged.getClassDate())
                || !Objects.equals(old.getClassPeriod(), merged.getClassPeriod())) {
            merged.setLeaveCount(null); merged.setHasLeave(null);
        }
        if (old.getSourceFingerprint() == null && "OA".equals(old.getSourceType())) merged.setSourceFingerprint(fingerprint(old));
        coordinator.validateCourseWrite(merged);
        merged.setParseStatus("PENDING".equals(old.getParseStatus()) ? "PENDING" : "RESOLVED");
        if (courseScheduleMapper.updateById(merged) != 1) throw new IllegalStateException("调整课程失败，已回滚");
        coordinator.changed("COURSE_ADJUST_APPROVED", "before=" + old + "; after=" + merged + "; reason=" + courseSchedule.getChangeReason());
        coordinator.reconcileConflicts();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rebindSource(com.hnkjzyxy.ab.dto.CourseSourceRebindDto request) {
        coordinator.lock();
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("来源复核必须填写确认依据");
        }
        List<CourseSchedule> rows = courseScheduleMapper.selectList(null);
        CourseSchedule old = uniqueCourse(rows, request.getCourseKey());
        if (!Integer.valueOf(1).equals(old.getLocalAdjusted()) || !"PENDING".equals(old.getParseStatus())) {
            throw new IllegalArgumentException("仅待复核的独立调整可以关联来源");
        }
        if (request.getRowVersion() == null || !request.getRowVersion().equals(old.getRowVersion())) {
            throw new IllegalArgumentException("调整已被修改，请刷新后复核");
        }
        String before = old.toString();
        CourseSchedule merged = new CourseSchedule();
        BeanUtils.copyProperties(old, merged);
        String sourceDetail = "人工确认来源已撤销，转为本地课程";
        if (request.getSourceCourseKey() == null || request.getSourceCourseKey().trim().isEmpty()) {
            merged.setSourceType("LOCAL");
        } else {
            CourseSchedule source = uniqueCourse(rows, request.getSourceCourseKey());
            if (Objects.equals(source.getCourseKey(), old.getCourseKey()) || !"OA".equals(source.getSourceType())
                    || Integer.valueOf(1).equals(source.getLocalAdjusted()) || source.getSourceFingerprint() == null) {
                throw new IllegalArgumentException("请选择未独立调整的新 OA 来源");
            }
            if (request.getSourceRowVersion() == null || !request.getSourceRowVersion().equals(source.getRowVersion())) {
                throw new IllegalArgumentException("来源已更新，请刷新后复核");
            }
            sourceDetail = source.toString();
            merged.setSourceType("OA");
            merged.setSourceFingerprint(source.getSourceFingerprint());
            if (courseScheduleMapper.deleteById(source.getId()) != 1) {
                throw new IllegalStateException("来源关联失败，已回滚");
            }
        }
        coordinator.validateCourseWrite(merged);
        merged.setParseStatus("RESOLVED");
        merged.setRowVersion(old.getRowVersion() + 1);
        if (courseScheduleMapper.updateById(merged) != 1) throw new IllegalStateException("保存复核失败，已回滚");
        coordinator.changed("COURSE_SOURCE_REBIND_APPROVED", "before=" + before + "; source=" + sourceDetail
                + "; after=" + merged + "; reason=" + request.getReason().trim());
        coordinator.reconcileConflicts();
        return true;
    }

    /**
     * 根据稳定键读取唯一课程，不采用首条匹配。
     *
     * @param rows 最新课程集合
     * @param key 稳定课程键
     * @return 唯一课程
     */
    private CourseSchedule uniqueCourse(List<CourseSchedule> rows, String key) {
        List<CourseSchedule> found = rows.stream().filter(c -> key != null && key.equals(c.getCourseKey()))
                .collect(java.util.stream.Collectors.toList());
        if (found.size() != 1) throw new IllegalArgumentException("课程来源不存在或不唯一，请刷新后复核");
        return found.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        coordinator.lock();
        cancelCourse(id);
        coordinator.changed("COURSE_CANCEL_APPROVED", "courseId=" + id);
        coordinator.reconcileConflicts();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        coordinator.lock();
        if (ids.size() > 200 || ids.contains(null) || new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("课程ID缺失、重复或超限");
        for (Integer id : ids) cancelCourse(id);
        coordinator.changed("COURSE_CANCEL_BATCH_APPROVED", ids.toString());
        coordinator.reconcileConflicts();
        return true;
    }

    /**
     * {@inheritDoc}
     */
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
                throw new IllegalStateException("OA 存在 " + invalidRows + " 条缺少必要字段的课表，本次发布放弃");
            }

            PublicationCount count = replaceAtomically(schedules, lease);
            int previousRows = count.getPreviousRows();
            result.setPreviousRows(previousRows);
            result.setSavedRows(count.getSavedRows());
            result.setSuccess(true);
            result.setMessage("同步成功");
            log.info("[课表同步][{}] 完成：替换前 {} 条 → 拉取 {} 条 → 入库 {} 条", source,
                    previousRows, totalRows, count.getSavedRows());
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
     * @return 替换前 OA 行数与保留独立调整后的实际发布行数
     */
    private PublicationCount replaceAtomically(List<CourseSchedule> schedules, RedisLockUtils.LockLease lease) {
        int batchSize = syncProperties.getBatchSize() > 0 ? syncProperties.getBatchSize() : 500;
        return transactionTemplate.execute(status -> {
            coordinator.lock();
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
            int previousRows = courseScheduleMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>().eq(CourseSchedule::getSourceType, "OA"));
            validateRows(schedules.size(), previousRows);
            List<CourseSchedule> previous = courseScheduleMapper.selectList(null);
            List<Classroom> rooms = classroomMapper.selectList(null);
            Map<String, List<CourseSchedule>> oldVersions = new HashMap<>();
            for (CourseSchedule old : previous) {
                String key = old.getSourceFingerprint() == null ? fingerprint(old) : old.getSourceFingerprint();
                if (!"LOCAL".equals(old.getSourceType())) oldVersions.computeIfAbsent(key, k -> new ArrayList<>()).add(old);
            }
            Map<String, Long> newCounts = schedules.stream().collect(java.util.stream.Collectors.groupingBy(this::fingerprint, java.util.stream.Collectors.counting()));
            Set<String> retained = new HashSet<>();
            List<CourseSchedule> publish = new ArrayList<>();
            for (CourseSchedule incoming : schedules) {
                String key = fingerprint(incoming);
                List<CourseSchedule> candidates = oldVersions.getOrDefault(key, Collections.emptyList());
                if (candidates.size() == 1 && newCounts.get(key) == 1) {
                    CourseSchedule old = candidates.get(0);
                    if (Integer.valueOf(1).equals(old.getLocalAdjusted())) {
                        if (Objects.equals(old.getClassName(), incoming.getClassName())) old.setClassSize(incoming.getClassSize());
                        else old.setClassSize(null);
                        if (Objects.equals(old.getClassName(), incoming.getClassName())
                                && Objects.equals(old.getClassDate(), incoming.getClassDate())
                                && Objects.equals(old.getClassPeriod(), incoming.getClassPeriod())) {
                            old.setLeaveCount(incoming.getLeaveCount()); old.setHasLeave(incoming.getHasLeave());
                        } else { old.setLeaveCount(null); old.setHasLeave(null); }
                        incoming = old;
                    } else {
                        incoming.setCourseKey(old.getCourseKey()); incoming.setRowVersion(old.getRowVersion() + 1);
                    }
                    retained.add(old.getCourseKey());
                }
                if (incoming.getCourseKey() == null) incoming.setCourseKey(UUID.randomUUID().toString());
                incoming.setId(null); incoming.setSourceType("OA"); incoming.setSourceFingerprint(key);
                if (incoming.getLocalAdjusted() == null) incoming.setLocalAdjusted(0);
                if (incoming.getEffective() == null) incoming.setEffective(1);
                if (incoming.getRowVersion() == null) incoming.setRowVersion(0L);
                incoming.setClassroomId(coordinator.binding(incoming, rooms));
                try {
                    periodResolver.resolve(incoming);
                    if (!"PENDING".equals(incoming.getParseStatus())) incoming.setParseStatus(incoming.getClassroomId() == null ? "UNBOUND" : "RESOLVED");
                } catch (RuntimeException error) { incoming.setParseStatus("INVALID"); }
                publish.add(incoming);
            }
            for (CourseSchedule old : previous) {
                if ("LOCAL".equals(old.getSourceType())) { publish.add(old); }
                else if ((Integer.valueOf(1).equals(old.getLocalAdjusted()) || "UNVERIFIED".equals(old.getSourceType()))
                        && !retained.contains(old.getCourseKey())) {
                    old.setLocalAdjusted(1);
                    old.setParseStatus("PENDING"); old.setRowVersion(old.getRowVersion() + 1); publish.add(old);
                    coordinator.changed("COURSE_ADJUSTMENT_REVIEW", "source changed; courseKey=" + old.getCourseKey());
                }
            }
            int deleted = courseScheduleMapper.deleteAll();
            log.info("[课表同步] 已清除旧数据 {} 条，开始批量写入 {} 条", deleted, schedules.size());
            for (int i = 0; i < publish.size(); i += batchSize) {
                lease.requireOwned();
                int end = Math.min(i + batchSize, publish.size());
                List<CourseSchedule> batch = new ArrayList<>(publish.subList(i, end));
                if (!saveBatch(batch, batchSize)) {
                    throw new IllegalStateException("课表批量写入失败，本次替换已回滚");
                }
            }
            coordinator.published(schedules);
            coordinator.changed("OA_PUBLISH", "rows=" + publish.size() + "; previous=" + previousRows);
            coordinator.reconcileConflicts();
            return new PublicationCount(previousRows, publish.size());
        });
    }

    /**
     * 单次发布事务中的真实计数，不由事务后的另一轮读取推断。
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class PublicationCount {
        /**
         * 替换前 OA 行数。
         */
        private int previousRows;
        /**
         * 本地课程及调整保留后的实际发布行数。
         */
        private int savedRows;
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
                    LocalDateTime.now(CoursePeriodResolver.ZONE).format(TIME_FORMATTER), result.isExecuted(), result.isSuccess(),
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
            course.setClassSize(nonNegativeCount(node.path("JXBRS")));
        }
        if (node.has("QJRS") && !node.path("QJRS").isNull()) {
            course.setLeaveCount(nonNegativeCount(node.path("QJRS")));
        }

        course.setHasLeave(node.path("SFYQJRS").asText(null));
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

    /**
     * 缺失、非整数或超限人数保持未知，不能由 OA 解析默认值制造零人。
     *
     * @param node 原始人数字段
     * @return 已确认的非负整数，无法确定返回 null
     */
    private Integer nonNegativeCount(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        String text = node.asText().trim();
        if (!text.matches("\\d+")) return null;
        try { return Integer.valueOf(text); }
        catch (NumberFormatException error) { return null; }
    }

    /**
     * 保存独立停课层，删除来源课程不能让下一次 OA 同步复活。
     * @param id 课程行主键
     */
    private void cancelCourse(Integer id) {
        CourseSchedule old = courseScheduleMapper.selectById(id);
        if (old == null) throw new IllegalArgumentException("课程不存在");
        if (old.getSourceFingerprint() == null) old.setSourceFingerprint(fingerprint(old));
        old.setEffective(0); old.setLocalAdjusted(1); old.setRowVersion(old.getRowVersion() + 1);
        if (courseScheduleMapper.updateById(old) != 1) throw new IllegalStateException("停课失败，已回滚");
    }

    /**
     * 生成来源版本指纹，仅精确匹配未改变的 OA 版本；调课身份变化必须人工复核。
     * @param course 来源课程
     * @return 版本指纹
     */
    private String fingerprint(CourseSchedule course) {
        String value = String.join("|", Arrays.asList(course.getCourseName(), course.getAcademicYear(), course.getSemester(),
                course.getTeacherId(), course.getClassName(), course.getClassDate(), course.getClassPeriod(),
                course.getClassroomNumber(), course.getCampus(), course.getBuildingName()).stream()
                .map(v -> v == null ? "" : v.trim()).toArray(String[]::new));
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
