package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.SwitchRecord;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.SwitchRecordService;
import com.hnkjzyxy.ab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 开关记录管理
 * 提供开关记录的增删改查功能，并根据用户权限等级控制访问范围
 */
@RestController
@RequestMapping("/smart")
@Validated
public class SwitchRecordController {

    @Autowired
    private SwitchRecordService switchRecordService;

    @Autowired
    private UserService userService;

    /**
     * 创建开关记录
     *
     * @param record         开关记录实体
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @PostMapping("/switchRecord")
    public ApiResult createSwitchRecord(@RequestBody SwitchRecord record, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        // 设置操作用户ID和操作时间
        record.setUserId(user.getUserId());
        record.setOperationTime(LocalDateTime.now());

        int result = switchRecordService.insert(record);
        if (result > 0) {
            return ApiResult.ok("开关记录创建成功！");
        } else {
            return ApiResult.error("开关记录创建失败！");
        }
    }

    /**
     * 删除开关记录
     *
     * @param switchId       开关记录ID
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @DeleteMapping("/switchRecord/{switchId}")
    public ApiResult deleteSwitchRecord(@PathVariable @NotNull Integer switchId, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        // 验证记录是否存在
        SwitchRecord existingRecord = switchRecordService.selectById(switchId);
        if (Objects.isNull(existingRecord)) {
            return ApiResult.error("开关记录不存在！");
        }

        int result = switchRecordService.deleteById(switchId);
        if (result > 0) {
            return ApiResult.ok("开关记录删除成功！");
        } else {
            return ApiResult.error("开关记录删除失败！");
        }
    }

    /**
     * 更新开关记录
     *
     * @param record         开关记录实体
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @PutMapping("/switchRecord")
    public ApiResult updateSwitchRecord(@Valid @RequestBody SwitchRecord record, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        if (Objects.isNull(record.getSwitchId())) {
            return ApiResult.error("开关记录ID不能为空！");
        }

        // 验证记录是否存在
        SwitchRecord existingRecord = switchRecordService.selectById(record.getSwitchId());
        if (Objects.isNull(existingRecord)) {
            return ApiResult.error("开关记录不存在！");
        }

        // 更新操作时间
        record.setOperationTime(LocalDateTime.now());

        int result = switchRecordService.update(record);
        if (result > 0) {
            return ApiResult.ok("开关记录更新成功！");
        } else {
            return ApiResult.error("开关记录更新失败！");
        }
    }

    /**
     * 根据ID查询开关记录
     *
     * @param switchId       开关记录ID
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/{switchId}")
    public ApiResult getSwitchRecordById(@PathVariable @NotNull Integer switchId, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        SwitchRecord record = switchRecordService.selectById(switchId);
        if (Objects.isNull(record)) {
            return ApiResult.error("开关记录不存在！");
        }

        return ApiResult.ok("data", record);
    }

    /**
     * 查询所有开关记录
     *
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord")
    public ApiResult getAllSwitchRecords(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        List<SwitchRecord> records = switchRecordService.selectAll();
        return ApiResult.ok("data", records);
    }

    /**
     * n'p'm
     * 根据锁ID查询开关记录
     *
     * @param lockId         锁ID
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/lock/{lockId}")
    public ApiResult getSwitchRecordsByLockId(@PathVariable @NotNull Integer lockId, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        List<SwitchRecord> records = switchRecordService.selectByLockId(lockId);
        return ApiResult.ok("data", records);
    }

    /**
     * 根据用户ID查询开关记录
     *
     * @param userId         用户ID
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/user/{userId}")
    public ApiResult getSwitchRecordsByUserId(@PathVariable @NotNull Integer userId, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        List<SwitchRecord> records = switchRecordService.selectByUserId(userId);
        return ApiResult.ok("data", records);
    }

    /**
     * 获取当前用户的开关记录
     *
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/current")
    public ApiResult getCurrentUserSwitchRecords(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        List<SwitchRecord> records = switchRecordService.selectByUserId(user.getUserId());
        return ApiResult.ok("data", records);
    }

    /**
     * 根据时间范围查询开关记录
     *
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/timeRange")
    public ApiResult getSwitchRecordsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        if (startTime.isAfter(endTime)) {
            return ApiResult.error("开始时间不能晚于结束时间！");
        }

        List<SwitchRecord> records = switchRecordService.selectByTimeRange(startTime, endTime);
        return ApiResult.ok("data", records);
    }

    /**
     * 统计某个锁的操作次数
     *
     * @param lockId         锁ID
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/count/lock/{lockId}")
    public ApiResult countSwitchRecordsByLockId(@PathVariable @NotNull Integer lockId, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        int count = switchRecordService.countByLockId(lockId);
        return ApiResult.ok("data", count);
    }

    /**
     * 统计某个用户的操作次数
     *
     * @param userId         用户ID
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/count/user/{userId}")
    public ApiResult countSwitchRecordsByUserId(@PathVariable @NotNull Integer userId, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        int count = switchRecordService.countByUserId(userId);
        return ApiResult.ok("data", count);
    }

    /**
     * 统计当前用户的操作次数
     *
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @GetMapping("/switchRecord/count/current")
    public ApiResult countCurrentUserSwitchRecords(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        int count = switchRecordService.countByUserId(user.getUserId());
        return ApiResult.ok("data", count);
    }

    /**
     * 批量删除开关记录
     *
     * @param switchIds      开关记录ID列表
     * @param authentication 当前认证用户
     * @return API响应结果
     */
    @DeleteMapping("/switchRecord/batch")
    public ApiResult batchDeleteSwitchRecords(@RequestBody List<Integer> switchIds, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        if (Objects.isNull(switchIds) || switchIds.isEmpty()) {
            return ApiResult.error("删除记录ID列表不能为空！");
        }

        int successCount = 0;
        int totalCount = switchIds.size();

        for (Integer switchId : switchIds) {
            try {
                int result = switchRecordService.deleteById(switchId);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                // 记录日志，但继续处理其他记录
                System.err.println("删除开关记录失败，ID: " + switchId + ", 错误: " + e.getMessage());
            }
        }

        if (successCount == totalCount) {
            return ApiResult.ok("批量删除成功，共删除 " + successCount + " 条记录！");
        } else {
            return ApiResult.ok("批量删除完成，成功删除 " + successCount + " 条记录，失败 " + (totalCount - successCount) + " 条记录！");
        }
    }
}
