package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.ConstructResult;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/10 12:02
 */
public interface ConstructResultService extends IService<ConstructResult> {

    void removeConstructResult(String id);
}
