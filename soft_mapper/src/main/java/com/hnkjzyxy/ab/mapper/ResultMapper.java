package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.dto.ResultAccessScope;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.vo.SubTaskVo;
import org.apache.ibatis.annotations.Param;

import java.util.HashSet;
import java.util.List;

/**
 * 考核结果数据访问接口
 */
public interface ResultMapper extends BaseMapper<Result> {

    /**
     * 统计用户在项目中状态为零或一的结果数量
     *
     * @param pId 考核项目ID
     * @param uId 被考核用户ID
     * @return 用户在项目中未完成或已完成的结果记录数
     */
    Integer selectByPId(@Param("pId") Integer pId, @Param("uId") Integer uId);

    /**
     * 查询项目中达到指定审批步骤且未处于状态二的结果
     *
     * @param pId 考核项目ID
     * @param step 审批步骤
     * @return 考核结果列表
     */
    default List<Result> selectResultCountByPId(@Param("pId") Integer pId, @Param("step") Integer step) {
        return selectList(new LambdaQueryWrapper<Result>()
                .eq(Result::getPId, pId).ge(Result::getStep, step).ne(Result::getIsFinish, 2));
    }

    /**
     * 统计用户指定月份创建的结果数量
     *
     * @param userId 用户ID
     * @param year 统计年度
     * @param month 统计月份
     * @return 用户在指定月份创建的结果记录数
     */
    Integer getUserResultMonthCount(@Param("userId") Integer userId, @Param("year") String year, @Param("month") String month);

    /**
     * 统计用户指定日期完成的结果数量
     *
     * @param s 日期字符串
     * @param userId 用户ID
     * @return 用户在指定日期创建且已完成的结果记录数
     */
    Integer getUserResultWeekCount(@Param("s") String s, @Param("userId") Integer userId);

    /**
     * 查询用户在指定项目下的结果
     *
     * @param userId 用户ID
     * @param pId 考核项目ID
     * @return 考核结果列表
     */
    default List<Result> findByUIdAndPID(@Param("uId") Integer userId, @Param("pId") Integer pId) {
        return selectList(new LambdaQueryWrapper<Result>()
                .eq(Result::getUId, userId).eq(Result::getPId, pId));
    }

    /**
     * 查询所有考核结果的佐证材料字段
     *
     * @return 全部考核结果的佐证材料字段列表
     */
    List<String> getResultEvidence();

    /**
     * 统计项目中达到指定审批步骤的结果数量
     *
     * @param pId 考核项目ID
     * @param step 审批步骤
     * @return 项目中达到指定审批步骤的结果记录数
     */
    default Integer selectApproveCountByPId(@Param("pId") Integer pId, @Param("step") Integer step) {
        return selectCount(new LambdaQueryWrapper<Result>()
                .eq(Result::getPId, pId).ge(Result::getStep, step));
    }

    /**
     * 查询用户项目结果的完成状态
     *
     * @param uId 被考核用户ID
     * @param pId 考核项目ID
     * @return 步骤为零的首条结果完成状态，无匹配记录时返回 null
     */
    Integer selectResultByPIdAndUId(@Param("uId") Integer uId, @Param("pId") Integer pId);

    /**
     * 查询用户指定项目的一条结果记录
     *
     * @param uId 被考核用户ID
     * @param pId 考核项目ID
     * @return 考核结果信息
     */
    Result selectResult(@Param("uId") Integer uId, @Param("pId") Integer pId);

    /**
     * 查询用户指定年度已完成的结果
     *
     * @param num 统计年度
     * @param uId 被考核用户ID
     * @return 考核结果列表
     */
    List<Result> selectListByYear(@Param("num") Integer num, @Param("uId") Integer uId);

    /**
     * 查询项目开始时间涉及的年度
     *
     * @param uId 兼容参数，当前查询不按用户过滤
     * @return 所有项目开始时间去重后的年度列表
     */
    List<String> getFinishScaleYears(@Param("uId") Integer uId);

    /**
     * 查询项目中达到指定步骤且状态为一或四的用户ID
     *
     * @param pId 考核项目ID
     * @param step 审批步骤
     * @return 满足审批步骤及完成状态条件的用户ID列表
     */
    List<Integer> getUserByResult(@Param("pId") Integer pId, @Param("step") Integer step);

    /**
     * 汇总用户在指定项目中的结果分数
     *
     * @param item 被考核用户ID
     * @param projectId 考核项目ID
     * @return 用户在项目中的评分总和，无匹配记录时返回 null
     */
    Integer findTotalScore(@Param("uId") Integer item, @Param("pId") Integer projectId);

    /**
     * 查询用户在项目中的结果完成状态
     *
     * @param item 被考核用户ID
     * @param projectId 考核项目ID
     * @return 首条匹配结果的完成状态，无匹配记录时返回 null
     */
    Integer findResultStatus(@Param("uId") Integer item, @Param("pId") Integer projectId);

    /**
     * 查询用户提交结果涉及的项目ID
     *
     * @param userId 用户ID
     * @return 用户提交结果涉及的项目ID列表，已去重
     */
    List<Integer> getProjectIds(@Param("uId") Integer userId);

    /**
     * 查询用户指定项目的佐证材料字段
     *
     * @param projectId 考核项目ID
     * @param userId 用户ID
     * @return 用户在指定项目中的佐证材料字段列表
     */
    List<String> selectEvidenceList(@Param("pId") Integer projectId, @Param("uId") Integer userId);

    /**
     * 查询指定角色下满足项目审批步骤及结果状态的用户ID
     *
     * @param role 角色ID
     * @param pId 考核项目ID
     * @param step 审批步骤
     * @return 满足角色、项目、审批步骤及完成状态条件的用户ID集合
     */
    HashSet<Integer> getUserIdByRole(@Param("role") Integer role, @Param("pId") Integer pId, @Param("step") Integer step);

    /**
     * 查询满足项目审批步骤及结果状态的用户ID
     *
     * @param pId 考核项目ID
     * @param step 审批步骤
     * @return 满足项目、审批步骤及完成状态条件的用户ID集合
     */
    HashSet<Integer> selectUserIdByStep(@Param("pId") Integer pId, @Param("step") Integer step);

    /**
     * 查询用户是否具有满足项目审批步骤及状态的结果
     *
     * @param pId 考核项目ID
     * @param step 审批步骤
     * @param val 用户ID
     * @return 符合条件的用户ID，无匹配结果时返回 null
     */
    Integer checkResultByUId(@Param("pId") Integer pId, @Param("step") Integer step, @Param("uId") Integer val);

    /**
     * 查询指定用户集合在项目分类下的评分
     *
     * @param projectId 考核项目ID
     * @param uIds 用户ID集合
     * @param category 任务分类
     * @return 指定用户集合在项目分类下的评分列表
     */
    List<Integer> checkResultScore(@Param("pId") Integer projectId, @Param("uIds") HashSet<Integer> uIds, @Param("category") String category);

    /**
     * 查询雷达图统计所需的已完成结果项目ID
     *
     * @param projectId 考核项目ID
     * @param year 统计年度
     * @param userId 用户ID
     * @return 满足雷达图查询条件的项目ID列表
     */
    List<Integer> getPIdByRadar(@Param("pId") Integer projectId, @Param("year") String year, @Param("uId") Integer userId);

    /**
     * 查询指定用户集合的项目评分
     *
     * @param projectId 考核项目ID
     * @param uIds 用户ID集合
     * @return 指定用户集合的项目评分列表
     */
    List<Integer> getProjectResult(@Param("pId") Integer projectId, @Param("uIds") HashSet<Integer> uIds);

    /**
     * 按项目名称和任务名称及分类查询子任务评分
     * <p>
     * 保留旧查询的字段及筛选条件；任务与结果必须属于同一项目。
     * 学院边界在 SQL 内执行，未提供范围时返回空列表，不能省略过滤后查询全部学院。
     * </p>
     *
     * @param title 项目名称
     * @param taskName 任务名称
     * @param taskCategory 任务分类名称
     * @param scope 权限策略生成的全学院查看范围或精确单学院范围，不能来自客户端
     * @return 范围内匹配筛选条件的评分列表，无匹配数据或缺少范围时返回空列表
     */
    List<SubTaskVo> getUsersSubTaskScore(@Param("title") String title, @Param("taskName") String taskName,
                                      @Param("taskCategory") String taskCategory, @Param("scope") ResultAccessScope scope);

    /**
     * 按项目ID、用户ID及任务条件查询子任务评分
     * <p>
     * DTO 中的用户ID仅代表查询目标，不决定操作人身份或学院范围。
     * 学院按精确名称匹配，越界目标返回空列表，不通过查询结果泄露其成绩。
     * </p>
     *
     * @param dto 项目、目标用户、任务名称、任务分类及最低分数等查询条件
     * @param scope 权限策略生成的全学院查看范围或精确单学院范围，不能来自客户端
     * @return 范围内匹配筛选条件的评分列表，无匹配数据或缺少范围时返回空列表
     */
    List<SubTaskVo> getUsersSubTaskScoreById(@Param("dto") SubTaskIdDto dto, @Param("scope") ResultAccessScope scope);

    /**
     * 批量更新子任务评分信息
     * <p>
     * 仅在 Service 已完成整批参数、任务归属、结果存在性及目标学院校验后调用。
     * 外层评分事务按项目ID、目标用户ID分别升序加锁；SQL 再通过用户表限定目标学院。
     * 缺少范围或传入全学院范围时不更新任何成绩，多条语句的原子性由外层事务保证。
     * </p>
     *
     * @param dto 非空的评分列表，每项按用户、项目和字符串任务ID定位结果
     * @param scope 已通过院长修改策略的单学院范围，不允许使用全学院查看范围
     */
    void updateBySubTaskName(@Param("rows") List<SubTaskIdDto> dto, @Param("scope") ResultAccessScope scope);

    /**
     * 锁定评分目标用户并读取当前学院
     * <p>
     * 必须在评分事务内、取得全部项目锁后，对去重的目标ID升序逐条调用。
     * 行锁持有到事务结束，使并发学院调整等待本次评分完成；查询刷新缓存以读取当前行。
     * 不过滤目标账号启用状态，允许核验历史停用用户的成绩归属。
     * </p>
     *
     * @param targetId 已解析并去重的目标用户ID，不是操作人ID
     * @return 仅包含用户ID及学院的用户对象；目标用户不存在时返回 null
     */
    User lockScoreTargetUser(@Param("targetId") Integer targetId);

    /**
     * 查询项目分类下指定教研室的评分
     *
     * @param projectId 考核项目ID
     * @param category 任务分类
     * @param department 教研室名称
     * @return 指定教研室在项目分类下的评分列表
     */
    List<Integer> selectScoreByDepartment(@Param("pId") Integer projectId, @Param("category") String category, @Param("department") String department);

    /**
     * 查询项目分类下的教研室和学院评分
     *
     * @param projectId 考核项目ID
     * @param category 任务分类
     * @return 项目分类下按教研室及学院汇总的评分列表
     */
    List<Integer> selectScoreByDepartmentAndCollege(@Param("pId") Integer projectId, @Param("category") String category);

    /**
     * 查询项目中指定教师的评分
     *
     * @param projectId 考核项目ID
     * @param teacher 教师用户ID字符串
     * @return 指定教师在项目中的评分列表
     */
    List<Integer> selectScoreByTeacher(@Param("pId") Integer projectId, @Param("teacher") String teacher);

    /**
     * 查询项目中指定教师的任务ID
     *
     * @param projectId 考核项目ID
     * @param teacherId 教师编号
     * @return 指定教师在项目中的任务ID列表
     */
    List<String> selectTaskByProjectAndTeacher(@Param("pId") Integer projectId,@Param("teacherId") String teacherId);

}
