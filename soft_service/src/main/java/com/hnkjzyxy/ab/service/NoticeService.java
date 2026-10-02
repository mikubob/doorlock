package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Notice;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.params.NoticeParam;
import com.hnkjzyxy.ab.params.PageQueryParam;

import java.util.Map;

public interface NoticeService extends IService<Notice> {

    Map<String, Object> getNoticeListByPage(PageQueryParam page);

    Notice getNoticeDetailById(Integer id);

    void addPageView(Integer id);

    void publishNotice(NoticeParam param);

    void noticeReturn(ResultItem resultItem);
}
