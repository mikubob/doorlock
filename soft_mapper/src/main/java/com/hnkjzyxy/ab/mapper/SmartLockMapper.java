package com.hnkjzyxy.ab.mapper;

import com.hnkjzyxy.ab.model.LockInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 智能锁Mapper接口
 */
@Mapper
public interface SmartLockMapper {
    /**
     * 根据ID查询智能锁信息（联表查询教室信息）
     *
     * @param lockId 智能锁唯一标识符
     * @return 智能锁信息
     */
    @Select("SELECT l.lock_id as lockId, l.ip_address as ipAddress, l.sn_code as snCode, " +
            "l.port_number as portNumber, l.switch_status as switchStatus, l.JSH as classroomNumber, " +
            "l.remarks, c.SKDD as classroomName, c.XQMC as campusName, " +
            "c.JZWMC as buildingName, c.floor " +
            "FROM sys_lock_info l LEFT JOIN sys_classroom c ON l.JSH COLLATE utf8mb4_general_ci = c.JSH COLLATE utf8mb4_general_ci " +
            "WHERE l.lock_id = #{lockId}")
    LockInfo selectById(Integer lockId);


    /**
     * 根据电子班牌ID查询智能锁信息（联表查询教室信息）
     *
     * @param boardSn 智能班牌的sn
     * @return 智能锁信息
     */
    @Select("SELECT l.lock_id as lockId, l.ip_address as ipAddress, l.sn_code as snCode, " +
            "l.port_number as portNumber, l.switch_status as switchStatus, l.JSH as classroomNumber, " +
            "l.remarks, c.SKDD as classroomName, c.XQMC as campusName, " +
            "c.JZWMC as buildingName, c.floor " +
            "FROM sys_lock_info l LEFT JOIN sys_classroom c ON l.JSH COLLATE utf8mb4_general_ci = c.JSH COLLATE utf8mb4_general_ci " +
            "WHERE c.board_sn = #{boardSn}")
    LockInfo selectByBoardSn(String boardSn);

    /**
     * 查询所有智能锁信息（联表查询教室信息）
     *
     * @return 智能锁列表
     */
    @Select("SELECT l.lock_id as lockId, l.ip_address as ipAddress, l.sn_code as snCode, " +
            "l.port_number as portNumber, l.switch_status as switchStatus, l.JSH as classroomNumber, " +
            "l.remarks, c.SKDD as classroomName, c.XQMC as campusName, " +
            "c.JZWMC as buildingName, c.floor, c.board_sn as boardSn " +
            "FROM sys_lock_info l LEFT JOIN sys_classroom c ON l.JSH COLLATE utf8mb4_general_ci = c.JSH COLLATE utf8mb4_general_ci")
    List<LockInfo> selectAll();



    /**
     * 根据教室编号查询智能锁
     *
     * @param classroomNumber 教室编号
     * @return 智能锁信息
     */
    @Select("SELECT l.lock_id as lockId, l.ip_address as ipAddress, l.sn_code as snCode, " +
            "l.port_number as portNumber, l.switch_status as switchStatus, l.JSH as classroomNumber, " +
            "l.remarks, c.SKDD as classroomName, c.XQMC as campusName, " +
            "c.JZWMC as buildingName, c.floor " +
            "FROM sys_lock_info l LEFT JOIN sys_classroom c ON l.JSH COLLATE utf8mb4_general_ci = c.JSH COLLATE utf8mb4_general_ci " +
            "WHERE l.JSH = #{classroomNumber}")
    LockInfo selectByClassroom(String classroomNumber);

    /**
     * 新增智能锁信息
     *
     * @param lockInfo 智能锁对象
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_lock_info(ip_address, sn_code, port_number, switch_status, JSH, remarks) " +
            "VALUES(#{ipAddress}, #{snCode}, #{portNumber}, 0, #{classroomNumber}, #{remarks})")
    @Options(useGeneratedKeys = false)
    int insert(LockInfo lockInfo);

    /**
     * 更新智能锁信息
     *
     * @param lockInfo 智能锁对象
     * @return 影响行数
     */
    @Update("UPDATE sys_lock_info SET " +
            "JSH = #{classroomNumber}, " +
            "switch_status = #{switchStatus}, " +
            "remarks = #{remarks} " +
            "WHERE lock_id = #{lockId}")
    int update(LockInfo lockInfo);

    /**
     * 更新智能锁开关状态（修复单引号转义问题）
     *
     * @param lockId       智能锁ID
     * @param switchStatus 开关状态
     * @return 影响行数
     */
    @Update("UPDATE sys_lock_info " +
            "SET switch_status = #{switchStatus}  " +
            "WHERE lock_id = #{lockId}")
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
    @Delete("DELETE FROM sys_lock_info WHERE lock_id = #{lockId}")
    int deleteById(Integer lockId);

    @Select("SELECT count(*) FROM sys_lock_info")
    int countNum();

    @Insert("INSERT INTO sys_lock_info " +
        "(ip_address, sn_code, port_number, switch_status, JSH, remarks) " +
        "VALUES (#{ipAddress}, #{snCode}, #{portNumber}, #{switchStatus}, #{classroomNumber}, #{remarks})")
    int addLockInfoAll(LockInfo lockInfo);


}
