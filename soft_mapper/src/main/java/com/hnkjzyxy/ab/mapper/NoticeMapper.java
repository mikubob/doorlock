package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Notice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知公告数据访问接口
 */
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 分页查询通知公告
     *
     * @param page 查询起始偏移量，从零开始
     * @param limit 每页条数
     * @return 通知公告列表
     */
    List<Notice> getNoticeListByPage(@Param("page") Integer page, @Param("limit") Integer limit);

    /**
     * 查询通知公告总数
     *
     * @return 有效通知公告的记录数
     */
    default Integer getNoticeTotal() {
        return selectCount(new LambdaQueryWrapper<Notice>());
    }

    /**
     * 按ID查询通知公告
     *
     * @param id 通知公告ID
     * @return 通知公告信息
     */
    default Notice getNoticeById(@Param("id") Integer id) {
        return selectById(id);
    }

    /**
     * 增加通知公告浏览次数
     *
     * @param id 通知公告ID
     */
    void addPageView(@Param("id") Integer id);

    /**
     * 写入用户消息通知
     *
     * @param nickName 用户姓名
     * @param msg 提示信息
     */
    void noticeInfo(@Param("nickName") String nickName, @Param("msg") String msg);
}
