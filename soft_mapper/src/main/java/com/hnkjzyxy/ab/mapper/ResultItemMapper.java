package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.ResultItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * 审批明细数据访问接口
 */
public interface ResultItemMapper extends BaseMapper<ResultItem> {

    /**
     * 查询结果在指定审批步骤的审批明细
     *
     * @param id 审批明细ID
     * @param sort 审批步骤排序值
     * @return 审批明细信息
     */
    @Select("select * from sys_result_item where parent_id = #{id} and step = #{sort} order by create_time desc")
    ResultItem findResultItem(@Param("id") Integer id, @Param("sort") Integer sort);

    /**
     * 查询结果的审批明细列表
     *
     * @param id 审批明细ID
     * @return 审批明细列表
     */
    @Select("select * from sys_result_item where parent_id = #{id} order by create_time desc")
    List<ResultItem> findResultItemList(@Param("id") Integer id);

    /**
     * 查询结果最高审批步骤对应的评分信息
     *
     * @param id 审批明细ID
     * @return 审批明细信息
     */
    @Select("select A.score from sys_result_item A where id = (select max(step) from sys_result_item where parent_id = #{id} and is_flag = 0) and parent_id = #{id} and is_flag = 0")
    ResultItem getMaxStepApproveScore(@Param("id") Integer id);

    /**
     * 新增审批步骤的评分及意见记录
     *
     * @param parentId 父记录ID
     * @param sort 审批步骤排序值
     * @param score 评分值
     * @param userId 用户ID
     * @param opinion 审批意见
     * @param is_flag 是否退回标记
     */
    @Insert("insert sys_result_item(parent_id,step,score,user_id,opinion,is_flag) values(#{parentId},#{sort},#{score},#{userId},#{opinion},#{isFlag})")
    void submitApprove(@Param("parentId") Integer parentId, @Param("sort") Integer sort,
                       @Param("score") String score, @Param("userId") Integer userId, @Param("opinion") String opinion, @Param("isFlag") Integer is_flag);


    //查询 审核结果表，1是已审批
    /**
     * 统计指定项目、被考核人和审批人的已审批步骤记录
     *
     * @param pId 考核项目ID
     * @param uId 被考核用户ID
     * @param userId 用户ID
     * @param step 审批步骤
     * @return 查询得到的数值
     */
    @Select("select count(*) from sys_result_item where u_id = #{uId} and p_id = #{pId} and step = #{step} " +
            "and user_id = #{userId} and status = 1")
    Integer selectStep(Integer pId, Integer uId, Integer userId, int step);

    /**
     * 查询指定项目、用户和步骤下的待处理退回意见
     *
     * @param pId 考核项目ID
     * @param uId 被考核用户ID
     * @param step 审批步骤
     * @return 审批明细列表
     */
    @Select("select * from sys_result_item where u_id = #{uId} and p_id = #{pId} and step = #{step} and is_flag = 1 and status = 0")
    List<ResultItem> findResultOpinion(Integer pId, Integer uId, Integer step);
}
