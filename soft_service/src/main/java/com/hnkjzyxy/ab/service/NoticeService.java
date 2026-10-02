package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Notice;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.params.NoticeParam;
import com.hnkjzyxy.ab.params.PageQueryParam;

import java.util.Map;

/**
 * 通知公告Service接口
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 分页查询通知公告
     *
     * @param page 页码，从一开始
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getNoticeListByPage(PageQueryParam page);

    /**
     * 查询通知公告详情
     *
     * @param id 通知公告ID
     * @return 通知公告信息
     */
    Notice getNoticeDetailById(Integer id);

    /**
     * 增加通知公告浏览次数
     *
     * @param id 通知公告ID
     */
    void addPageView(Integer id);

    /**
     * 发布通知公告并生成接收人消息
     *
     * @param param 通知公告操作或查询参数
     */
    void publishNotice(NoticeParam param);

    /**
     * 根据审批结果发送消息通知
     *
     * @param resultItem 审批明细信息
     */
    void noticeReturn(ResultItem resultItem);
}
