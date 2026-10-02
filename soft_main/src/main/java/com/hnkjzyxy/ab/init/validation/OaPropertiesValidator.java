package com.hnkjzyxy.ab.init.validation;

import com.hnkjzyxy.ab.config.OaProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OA 应用凭据启动期校验器
 * <p>
 * 凭据缺失属于部署配置问题，应在启动期快速失败，而不是拖到定时同步时才暴露。
 * 因此本校验器在容器初始化阶段检查 {@code oa.app-key} / {@code oa.app-secret} 是否已配置，
 * 为空则直接抛出异常终止启动；配置正常时打印脱敏后的 key，便于运维核对。
 * </p>
 * <p>
 * 注意：默认配置中凭据取值为空占位符，正式环境需由环境变量
 * {@code OA_APP_KEY} / {@code OA_APP_SECRET} 注入。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Slf4j
@Component
public class OaPropertiesValidator implements InitializingBean {

    /**
     * OA 接口及应用凭据配置
     */
    private final OaProperties oaProperties;

    /**
     * 初始化OaPropertiesValidator
     *
     * @param oaProperties OA 接口及应用凭据配置
     */
    public OaPropertiesValidator(OaProperties oaProperties) {
        this.oaProperties = oaProperties;
    }

    /**
     * 在容器初始化时检查 OA 应用凭据
     * <p>
     * 凭据有效时记录脱敏后的应用 key，不输出应用 secret。
     * </p>
     *
     * @throws IllegalStateException oa.app-key 或 oa.app-secret 为空时抛出并终止初始化
     */
    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(oaProperties.getAppKey()) || !StringUtils.hasText(oaProperties.getAppSecret())) {
            throw new IllegalStateException(
                    "OA 应用凭据未配置：请检查配置项 oa.app-key / oa.app-secret"
                            + "（正式方案要求通过环境变量 OA_APP_KEY / OA_APP_SECRET 注入，禁止写回源码）");
        }
        log.info("OA 应用凭据已加载（key={}, baseUrl={}）", mask(oaProperties.getAppKey()), oaProperties.getBaseUrl());
    }

    /**
     * 脱敏展示：保留首尾 4 位，中间以 **** 替代
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    private static String mask(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
