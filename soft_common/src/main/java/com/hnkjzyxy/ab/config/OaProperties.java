package com.hnkjzyxy.ab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OA 开放平台调用参数配置
 * <p>
 * 对应 application-*.yml 中的 {@code oa.*} 节点。
 * 由 SoftApplication 上的 {@code @ConfigurationPropertiesScan} 自动注册，无需再加 {@code @Component}。
 * </p>
 * <p>
 * 安全约定：源码与 yml 中不允许出现真实的 OA 应用凭据，
 * 正式方案要求 {@code oa.app-key} / {@code oa.app-secret} 由环境变量
 * {@code OA_APP_KEY} / {@code OA_APP_SECRET} 注入（见配置节点内 TODO 说明）。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Data
@ConfigurationProperties(prefix = "oa")
public class OaProperties {

    /**
     * OA 开放平台基础地址（不含末尾斜杠），dev / test / prd 可各自覆盖
     */
    private String baseUrl = "https://dmp.hnkjxy.net.cn";

    /**
     * OA 应用 key（由 OA 侧签发）
     */
    private String appKey;

    /**
     * OA 应用 secret（由 OA 侧签发）
     */
    private String appSecret;

    /**
     * 建立连接超时（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 读取响应超时（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * access_token 内存缓存时长（秒），默认 3300（55 分钟），提前 5 分钟过期
     */
    private long tokenCacheSeconds = 3300L;

    /**
     * 单次请求返回条数上限，与课表同步的截断闸门共用同一语义
     */
    private int pageSize = 1000;

    /**
     * 拼接鉴权接口地址
     *
     * @return 鉴权接口完整地址
     */
    public String buildTokenUrl() {
        return baseUrl + "/open_api/authentication/get_access_token";
    }

    /**
     * 拼接学生请假数据接口地址
     *
     * @param accessToken 访问令牌
     * @return 请假数据接口完整地址
     */
    public String buildHotelUrl(String accessToken) {
        return baseUrl + "/open_api/customization/view_india/full?access_token=" + accessToken;
    }

    /**
     * 拼接电子班牌数据接口地址
     *
     * @param accessToken 访问令牌
     * @return 电子班牌数据接口完整地址
     */
    public String buildClassBoardUrl(String accessToken) {
        return baseUrl + "/open_api/customization/view_mike/full?access_token=" + accessToken
                + "&per_page=" + pageSize;
    }
}
