package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Notice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface NoticeMapper extends BaseMapper<Notice> {

    @Select("select id,title,content,create_name createName,read_count readCount,status,create_time createTime from sys_notice " +
            "where status = 1 order by createTime desc limit #{page},#{limit}")
    List<Notice> getNoticeListByPage(@Param("page") Integer page, @Param("limit") Integer limit);

    @Select("select count(*) from sys_notice where status = 1")
    Integer getNoticeTotal();

    @Select("select * from sys_notice where id = #{id} and status = 1")
    Notice getNoticeById(@Param("id") Integer id);

    @Update("update sys_notice set read_count = read_count+1 where id = #{id}")
    void addPageView(@Param("id") Integer id);

    @Insert("insert sys_notice(title,content,create_name) values(#{msg},#{msg},#{nickName})")
    void noticeInfo(@Param("nickName") String nickName, String msg);
}
