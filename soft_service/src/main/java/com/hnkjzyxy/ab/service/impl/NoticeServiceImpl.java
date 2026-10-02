package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.NoticeMapper;
import com.hnkjzyxy.ab.model.Message;
import com.hnkjzyxy.ab.model.Notice;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.NoticeParam;
import com.hnkjzyxy.ab.service.MessageService;
import com.hnkjzyxy.ab.service.NoticeService;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.params.PageQueryParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

/**
 * 通知公告Service实现类
 *
 * @author 16702
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    /**
     * 通知公告数据访问接口
     */
    @Resource
    private NoticeMapper noticeMapper;
    /**
     * 用户消息业务服务
     */
    @Resource
    private MessageService messageService;
    /**
     * 考核项目业务服务
     */
    @Resource
    private ProjectService projectService;
    /**
     * 用户业务服务
     */
    @Resource
    private UserService userService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getNoticeListByPage(PageQueryParam page) {
        HashMap<String, Object> map = new HashMap<>();
        List<Notice> list = noticeMapper.getNoticeListByPage((page.getPage().intValue() - 1) * page.getLimit().intValue(), page.getLimit().intValue());
        Integer total = noticeMapper.getNoticeTotal();
        map.put("total", total);
        map.put("list", list);
        return map;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Notice getNoticeDetailById(Integer id) {
        return noticeMapper.getNoticeById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void addPageView(Integer id) {
        noticeMapper.addPageView(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void publishNotice(NoticeParam param) {
        //判断是否置顶
        if (param.getIsTop().equals(1)) {
            Notice notice = noticeMapper.selectOne(new QueryWrapper<Notice>().eq("is_top", 1));
            if (ObjectUtil.isNotNull(notice)) {
                notice.setIsTop(0);
                noticeMapper.updateById(notice);
            }
        }
        Notice notice = new Notice();
        BeanUtil.copyProperties(param, notice);
        notice.setContent(param.getContent());
        noticeMapper.insert(notice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void noticeReturn(ResultItem resultItem) {
        Project project = projectService.getById(resultItem.getPId());
        User user = userService.getById(resultItem.getUId());
        Message message = Message.builder()
                .title("打回通知")
                .userId(resultItem.getUId())
                .content("项目名称：" + project.getTitle())
                .createName(user.getNickName())
                .build();
        messageService.save(message);
    }

}
