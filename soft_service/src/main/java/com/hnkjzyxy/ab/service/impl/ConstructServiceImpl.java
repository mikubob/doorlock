package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.mapper.ConstructMapper;
import com.hnkjzyxy.ab.model.Construct;
import com.hnkjzyxy.ab.model.ConstructResult;
import com.hnkjzyxy.ab.params.ConstructQueryParam;
import com.hnkjzyxy.ab.service.ConstructResultService;
import com.hnkjzyxy.ab.service.ConstructService;
import com.hnkjzyxy.ab.utils.UploadUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 建设项目Service实现类
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/8 15:38
 */
@Service
public class ConstructServiceImpl extends ServiceImpl<ConstructMapper, Construct> implements ConstructService {

    /**
     * 建设项目数据访问接口
     */
    @Resource
    private ConstructMapper constructMapper;
    /**
     * 建设项目提交结果业务服务
     */
    @Resource
    private ConstructResultService constructResultService;
    /**
     * 上传、下载及材料压缩工具
     */
    @Resource
    private UploadUtils uploadUtils;


    /**
     * {@inheritDoc}
     */
    @Override
    @CacheEvict(value = {HnkjxyConstants.CONSTRUCT_YEARS,
            HnkjxyConstants.CONSTRUCT_LIST, HnkjxyConstants.CONSTRUCT_USER_LIST, HnkjxyConstants.CONSTRUCT_USER_YEARS,
            HnkjxyConstants.CONSTRUCT_DETAIL}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void addConstruct(Construct construct) {
        //递归添加
        try {
            saveConstruct(construct);
            if ((ObjectUtil.isNotEmpty(construct.getUIds()) && construct.getUIds().size() > 0)) {
                String replace = JSONArray.toJSONString(construct.getUIds()).replace("[", "").replace("]", "");
                Integer count = constructMapper.getConstructById(construct.getId());
                if (count.intValue() > 0) {
                    constructMapper.updateConstructByUId(construct.getId(), replace);
                } else {
                    constructMapper.addConstructByUId(construct.getId(), replace);
                }
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException("创建失败！");
        }
    }

    /**
     * 递归保存建设项目及其子项目
     *
     * @param construct 建设项目信息
     */
    @Transactional
    public void saveConstruct(Construct construct) {
        this.saveOrUpdate(construct);
        List<Construct> children = construct.getChildren();
        if (ObjectUtil.isNotEmpty(children) && children.size() > 0) {
            children.forEach(item -> {
                item.setPId(construct.getId());
                saveConstruct(item);
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {HnkjxyConstants.CONSTRUCT_DETAIL}, key = "#id")
    public Construct getConstructById(String id) {
        if (ObjectUtil.isNull(id)) {
            throw new RuntimeException("建设项目id不能为空！");
        }
        Construct construct = this.getById(id);
        if (ObjectUtil.isEmpty(construct)) {
            throw new RuntimeException("建设项目不存在！");
        }
        //递归拿取
        setConstruct(construct);
        String ids = constructMapper.getConstructUIdsById(construct.getId());
        construct.setUIds(JSONArray.parseArray("[" + ids + "]", Integer.class));
        //JSON.parseArray()
        return construct;
    }

    /**
     * 递归补充建设项目的子项目
     *
     * @param construct 建设项目信息
     */
    public void setConstruct(Construct construct) {
        List<Construct> children = this.list(new QueryWrapper<Construct>().eq("p_id", construct.getId()));
        if (ObjectUtil.isNotEmpty(children) && children.size() > 0) {
            construct.setChildren(children);
            children.forEach(this::setConstruct);
        }
        if (construct.getIsTask().intValue() == 1) {
            construct.setOutcomesList(constructResultService.list(new QueryWrapper<ConstructResult>().eq("con_id", construct.getId())));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {HnkjxyConstants.CONSTRUCT_YEARS}, key = "#userId")
    public List<String> getConstructYears(Integer userId) {
        return new ArrayList<>(constructMapper.getConstructYears(userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {HnkjxyConstants.CONSTRUCT_LIST}, key = "#param.getYear() + '-' + #param.getPage()+ '-' +#param.getLimit()+ '-' + #param.getUserId()+ '-' + #param.getTitleName()")
    public Map<String, Object> getConstructList(ConstructQueryParam param) {
        Page<Construct> page = new Page<>(param.getPage(), param.getLimit());
        LambdaQueryWrapper<Construct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Construct::getPId, 0);
        wrapper.eq(Construct::getUId, param.getUserId());
        checkWrapper(param, wrapper);
        this.page(page, wrapper);
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {HnkjxyConstants.CONSTRUCT_USER_YEARS}, key = "#userId")
    public List<String> getConstructYearsByUser(Integer userId) {
        if (ObjectUtil.isNull(userId)) {
            throw new RuntimeException("用户id不能为空！");
        }
        List<Integer> conIds = constructMapper.getConIds(userId);
        if (ObjectUtil.isEmpty(conIds) && conIds.size() == 0) {
            return new ArrayList<>();
        }
        QueryWrapper<Construct> wrapper = new QueryWrapper<>();
        wrapper.in("id", conIds);
        wrapper.eq("p_id", 0);
        wrapper.orderByDesc("year");
        List<String> list = new ArrayList<>(constructMapper.getConstructYearsByUser(wrapper));
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {HnkjxyConstants.CONSTRUCT_USER_LIST}, key = "#param.getYear() + '-' + #param.getUserId()+ '-' +#param.getPage()+ '-' +#param.getLimit()+ '-' +#param.getTitleName()")
    public Map<String, Object> getConstructListByUserId(ConstructQueryParam param) {
        List<Integer> conIds = constructMapper.getConIds(param.getUserId());
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", 0);
        map.put("list", new ArrayList<>());
        if (ObjectUtil.isEmpty(conIds) && conIds.size() == 0) {
            return map;
        }
        Page<Construct> page = new Page<>(param.getPage(), param.getLimit());
        LambdaQueryWrapper<Construct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Construct::getPId, 0);
        checkWrapper(param, wrapper);
        wrapper.in(Construct::getId, conIds);
        this.page(page, wrapper);
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return map;
    }

    /**
     * 将建设项目查询参数应用到查询条件
     *
     * @param param 建设项目操作或查询参数
     * @param wrapper 数据库查询条件
     */
    private void checkWrapper(ConstructQueryParam param, LambdaQueryWrapper<Construct> wrapper) {
        if (StrUtil.isNotBlank(param.getYear())) {
            wrapper
                    .ge(Construct::getCreateTime, param.getYear().concat("-01-01 00:00:00"))
                    .le(Construct::getCreateTime, param.getYear().concat("-12-31 23:59:59"));
        }
        if (StrUtil.isNotBlank(param.getTitleName())) {
            wrapper.like(Construct::getTitleName, param.getTitleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = {HnkjxyConstants.CONSTRUCT_USER_LIST, HnkjxyConstants.CONSTRUCT_USER_YEARS}, allEntries = true)
    public void assignmentConstruct(ConstructQueryParam param) {
        String replace = JSONArray.toJSONString(param.getUserIds()).replace("[", "").replace("]", "");
        Integer id = constructMapper.getConstructById(param.getConId());
        if (id.intValue() > 0) {
            constructMapper.updateConstructByUId(param.getConId(), replace);
        } else {
            constructMapper.addConstructByUId(param.getConId(), replace);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @CacheEvict(value = {HnkjxyConstants.CONSTRUCT_DETAIL}, allEntries = true)
    @Transactional
    public ConstructResult subConstructResult(ConstructResult newResult) {
        ConstructResult constructResult = null;
        if (ObjectUtil.isNotNull(newResult.getId())) {
            constructResult = constructResultService.getById(newResult.getId());
        }
        constructResultService.saveOrUpdate(newResult);
        if (ObjectUtil.isNotNull(constructResult) && !constructResult.getResultPath().equals(newResult.getResultPath())) {
            uploadUtils.isConFile(constructResult.getResultPath());
        }
        return newResult;
    }

}