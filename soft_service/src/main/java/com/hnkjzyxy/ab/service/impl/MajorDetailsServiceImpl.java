package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.MajorDetailsMapper;
import com.hnkjzyxy.ab.model.MajorDetails;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.MajorDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * (MajorDetails)表服务实现类
 *
 * @author Binc
 * @since 2025-04-14 13:31:29
 */
@Service
public class MajorDetailsServiceImpl extends ServiceImpl<MajorDetailsMapper, MajorDetails> implements MajorDetailsService {
    /**
     * 专业录取名额数据访问接口
     */
    @Autowired
    private MajorDetailsMapper majorDetailsMapper;

    /**
     * 新增专业录取名额信息
     *
     * @param majorDetails 专业录取名额信息
     * @return 统一接口响应
     */
    public ApiResult addMajorDetails(MajorDetails majorDetails) {
        if (checkUnique(majorDetails.getMajorCode(), majorDetails.getMajorName(), majorDetails.getId())) {
            return ApiResult.error("专业编码或名称已存在");
        }
        majorDetails.setDelFlag(1);
        majorDetails.setAccepted(0);
        majorDetailsMapper.insert(majorDetails);
        return ApiResult.ok();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult updateMajorDetails(MajorDetails majorDetails) {
        if (checkUnique(majorDetails.getMajorCode(), majorDetails.getMajorName(), majorDetails.getId())) {
            return ApiResult.error("专业编码或名称已存在");
        }
        majorDetailsMapper.updateById(majorDetails);
        return ApiResult.ok();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult deleteMajorDetails(Long id) {
        majorDetailsMapper.deleteById(id);
        return ApiResult.ok();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResult getMajorDetails() {
        List<MajorDetails> majorDetailsList = majorDetailsMapper.selectMajorDetails();
        return ApiResult.ok("data", majorDetailsList);
    }

    /**
     * 检查专业的唯一性
     * <p>
     * 该方法用于检查给定的专业代码和名称是否在数据库中唯一它通过查询数据库中是否存在具有相同代码或名称的专业来实现
     * 如果存在，则返回true表示专业编码或名称已存在如果excludeId不为空，则排除指定的ID进行检查，这通常用于更新专业信息时的唯一性验证
     *
     * @param code      专业代码
     * @param name      专业名称
     * @param excludeId 要排除的ID，通常用于更新验证时排除当前专业ID
     * @return 如果存在唯一性冲突，返回true；否则返回false
     */
    private boolean checkUnique(String code, String name, Long excludeId) {
        // 创建查询条件构造器
        LambdaQueryWrapper<MajorDetails> wrapper = new LambdaQueryWrapper<>();
        // 设置查询条件：专业代码或名称相等
        wrapper.and(w -> w.eq(MajorDetails::getMajorCode, code)
                .or()
                .eq(MajorDetails::getMajorName, name));

        // 如果excludeId不为空，则添加条件排除指定ID
        if (excludeId != null) {
            wrapper.ne(MajorDetails::getId, excludeId);
        }
        // 使用selectCount查询符合条件的专业数量
        int count = majorDetailsMapper.selectCount(wrapper);
        // 如果数量大于0，表示专业编码或名称已存在
        return count > 0;
    }
}