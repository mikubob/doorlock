package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开关记录实体类
 * 对应数据库表 sys_operation_log（记录智能锁开关操作历史日志）
 */
@TableName("sys_operation_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SwitchRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开关记录唯一标识符（自增主键）
     */
    @TableId("switch_id")
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
     * 返回开关记录的日志描述
     *
     * @return 包含操作标识及时间的描述
     */
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

    /**
     * 按非空记录主键比较相等性
     *
     * @param o 待比较对象
     * @return 同一实例或非空主键相同时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SwitchRecord)) {
            return false;
        }
        SwitchRecord that = (SwitchRecord) o;
        return switchId != null && switchId.equals(that.switchId);
    }

    /**
     * 计算与主键相等规则一致的哈希值
     *
     * @return 主键哈希值，主键为空时返回0
     */
    @Override
    public int hashCode() {
        return switchId == null ? 0 : switchId.hashCode();
    }
}
