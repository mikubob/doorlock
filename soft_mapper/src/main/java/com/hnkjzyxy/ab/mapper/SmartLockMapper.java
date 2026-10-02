package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.LockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 智能锁数据访问接口
 */
@Mapper
public interface SmartLockMapper {
    /**
     * 根据ID查询智能锁信息（联表查询教室信息）
     *
     * @param lockId 智能锁唯一标识符
     * @return 智能锁信息
     */
    LockInfo selectById(@Param("lockId") Integer lockId);

    /**
     * 根据电子班牌ID查询智能锁信息（联表查询教室信息）
     *
     * @param boardSn 智能班牌的sn
     * @return 智能锁信息
     */
    LockInfo selectByBoardSn(@Param("boardSn") String boardSn);

    /**
     * 查询所有智能锁信息（联表查询教室信息）
     *
     * @return 智能锁列表
     */
    List<LockInfo> selectAll();

    /**
     * 根据教室编号查询智能锁
     *
     * @param classroomNumber 教室编号
     * @return 智能锁信息
     */
    LockInfo selectByClassroom(@Param("classroomNumber") String classroomNumber);

    /**
     * 新增智能锁信息
     *
     * @param lockInfo 智能锁对象
     * @return 影响行数
     */
    int insert(LockInfo lockInfo);

    /**
     * 更新智能锁信息
     *
     * @param lockInfo 智能锁对象
     * @return 影响行数
     */
    int update(LockInfo lockInfo);

    /**
     * 更新智能锁开关状态（修复单引号转义问题）
     *
     * @param lockId       智能锁ID
     * @param switchStatus 开关状态
     * @return 影响行数
     */
    int updateSwitchStatus(
            @Param("lockId") Integer lockId,
            @Param("switchStatus") Integer switchStatus
    );

    /**
     * 根据ID删除智能锁信息
     *
     * @param lockId 智能锁唯一标识符
     * @return 影响行数
     */
    int deleteById(@Param("lockId") Integer lockId);

    /**
     * 统计门禁设备数量
     *
     * @return 智能锁设备记录数
     */
    int countNum();

    /**
     * 新增门禁设备的完整信息
     *
     * @param lockInfo 智能门锁设备信息
     * @return 受影响的记录数量
     */
    int addLockInfoAll(LockInfo lockInfo);

}
