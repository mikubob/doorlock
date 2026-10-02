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
public class SamrtLockController {
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
    @PostMapping("/addLockInfo")
    public ApiResult addLockInfo(@RequestBody LockInfo lockInfo) {
        if(smartLockService.addLockInfoAll(lockInfo)){
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
    @PostMapping("/updateLock")
    public ApiResult updateLock(@RequestBody LockInfo lockInfo) {
        LockInfo byId = smartLockService.getById(lockInfo.getLockId());
        if (byId == null) {
            return ApiResult.error("锁不存在");
        }
        LockInfo UpdatelockInfo = setLockInfoPojo(lockInfo);
        boolean result = smartLockService.update(UpdatelockInfo);
        if (result) {
            return ApiResult.ok("修改成功");
        }
        return ApiResult.error("修改失败");
    }

    /**
     * 更新锁开关状态（开锁 / 关锁）
     * 支持普通用户 token 与开锁专用 token，并自动记录开关锁操作日志
     *
     * @param lockInfo 锁状态信息（boardSn、switchStatus）
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/updateSwitchStatus")
    public ApiResult updateSwitchStatus(@RequestBody LockInfo lockInfo, Authentication authentication) {
        // 检查是否为开锁专用token
        String username = authentication.getName();
        boolean isLockOnlyToken = "LOCK_ONLY_USER".equals(username);


        //联查获取到锁的完整信息
        LockInfo byId = smartLockService.getByBoardSn(lockInfo.getBoardSn());
        if (byId == null){
            return ApiResult.error("你的sn不存在");
        }
        if (lockInfo.getSwitchStatus() == byId.getSwitchStatus()){
            if (byId.getSwitchStatus()==1) {
                return ApiResult.error("锁已处于开锁状态");
            }
            else {
                return ApiResult.error("锁已处于关锁状态");
            }
        }
        lockInfo.setLockId(byId.getLockId());

        // 如果不是开锁专用token,需要获取用户信息并记录操作日志
        UserVo userInfo = null;
        if (!isLockOnlyToken) {
            userInfo = userService.getUserInfo(authentication.getName());
        }

        //更改锁状态
        boolean result = smartLockService.updateSwitchStatus(lockInfo.getLockId(), lockInfo.getSwitchStatus());
        if (result) {
            //判断用户的意图(开/关)
            if (byId != null && lockInfo.getSwitchStatus() == 1) {
                smartLockGateway.openDoor(byId.getIpAddress(), byId.getPortNumber(), byId.getSnCode(),byId.getRemarks());
                SwitchRecord switchRecord = new SwitchRecord();
                switchRecord.setOperationMethod(1);
                switchRecord.setLockId(byId.getLockId());
                // 如果是开锁专用token,userId设为-1表示通过密码开锁
                switchRecord.setUserId(isLockOnlyToken ? -1 : userInfo.getUserId());
                switchRecord.setOperationTime(LocalDateTime.now());
                switchRecordService.insert(switchRecord);
                return ApiResult.ok("开锁成功");
            } else if (byId != null && lockInfo.getSwitchStatus() == 0) {
                smartLockGateway.closeDoor(byId.getSnCode(), byId.getIpAddress(), byId.getPortNumber(),byId.getRemarks());
                SwitchRecord switchRecord = new SwitchRecord();
                switchRecord.setLockId(byId.getLockId());
                switchRecord.setUserId(isLockOnlyToken ? -1 : userInfo.getUserId());
                switchRecord.setOperationTime(LocalDateTime.now());
                switchRecord.setOperationMethod(0);
                switchRecordService.insert(switchRecord);
                return ApiResult.ok("关锁成功");
            }
        }
        return ApiResult.error("更新失败");
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
    @PostMapping("/updateTimerStatus")
    public ApiResult updateTimerStatus(@RequestBody ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException, JsonProcessingException {
        ScheduleTask scheduleTaskTemp = trantoScheduleTask(scheduleTask);
        scheduleService.updateStatus(scheduleTaskTemp);
        if (scheduleTask.getTaskStatus() == 1) {
            setTimerTask(scheduleTask, authentication);
        }
        if (scheduleService.updateStatus(scheduleTaskTemp) > 0) {
            return ApiResult.ok("修改成功");
        }
        return ApiResult.error("修改失败");
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
    @PostMapping("/addTimer")
    public ApiResult addTimer(@RequestBody ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException {
        // 获取用户信息
        UserVo userInfo = userService.getUserInfo(authentication.getName());
        // 验证锁是否存在
        LockInfo lockInfo = smartLockService.getById(scheduleTask.getLockId());
        if (lockInfo == null) {
            return ApiResult.error("锁不存在");
        }
        ScheduleTask scheduleTaskTemp = trantoScheduleTask(scheduleTask);
        scheduleService.insert(scheduleTaskTemp);

        // 生成Cron表达式（直接使用前端传递的0-6数组）
        String cronExpression = quartzConfig.generateWeeklyCronExpression(scheduleTaskTemp.getHour(), scheduleTaskTemp.getMinute(), scheduleTask.getCountDay());

        // 创建任务和触发器
        JobDetail jobDetail = quartzConfig.createJobDetail(scheduleTaskTemp.getLockId(), userInfo.getUserId(), scheduleTaskTemp.getTaskId(),scheduleTask.getRemarks());
        Trigger trigger;
        if (scheduleTask.getLoopCount() > -2) {
            trigger = quartzConfig.createCronTrigger(jobDetail, cronExpression, scheduleTaskTemp.getLoopCount());
        } else {
            trigger = quartzConfig.createCronTrigger(jobDetail, cronExpression);
        }
        // 获取调度器并安排任务
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        // 先删除可能存在的同名任务
        if (scheduler.checkExists(jobDetail.getKey())) {
            scheduler.deleteJob(jobDetail.getKey());
        }

        scheduler.scheduleJob(jobDetail, trigger);
        return ApiResult.ok("每周定时任务已设置", cronExpression);
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
    @PostMapping("/updateTimer")
    public ApiResult updateTimer(@RequestBody ScheduleTaskDto scheduleTask, Authentication authentication) throws SchedulerException, JsonProcessingException {
        ScheduleTask byId = scheduleService.getById(scheduleTask.getTaskId());
        ScheduleTask scheduleTaskTemp = trantoScheduleTask(scheduleTask);
        byId.setTaskDetails(scheduleTaskTemp.getTaskDetails());
        byId.setHour(scheduleTaskTemp.getHour());
        byId.setMinute(scheduleTaskTemp.getMinute());
        // 直接使用前端传递的0-6数组
        byId.setCountDay(Arrays.toString(scheduleTask.getCountDay()));
        byId.setLoopCount(scheduleTaskTemp.getLoopCount());
        byId.setIsLoop(scheduleTaskTemp.getIsLoop());
        byId.setUpdatedTime(LocalDateTime.now());
        byId.setRemarks(scheduleTaskTemp.getRemarks());
        int update = scheduleService.update(byId);
        if (byId.getTaskStatus() == 1) {
            setTimerTask(scheduleTask, authentication);
        }
        if (update > 0) {
            return ApiResult.ok("修改成功");
        }
        return ApiResult.error("修改失败");
    }

    /**
     * 取消定时任务
     *
     * @param scheduleTask 定时任务信息（lockId）
     * @return 操作结果
     * @throws SchedulerException Quartz 任务查询或调度操作失败时抛出
     */
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
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        JobKey jobKey = new JobKey("smartLockJob_" + scheduleTask.getLockId(), "smartLockGroup");

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            scheduleTask.setTaskStatus(3);
            scheduleService.updateStatus(scheduleTask);
            return 1;
        }
        scheduleService.updateStatus(scheduleTask);
        return 0;
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
                                System.err.println("查询锁状态失败: " + lockInfo.getSnCode() + ", 错误: " + error.getMessage());
                                return lockInfo;
                            }
                            return updated;
                        });
                    } catch (Exception e) {
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
        ScheduleTask scheduleTask1 = scheduleService.getById(scheduleTask.getTaskId());
        // 获取用户信息
        UserVo userInfo = userService.getUserInfo(authentication.getName());

        // 直接使用前端传递的0-6数组
        String countDay = scheduleTask1.getCountDay();
        ObjectMapper mapper = new ObjectMapper();
        int[] days = mapper.readValue(countDay, int[].class);


        // 生成Cron表达式
        String cronExpression = quartzConfig.generateWeeklyCronExpression(scheduleTask1.getHour(), scheduleTask1.getMinute(), days);

        // 创建任务和触发器
        JobDetail jobDetail = quartzConfig.createJobDetail(scheduleTask1.getLockId(), userInfo.getUserId(), scheduleTask1.getTaskId(),scheduleTask.getRemarks());
        Trigger trigger;
        if (scheduleTask.getLoopCount() > 0) {
            trigger = quartzConfig.createCronTrigger(jobDetail, cronExpression, scheduleTask1.getLoopCount());
        } else {
            trigger = quartzConfig.createCronTrigger(jobDetail, cronExpression);
        }

        // 获取调度器并安排任务
        Scheduler scheduler = schedulerFactoryBean.getScheduler();

        // 先删除可能存在的同名任务
        if (scheduler.checkExists(jobDetail.getKey())) {
            scheduler.deleteJob(jobDetail.getKey());
        }

        scheduler.scheduleJob(jobDetail, trigger);
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
        scheduleTaskTemp.setUpdatedTime(LocalDateTime.now());
        scheduleTaskTemp.setIsLoop(scheduleTaskDto.getIsLoop());
        scheduleTaskTemp.setHour(scheduleTaskDto.getHour());
        scheduleTaskTemp.setMinute(scheduleTaskDto.getMinute());

        // 直接存储前端传递的0-6数组（转为字符串）
        if (scheduleTaskDto.getCountDay() != null) {
            scheduleTaskTemp.setCountDay(Arrays.toString(scheduleTaskDto.getCountDay()));
        }

        scheduleTaskTemp.setTimedOperation(scheduleTaskDto.getTimedOperation());
        scheduleTaskTemp.setRemarks(scheduleTaskDto.getRemarks());
        scheduleTaskTemp.setTaskStatus(scheduleTaskDto.getTaskStatus());
        scheduleTaskTemp.setCreatedTime(LocalDateTime.now());
        scheduleTaskTemp.setLoopCount(scheduleTaskDto.getLoopCount());

        return scheduleTaskTemp;
    }

    /**
     * 读取已有设备并应用非空的状态、教室编号及备注字段
     *
     * @param lockInfo 智能门锁设备信息
     * @return 智能门锁设备信息
     */
    public LockInfo setLockInfoPojo(LockInfo lockInfo) {
        LockInfo byId = smartLockService.getById(lockInfo.getLockId());
        if (lockInfo.getSwitchStatus() != null) {
            byId.setSwitchStatus(lockInfo.getSwitchStatus());
        }
        if (lockInfo.getClassroomNumber() != null) {
            byId.setClassroomNumber(lockInfo.getClassroomNumber());
        }
        if (lockInfo.getRemarks() != null) {
            byId.setRemarks(lockInfo.getRemarks());
        }
        return byId;
    }

}
