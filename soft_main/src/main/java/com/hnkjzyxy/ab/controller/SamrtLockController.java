package com.hnkjzyxy.ab.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.config.QuartzConfig;
import com.hnkjzyxy.ab.dto.ScheduleTaskDto;
import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.model.ScheduleTask;
import com.hnkjzyxy.ab.model.SwitchRecord;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ScheduleService;
import com.hnkjzyxy.ab.service.support.LockCommandService;
import com.hnkjzyxy.ab.config.LockScheduleReconcileService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.SwitchRecordService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import com.hnkjzyxy.ab.service.support.SmartLockStateService;
import com.hnkjzyxy.ab.service.support.SmartLockDiscoveryService;
import com.hnkjzyxy.ab.vo.UserVo;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 智能门锁管理
 * 提供锁设备维护、远程开锁/关锁、开关锁记录及定时开关锁任务管理接口
 */
@RestController
@RequestMapping("/smart/lock")
@PreAuthorize("hasRole('admin')")
public class SamrtLockController {
    /**
     * 厂家回执命令服务。
     */
    @Autowired
    private LockCommandService commandService;
    /**
     * 持久化期望任务与 Quartz 对账。
     */
    @Autowired
    private LockScheduleReconcileService reconcileService;
    /**
     * 设备绑定变更的共同事务及审计。
     */
    @Autowired
    private com.hnkjzyxy.ab.service.ScheduleWriteCoordinator scheduleCoordinator;
    /**
     * 持久化命令回执查询。
     */
    @Autowired
    private com.hnkjzyxy.ab.mapper.LockCommandMapper lockCommandMapper;
    /**
     * 未知回执的独立人工确认服务。
     */
    @Autowired
    private com.hnkjzyxy.ab.service.support.LockCommandReceiptService receiptService;
    /**
     * 线程池：用于异步执行查询门锁状态任务
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    /**
     * 智能门锁业务服务
     */
    @Autowired
    SmartLockService smartLockService;
    /**
     * 门禁设备通讯接口
     */
    @Autowired
    private SmartLockGateway smartLockGateway;
    /**
     * 门禁状态查询及回写服务
     */
    @Autowired
    private SmartLockStateService smartLockStateService;
    /**
     * 门禁设备发现及登记服务
     */
    @Autowired
    private SmartLockDiscoveryService smartLockDiscoveryService;
    /**
     * 开关锁记录业务服务
     */
    @Autowired
    SwitchRecordService switchRecordService;
    /**
     * 用户业务服务
     */
    @Autowired
    UserService userService;
    /**
     * 门禁定时任务业务服务
     */
    @Autowired
    ScheduleService scheduleService;
    /**
     * Quartz 任务及触发器配置
     */
    @Autowired
    QuartzConfig quartzConfig;
    /**
     * Quartz 调度器工厂
     */
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    /**
     * 查询所有锁设备信息
     *
     * @return 锁设备列表
     */
    @GetMapping("/selectAll")
    public ApiResult selectAll() {
        List<LockInfo> all = smartLockService.getAll();
        return ApiResult.ok("data", all);
    }
    /**
     * 新增锁设备信息
     *
     * @param lockInfo 锁设备信息
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/addLockInfo")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ApiResult addLockInfo(@RequestBody LockInfo lockInfo) {
        scheduleCoordinator.lock();
        validateLockBinding(lockInfo);
        if(smartLockService.addLockInfoAll(lockInfo)){
            scheduleCoordinator.changed("LOCK_BINDING_ADD", lockInfo.toString());
            return ApiResult.ok("添加成功");
        }else {
            return ApiResult.error("添加失败");
        }
    }
    /**
     * 根据教室编号查询锁信息
     *
     * @param classroomNumber 教室编号
     * @return 锁设备信息
     */
    @GetMapping("/getCollege")
    public ApiResult getByClassroom(@RequestParam("classroomNumber") String classroomNumber) {
        LockInfo lockInfo = smartLockService.getByClassroom(classroomNumber);
        return ApiResult.ok("data", lockInfo);
    }

    /**
     * 根据锁ID查询锁信息
     *
     * @param lockId 锁ID
     * @return 锁设备信息
     */
    @GetMapping("/getById")
    public ApiResult getById(@RequestParam("lockId") Integer lockId) {
        LockInfo lockInfo = smartLockService.getById(lockId);
        return ApiResult.ok("data", lockInfo);
    }

    /**
     * 搜索局域网内的锁设备
     * 搜索结果暂存于内存，供刷新锁设备记录使用
     */
    @GetMapping("/searchcheck")
    public void search() {
        smartLockDiscoveryService.startDiscovery();
    }

    /**
     * 刷新锁设备记录
     * 将局域网搜索到的锁设备同步到数据库（按 SN 码自动去重）
     *
     * @return 操作结果
     */
    @PostMapping("/insertLock")
    public ApiResult insertLock() {
        if (smartLockDiscoveryService.registerDiscoveredDevices()) {
            return ApiResult.ok("添加成功");
        }
        return ApiResult.error("添加失败");
    }
    /**
     * 删除锁设备
     *
     * @param lockId 锁设备信息（使用其中的 lockId 字段）
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/deleteLock")
    public ApiResult deleteLock(@RequestBody LockInfo lockId) {
        boolean result = smartLockService.deleteById(lockId.getLockId());
        if (result) {
            return ApiResult.ok("删除成功");
        }
        return ApiResult.error("删除失败");
    }

    /**
     * 修改锁设备信息
     *
     * @param lockInfo 锁设备信息
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/updateLock")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ApiResult updateLock(@RequestBody LockInfo lockInfo) {
        scheduleCoordinator.lock();
        LockInfo byId = smartLockService.getById(lockInfo.getLockId());
        if (byId == null) {
            return ApiResult.error("锁不存在");
        }
        LockInfo UpdatelockInfo = setLockInfoPojo(lockInfo);
        validateLockBinding(UpdatelockInfo);
        boolean result = smartLockService.update(UpdatelockInfo);
        if (result) {
            scheduleCoordinator.changed("LOCK_BINDING_UPDATE", "before=" + byId + "; after=" + UpdatelockInfo);
            return ApiResult.ok("修改成功");
        }
        return ApiResult.error("修改失败");
    }

    /**
     * 更新锁开关状态（开锁 / 关锁）
     * 管理端提交真实设备命令，回执与物理观测独立保存。
     *
     * @param lockInfo 锁状态信息（boardSn、switchStatus）
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/updateSwitchStatus")
    public ApiResult updateSwitchStatus(@RequestBody LockInfo lockInfo, Authentication authentication) {
        if (lockInfo.getSwitchStatus() == null || (lockInfo.getSwitchStatus() != 0 && lockInfo.getSwitchStatus() != 1)) return ApiResult.error("开关动作不合法");
        LockInfo device = lockInfo.getLockId() == null ? smartLockService.getByBoardSn(lockInfo.getBoardSn()) : smartLockService.getById(lockInfo.getLockId());
        if (device == null || device.getClassroomId() == null) return ApiResult.error("真实设备教室绑定未确认");
        com.hnkjzyxy.ab.model.LockCommand command = commandService.submit(device, device.getDoorChannel(), lockInfo.getSwitchStatus() == 1,
                authentication.getName(), null, java.util.UUID.randomUUID().toString(), null);
        if ("failed".equals(command.getStatus())) return ApiResult.error("设备指令失败，物理状态未知").put("data", command);
        return ApiResult.ok("acknowledged".equals(command.getStatus()) ? "指令已确认，物理状态待观测"
                : "unknown".equals(command.getStatus()) ? "回执超时，设备状态待人工确认" : "门禁命令已提交，物理门状态待确认").put("data", command);
    }

    /**
     * 统计锁设备数量
     *
     * @return 锁设备总数
     */
    @GetMapping("/countNum")
    public ApiResult countNum() {
        int num = smartLockService.countNum();
        return ApiResult.ok("ok", num);
    }

    /**
     * 查询所有定时任务
     *
     * @return 定时任务列表
     */
    @GetMapping("/getTimer")
    public ApiResult getTimer() {
        List<ScheduleTask> all = scheduleService.getAll();
        return ApiResult.ok("data", all);
    }

    /**
     * 根据用户ID查询定时任务
     *
     * @param UserId 用户ID
     * @return 该用户的定时任务列表
     */
    @GetMapping("/getScheduleByUserId")
    public ApiResult getByUserId(@RequestParam("UserId") int UserId) {
        List<ScheduleTask> all = scheduleService.getByUserId(UserId);
        return ApiResult.ok("data", all);
    }

    /**
     * 根据时间范围查询定时任务
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 该时间范围内的定时任务列表
     */
    @GetMapping("/getTimerByTime")
    public ApiResult getTimerByTime(@RequestParam("startTime") LocalDateTime startTime, @RequestParam("endTime") LocalDateTime endTime) {
        List<ScheduleTask> byTime = scheduleService.getByTime(startTime, endTime);
        return ApiResult.ok("data", byTime);
    }

    /**
     * 根据ID查询定时任务
     *
     * @param id 定时任务ID
     * @return 定时任务详情
     */
    @GetMapping("/getTimerById")
    public ApiResult getTimerById(@RequestParam("id") Integer id) {
        ScheduleTask byId = scheduleService.getById(id);
        return ApiResult.ok("data", byId);
    }

    /**
     * 修改定时任务状态
     * taskStatus 为 1 时同步注册 Quartz 定时任务
     *
     * @param scheduleTask 定时任务信息
     * @param authentication 当前登录认证信息
     * @return 操作结果
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     * @throws JsonProcessingException JSON 数据解析失败时抛出
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/updateTimerStatus")
    public ApiResult updateTimerStatus(@RequestBody ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException, JsonProcessingException {
        ScheduleTask existing = scheduleService.getById(scheduleTask.getTaskId());
        if (existing == null) return ApiResult.error("任务不存在");
        if (scheduleTask.getTaskStatus() < 0 || scheduleTask.getTaskStatus() > 3) return ApiResult.error("任务状态不合法");
        existing.setTaskStatus(scheduleTask.getTaskStatus());
        existing.setUpdatedTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        if (existing.getTaskStatus() == 1 && existing.getLoopCount() == 0) return ApiResult.error("次数已耗尽，请独立修改次数后启用");
        if (existing.getTaskStatus() == 1) validateTaskBinding(existing);
        if (scheduleService.updateStatus(existing) != 1) return ApiResult.error("保存任务状态失败或次数已耗尽");
        reconcileService.reconcile();
        return ApiResult.ok("任务状态已保存，调度将自动对账");
    }

    /**
     * 新增定时任务
     * 写入数据库并按周生成 Cron 表达式，注册 Quartz 定时开关锁任务
     *
     * @param scheduleTask 定时任务信息
     * @param authentication 当前登录认证信息
     * @return 操作结果及生成的 Cron 表达式
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/addTimer")
    public ApiResult addTimer(@RequestBody ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException {
        ScheduleTask task = trantoScheduleTask(scheduleTask);
        task.setUserId(userService.getUserInfo(authentication.getName()).getUserId());
        task.setTaskId(0);
        validateTaskBinding(task);
        quartzConfig.generateWeeklyCronExpression(task.getHour(), task.getMinute(), scheduleTask.getCountDay());
        if (task.getDoorChannel() == null || !task.getDoorChannel().matches("[1-4]")) return ApiResult.error("请配置真实通道1至4");
        if (task.getLoopCount() < -1 || task.getLoopCount() == 0) return ApiResult.error("次数必须为正数或-1（无限）");
        if (task.getTaskStatus() != 0 && task.getTaskStatus() != 1) return ApiResult.error("新增任务只能未启用或已启用");
        if (scheduleService.insert(task) != 1) return ApiResult.error("保存任务失败");
        reconcileService.reconcile();
        return ApiResult.ok("任务已保存，调度将自动对账").put("data", task);
    }

    /**
     * 修改定时任务信息
     *
     * @param scheduleTask 定时任务信息
     * @param authentication 当前登录认证信息
     * @return 操作结果
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     * @throws JsonProcessingException JSON 数据解析失败时抛出
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/updateTimer")
    public ApiResult updateTimer(@RequestBody ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException, JsonProcessingException {
        ScheduleTask old = scheduleService.getById(scheduleTask.getTaskId());
        if (old == null) return ApiResult.error("任务不存在");
        ScheduleTask task = trantoScheduleTask(scheduleTask);
        task.setUserId(old.getUserId()); task.setCreatedTime(old.getCreatedTime()); task.setLockId(old.getLockId());
        task.setTaskStatus(old.getTaskStatus());
        quartzConfig.generateWeeklyCronExpression(task.getHour(), task.getMinute(), scheduleTask.getCountDay());
        if (task.getDoorChannel() == null || !task.getDoorChannel().matches("[1-4]") || task.getLoopCount() < -1) return ApiResult.error("通道或次数不合法");
        if (task.getLoopCount() == 0 && old.getLoopCount() != 0) return ApiResult.error("零次仅表示耗尽，请填写正数或-1");
        validateTaskBinding(task);
        if (scheduleService.update(task) != 1) return ApiResult.error("保存任务失败");
        reconcileService.reconcile();
        return ApiResult.ok("任务已保存，调度将自动对账");
    }

    /**
     * 取消定时任务
     *
     * @param scheduleTask 定时任务信息（lockId）
     * @return 操作结果
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/cancelWeeklyTimer")
    public ApiResult cancelWeeklyTimer(@RequestBody ScheduleTask scheduleTask) throws SchedulerException {
        if (cancelTimer(scheduleTask) > 0) {
            return ApiResult.ok("定时任务已取消");
        }
        return ApiResult.error("未找到对应的定时任务");
    }

    /**
     * 删除指定锁的 Quartz 定时任务并更新任务状态
     *
     * @param scheduleTask 门禁定时任务信息
     * @return 删除了已存在调度任务时返回一，未找到调度任务时返回零
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     */
    public int cancelTimer(ScheduleTask scheduleTask) throws SchedulerException {
        ScheduleTask old = scheduleService.getById(scheduleTask.getTaskId());
        if (old == null) return 0;
        if (old.getTaskStatus() == 3) return 1;
        old.setTaskStatus(3); old.setUpdatedTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        if (scheduleService.updateStatus(old) != 1) throw new IllegalStateException("取消任务保存失败");
        reconcileService.reconcile(); return 1;
    }

    /**
     * 异步查询所有锁设备的实时开关状态
     *
     * @return 查询完成后的锁设备状态列表
     */
    @GetMapping("/queryStatus")
    public CompletableFuture<ApiResult> queryStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LockInfo> all = smartLockService.getAll();

                // 为每个锁设备创建异步查询任务
                List<CompletableFuture<LockInfo>> futures = all.stream().map(lockInfo -> {
                    try {
                        return smartLockStateService.refreshStatus(lockInfo).handle((updated, error) -> {
                            if (error != null) {
                                lockInfo.setSwitchStatus(null);
                                System.err.println("查询锁状态失败: " + lockInfo.getSnCode() + ", 错误: " + error.getMessage());
                                return lockInfo;
                            }
                            return updated;
                        });
                    } catch (Exception e) {
                        lockInfo.setSwitchStatus(null);
                        System.err.println("查询锁状态失败: " + lockInfo.getSnCode() + ", 错误: " + e.getMessage());
                        return CompletableFuture.completedFuture(lockInfo);
                    }
                }).collect(Collectors.toList());

                // 等待所有异步任务完成
                CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

                // 收集所有结果
                List<LockInfo> results = allOf.thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList())).join();

                return ApiResult.ok("异步查询完成", results);
            } catch (Exception e) {
                System.err.println("异步查询状态时发生错误: " + e.getMessage());
                return ApiResult.error("查询失败: " + e.getMessage());
            }
        }, executorService);
    }

    /**
     * 按数据库任务配置重新注册门禁定时任务
     *
     * @param scheduleTask 门禁定时任务请求参数
     * @param authentication 当前登录认证信息
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     * @throws JsonProcessingException JSON 数据解析失败时抛出
     */
    public void setTimerTask(ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException, JsonProcessingException {
        reconcileService.reconcile();
    }

    /**
     * 将定时任务请求转换为持久化对象并设置时间字段
     *
     * @param scheduleTaskDto 门禁定时任务请求参数
     * @return 门禁定时任务信息
     */
    public ScheduleTask trantoScheduleTask(ScheduleTaskDto scheduleTaskDto) {
        ScheduleTask scheduleTaskTemp = new ScheduleTask();
        scheduleTaskTemp.setLockId(scheduleTaskDto.getLockId());
        scheduleTaskTemp.setTaskId(scheduleTaskDto.getTaskId());
        scheduleTaskTemp.setTaskStatus(scheduleTaskDto.getTaskStatus());
        scheduleTaskTemp.setUserId(scheduleTaskDto.getUserId());
        scheduleTaskTemp.setTaskDetails(scheduleTaskDto.getTaskDetails());
        scheduleTaskTemp.setUpdatedTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        scheduleTaskTemp.setIsLoop(scheduleTaskDto.getIsLoop());
        scheduleTaskTemp.setHour(scheduleTaskDto.getHour());
        scheduleTaskTemp.setMinute(scheduleTaskDto.getMinute());

        // 直接存储前端传递的0-6数组（转为字符串）
        if (scheduleTaskDto.getCountDay() != null) {
            scheduleTaskTemp.setCountDay(Arrays.toString(scheduleTaskDto.getCountDay()));
        }

        scheduleTaskTemp.setTimedOperation(scheduleTaskDto.getTimedOperation());
        scheduleTaskTemp.setRemarks(scheduleTaskDto.getRemarks());
        scheduleTaskTemp.setDoorChannel(scheduleTaskDto.getDoorChannel());
        scheduleTaskTemp.setTaskStatus(scheduleTaskDto.getTaskStatus());
        scheduleTaskTemp.setCreatedTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        scheduleTaskTemp.setLoopCount(scheduleTaskDto.getLoopCount());

        return scheduleTaskTemp;
    }

    /**
     * 查询命令回执，厂家指令确认不代表物理门已开。
     *
     * @param commandId 命令稳定主键
     * @return 提交、确认、失败或未知及设备绑定快照
     */
    @GetMapping("/command/{commandId}")
    public ApiResult command(@org.springframework.web.bind.annotation.PathVariable String commandId) {
        com.hnkjzyxy.ab.model.LockCommand command = lockCommandMapper.selectById(commandId);
        return command == null ? ApiResult.error("命令不存在") : ApiResult.ok("data", command);
    }

    /**
     * 查询指定真实任务的未知回执，用于现场核查，不重新发送历史动作。
     *
     * @param taskId 真实任务主键
     * @return 最多二百条未知回执
     */
    @GetMapping("/commands")
    public ApiResult unknownCommands(@RequestParam Integer taskId) {
        return ApiResult.ok("data", lockCommandMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.hnkjzyxy.ab.model.LockCommand>()
                .eq(com.hnkjzyxy.ab.model.LockCommand::getTaskId, taskId).eq(com.hnkjzyxy.ab.model.LockCommand::getStatus, "unknown")
                .orderByDesc(com.hnkjzyxy.ab.model.LockCommand::getCreatedTime).last("LIMIT 200")));
    }

    /**
     * 管理员依据真实核查收敛未知指令；此接口不产生设备动作。
     *
     * @param commandId 命令主键
     * @param request 确认结果及理由
     * @return 独立复核结果
     */
    @PostMapping("/command/{commandId}/review")
    public ApiResult reviewCommand(@org.springframework.web.bind.annotation.PathVariable String commandId,
            @RequestBody com.hnkjzyxy.ab.dto.LockCommandReviewDto request) {
        receiptService.review(commandId, request);
        reconcileService.reconcile();
        return ApiResult.ok("原指令已独立复核，未补发设备动作");
    }

    /**
     * 核实内部教室、真实设备及独立通道，名称由唯一教室主键读取。
     *
     * @param lock 待保存的真实绑定
     */
    private void validateLockBinding(LockInfo lock) {
        if (lock.getClassroomId() == null || lock.getDoorChannel() == null || !lock.getDoorChannel().matches("[1-4]")) {
            throw new IllegalArgumentException("必须选择唯一教室主键及通道1至4");
        }
        if (lock.getSnCode() == null || lock.getSnCode().trim().isEmpty() || lock.getSnCode().length() > 128
                || lock.getIpAddress() == null || lock.getIpAddress().trim().isEmpty()
                || lock.getPortNumber() == null || lock.getPortNumber() < 1 || lock.getPortNumber() > 65535) {
            throw new IllegalArgumentException("真实设备通讯信息不完整");
        }
        boolean duplicate = smartLockService.getAll().stream().anyMatch(other -> !java.util.Objects.equals(other.getLockId(), lock.getLockId())
                && lock.getSnCode().equals(other.getSnCode()) && lock.getDoorChannel().equals(other.getDoorChannel()));
        if (duplicate) throw new IllegalArgumentException("设备同一通道已有绑定，请独立核实");
        com.hnkjzyxy.ab.model.Classroom room = scheduleCoordinator.room(lock.getClassroomId(), null);
        lock.setClassroomNumber(room.getClassroomNumber()); lock.setClassroomName(room.getClassroomName());
        lock.setCampusName(room.getCampusName()); lock.setBuildingName(room.getBuildingName());
        lock.setSwitchStatus(null); lock.setObservedAt(null);
    }

    /**
     * 任务通道必须与真实锁的教室通道绑定一致，不凭描述或任意数字发指令。
     *
     * @param task 待保存的任务
     */
    private void validateTaskBinding(ScheduleTask task) {
        LockInfo lock = smartLockService.getById(task.getLockId());
        if (lock == null || lock.getClassroomId() == null || task.getDoorChannel() == null
                || !task.getDoorChannel().equals(lock.getDoorChannel())) {
            throw new IllegalArgumentException("任务与真实教室、锁或通道绑定不一致，请先核实绑定");
        }
        if (task.getTimedOperation() != 0 && task.getTimedOperation() != 1) throw new IllegalArgumentException("门禁动作不合法");
    }

    /**
     * 读取已有设备并应用非空的状态、教室编号及备注字段
     *
     * @param lockInfo 智能门锁设备信息
     * @return 智能门锁设备信息
     */
    public LockInfo setLockInfoPojo(LockInfo lockInfo) {
        LockInfo byId = smartLockService.getById(lockInfo.getLockId());
        if (lockInfo.getClassroomNumber() != null) {
            byId.setClassroomNumber(lockInfo.getClassroomNumber());
        }
        if (lockInfo.getRemarks() != null) {
            byId.setRemarks(lockInfo.getRemarks());
        }
        if (lockInfo.getClassroomId() != null) byId.setClassroomId(lockInfo.getClassroomId());
        if (lockInfo.getDoorChannel() != null) byId.setDoorChannel(lockInfo.getDoorChannel());
        return byId;
    }

}
