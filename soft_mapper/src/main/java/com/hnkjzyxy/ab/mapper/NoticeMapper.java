package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Notice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * 通知公告数据访问接口
 */
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 分页查询通知公告
     *
     * @param page 页码，从一开始
     * @param limit 每页条数
     * @return 通知公告列表
     */
    @Select("select id,title,content,create_name createName,read_count readCount,status,create_time createTime from sys_notice " +
            "where status = 1 order by createTime desc limit #{page},#{limit}")
    List<Notice> getNoticeListByPage(@Param("page") Integer page, @Param("limit") Integer limit);

    /**
     * 查询通知公告总数
     *
     * @return 查询得到的数值
     */
    @Select("select count(*) from sys_notice where status = 1")
    Integer getNoticeTotal();

    /**
     * 按ID查询通知公告
     *
     * @param id 通知公告ID
     * @return 通知公告信息
     */
    @Select("select * from sys_notice where id = #{id} and status = 1")
    Notice getNoticeById(@Param("id") Integer id);

    /**
     * 增加通知公告浏览次数
     *
     * @param id 通知公告ID
     */
    @Update("update sys_notice set read_count = read_count+1 where id = #{id}")
    void addPageView(@Param("id") Integer id);

    /**
     * 写入用户消息通知
     *
     * @param nickName 用户姓名
     * @param msg 提示信息
     */
    @Insert("insert sys_notice(title,content,create_name) values(#{msg},#{msg},#{nickName})")
    void noticeInfo(@Param("nickName") String nickName, String msg);
}
