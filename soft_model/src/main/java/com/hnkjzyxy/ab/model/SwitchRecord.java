package com.hnkjzyxy.ab.model;

import java.time.LocalDateTime;

/**
 * 开关记录实体类
 * 对应数据库中的开关记录表
 */
public class SwitchRecord {

    /**
     * 开关记录唯一标识符（自增主键）
     */
    private Integer switchId;

    /**
     * 关联的智能锁ID
     */
    private Integer lockId;

    /**
     * 操作用户ID
     */
    private Integer userId;

    /**
     * 操作方式（0-关 1-开）
     */
    private Integer operationMethod;

    /**
     * 操作时间（精确到年月日时分秒）
     */
    private LocalDateTime operationTime;

    /**
     * 无参构造方法
     */
    public SwitchRecord() {
    }

    /**
     * 全参构造方法
     *
     * @param switchId        开关记录唯一标识符
     * @param lockId          关联的智能锁ID
     * @param userId          操作用户ID
     * @param operationMethod 操作方式（0=关，1=开）
     * @param operationTime   操作时间
     */
    public SwitchRecord(Integer switchId, Integer lockId, Integer userId,
                        Integer operationMethod, LocalDateTime operationTime) {
        this.switchId = switchId;
        this.lockId = lockId;
        this.userId = userId;
        this.operationMethod = operationMethod;
        this.operationTime = operationTime;
    }

    /**
     * 新增用构造方法：不指定 switchId，由数据库自增生成
     *
     * @param lockId          关联的智能锁ID
     * @param userId          操作用户ID
     * @param operationMethod 操作方式（0=关，1=开）
     * @param operationTime   操作时间
     */
    public SwitchRecord(Integer lockId, Integer userId,
                        Integer operationMethod, LocalDateTime operationTime) {
        this.lockId = lockId;
        this.userId = userId;
        this.operationMethod = operationMethod;
        this.operationTime = operationTime;
    }

    /**
     * Getter 和 Setter 方法
     */
    public Integer getSwitchId() {
        return switchId;
    }

    public void setSwitchId(Integer switchId) {
        this.switchId = switchId;
    }

    public Integer getLockId() {
        return lockId;
    }

    public void setLockId(Integer lockId) {
        this.lockId = lockId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getOperationMethod() {
        return operationMethod;
    }

    public void setOperationMethod(Integer operationMethod) {
        this.operationMethod = operationMethod;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    @Override
    public String toString() {
        return "SwitchRecord{" +
                "switchId=" + switchId +
                ", lockId=" + lockId +
                ", userId=" + userId +
                ", operationMethod=" + operationMethod +
                ", operationTime=" + operationTime +
                '}';
    }
}