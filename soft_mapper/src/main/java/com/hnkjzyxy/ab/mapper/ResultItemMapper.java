package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.ResultItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface ResultItemMapper extends BaseMapper<ResultItem> {

    @Select("select * from sys_result_item where parent_id = #{id} and step = #{sort} order by create_time desc")
    ResultItem findResultItem(@Param("id") Integer id, @Param("sort") Integer sort);

    @Select("select * from sys_result_item where parent_id = #{id} order by create_time desc")
    List<ResultItem> findResultItemList(@Param("id") Integer id);

    @Select("select A.score from sys_result_item A where id = (select max(step) from sys_result_item where parent_id = #{id} and is_flag = 0) and parent_id = #{id} and is_flag = 0")
    ResultItem getMaxStepApproveScore(@Param("id") Integer id);

    @Insert("insert sys_result_item(parent_id,step,score,user_id,opinion,is_flag) values(#{parentId},#{sort},#{score},#{userId},#{opinion},#{isFlag})")
    void submitApprove(@Param("parentId") Integer parentId, @Param("sort") Integer sort,
                       @Param("score") String score, @Param("userId") Integer userId, @Param("opinion") String opinion, @Param("isFlag") Integer is_flag);


    //查询 审核结果表，1是已审批
    @Select("select count(*) from sys_result_item where u_id = #{uId} and p_id = #{pId} and step = #{step} " +
            "and user_id = #{userId} and status = 1")
    Integer selectStep(Integer pId, Integer uId, Integer userId, int step);

    @Select("select * from sys_result_item where u_id = #{uId} and p_id = #{pId} and step = #{step} and is_flag = 1 and status = 0")
    List<ResultItem> findResultOpinion(Integer pId, Integer uId, Integer step);
}
