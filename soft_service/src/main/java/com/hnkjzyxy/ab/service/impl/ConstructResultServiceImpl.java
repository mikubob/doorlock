package com.hnkjzyxy.ab.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.ConstructResultMapper;
import com.hnkjzyxy.ab.model.ConstructResult;
import com.hnkjzyxy.ab.service.ConstructResultService;
import com.hnkjzyxy.ab.utils.UploadUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;


/**
 * 建设项目提交结果Service实现类
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/10 12:03
 */
@Service
public class ConstructResultServiceImpl extends ServiceImpl<ConstructResultMapper, ConstructResult> implements ConstructResultService {

    /**
     * 上传、下载及材料压缩工具
     */
    @Resource
    private UploadUtils uploadUtils;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void removeConstructResult(String id) {
        if (Objects.isNull(id)) {
            throw new RuntimeException("建设项目结果id不能为空！");
        }
        ConstructResult constructResult = this.getById(id);
        if (ObjectUtil.isNotNull(constructResult)) {
            this.removeById(constructResult.getId());
            uploadUtils.isConFile(constructResult.getResultPath());
        }
    }
}

