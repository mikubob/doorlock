package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.InfoMapper;
import com.hnkjzyxy.ab.model.Info;
import com.hnkjzyxy.ab.service.InfoService;
import org.springframework.stereotype.Service;

/**
 * 考核资料Service实现类
 *
 * @author 16702
 */
@Service
public class InfoServiceImpl extends ServiceImpl<InfoMapper, Info> implements InfoService {
}
