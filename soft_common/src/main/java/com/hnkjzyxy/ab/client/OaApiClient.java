package com.hnkjzyxy.ab.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.config.OaProperties;
import com.hnkjzyxy.ab.exception.OaApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * OA 开放平台调用客户端
 * <p>
 * 由原静态工具类 {@code OaRequestAPIUtils} 改造而来，职责与变化：
 * <ul>
 *   <li>由 Spring 容器管理，凭据从 {@link OaProperties} 注入，源码中不再出现任何密钥；</li>
 *   <li>请求体由 ObjectMapper 序列化，不再手工拼接 JSON 字符串；</li>
 *   <li>access_token 进程内缓存，避免每次调用都请求一次鉴权接口；</li>
 *   <li>调用失败一律抛 {@link OaApiException} 并携带原因，不再吞异常返回 null / 空串；</li>
 *   <li>日志脱敏，不再把 token 与完整报文打到标准输出。</li>
 * </ul>
 * </p>
 *
 * @version 1.0
 * @date 2026-10-01
 */
@Slf4j
@Component
public class OaApiClient {

    /**
     * OA 业务成功码
     */
    private static final int BIZ_CODE_SUCCESS = 10000;

    private final OaProperties props;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 缓存 access_token，提前 5 分钟过期，避免边界失效
     */
    private volatile String cachedToken;

    /**
     * 缓存到期时间戳（毫秒）
     */
    private volatile long expireAt;

    /**
     * 构造函数
     * @param props OA 配置
     */
    public OaApiClient(OaProperties props) {
        this.props = props;
    }

    /**
     * 获取 OA 访问令牌（带进程内缓存）
     *
     * @return 访问令牌，非空
     * @throws OaApiException 鉴权失败时抛出
     */
    public String getAccessToken() {
        String token = cachedToken;
        if (token != null && System.currentTimeMillis() < expireAt) {
            return token;
        }
        synchronized (this) {
            if (cachedToken != null && System.currentTimeMillis() < expireAt) {
                return cachedToken;
            }
            String fresh = requestAccessToken();
            cachedToken = fresh;
            expireAt = System.currentTimeMillis() + props.getTokenCacheSeconds() * 1000L;
            log.debug("已获取 OA token（已脱敏）：{}，缓存 {} 秒", mask(fresh), props.getTokenCacheSeconds());
            return fresh;
        }
    }

    /**
     * 向 OA 鉴权接口请求 access_token
     *
     * @return 访问令牌，非空
     * @throws OaApiException 网络异常 / HTTP 非 200 / 业务码非 10000 / 响应缺少令牌时抛出
     */
    private String requestAccessToken() {
        HttpURLConnection connection = null;
        try {
            Map<String, String> body = new HashMap<>(4);
            body.put("key", props.getAppKey());
            body.put("secret", props.getAppSecret());
            byte[] payload = objectMapper.writeValueAsBytes(body);

            URL targetUrl = new URL(props.buildTokenUrl());
            connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(props.getConnectTimeout());
            connection.setReadTimeout(props.getReadTimeout());
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new OaApiException("OA 鉴权失败，HTTP " + responseCode, responseCode);
            }

            String responseBody = readBody(connection);
            JsonNode root = objectMapper.readTree(responseBody);
            int code = root.path("code").asInt();
            String message = root.path("message").asText("");
            if (code != BIZ_CODE_SUCCESS) {
                throw new OaApiException("OA 返回业务错误 code=" + code + ", message=" + message, null, code, null);
            }
            String token = root.path("result").path("access_token").asText(null);
            if (token == null || token.trim().isEmpty()) {
                throw new OaApiException("OA 鉴权响应中缺少 access_token 字段");
            }
            return token;
        } catch (OaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OaApiException("调用 OA 鉴权接口异常：" + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 查询指定班级当天的请假人数
     *
     * @param className 班级名称，需与 OA 系统中保持一致
     * @return 请假人数
     * @throws OaApiException 调用失败时抛出
     */
    public int queryHotelDataByToday(String className) {
        HttpURLConnection connection = null;
        try {
            String accessToken = getAccessToken();
            URL targetUrl = new URL(props.buildHotelUrl(accessToken));
            connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(props.getConnectTimeout());
            connection.setReadTimeout(props.getReadTimeout());
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new OaApiException("OA 请假数据接口调用失败，HTTP " + responseCode, responseCode);
            }

            String responseBody = readBody(connection);
            JsonNode root = objectMapper.readTree(responseBody);
            int code = root.path("code").asInt();
            String message = root.path("message").asText("");
            if (code != BIZ_CODE_SUCCESS) {
                throw new OaApiException("OA 返回业务错误 code=" + code + ", message=" + message, null, code, null);
            }

            JsonNode dataNode = root.path("result").path("data");
            if (!dataNode.isArray()) {
                log.warn("OA 请假数据响应中 result.data 不是数组，按 0 人处理");
                return 0;
            }
            int result = 0;
            for (JsonNode node : dataNode) {
                // 检查班级名称是否匹配上
                if (className != null && className.contains(node.path("BJMC").asText(""))) {
                    result++;
                }
            }
            return result;
        } catch (OaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OaApiException("调用 OA 请假数据接口异常：" + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 获取电子班牌数据（result.data 数组）
     * <p>
     * 返回字段说明：SKRQ 上课日期，XQ 校区，JZWMC 教学楼，SKDD 上课地点，JSH 教室号，
     * JGH 授课教师工号，JSXM 授课教师姓名，SZDWMC 授课教师部门，KCMC 课程名称，
     * KKXND 学年，KKXQM 学期，ZC 周次，XQJ 星期几，SKJC 上课节次，
     * BJMC 上课班级名称，FDYXM 辅导员姓名，JXBRS 教学班人数，QJRS 请假人数，SFYQJRS 是否有请假人数。
     * </p>
     * <p>
     * 使用 HttpURLConnection 调用 OA 接口，获取返回结果中的 result.data 数据。
     * </p>
     *
     * @param xq    校区，可为空
     * @param jzwmc 教学楼，可为空
     * @return result.data 数组的 JSON 字符串（保证非 null，业务上无数据时为空数组 {@code []}）
     * @throws OaApiException 调用失败 / 业务码错误 / 响应结构异常时抛出
     */
    public String getClassBoardData(String xq, String jzwmc) {
        HttpURLConnection connection = null;
        try {
            // 1. 构造请求参数（所有参数均为非必填）
            Map<String, String> requestParams = new HashMap<>(4);
            if (xq != null && !xq.isEmpty() && jzwmc != null && !jzwmc.isEmpty()) {
                requestParams.put("XQ", xq);
                requestParams.put("JZWMC", jzwmc);
            }
            byte[] payload = objectMapper.writeValueAsBytes(requestParams);

            // 2. 构造目标地址（token 由客户端内部获取并在内存中缓存）
            String accessToken = getAccessToken();
            URL targetUrl = new URL(props.buildClassBoardUrl(accessToken));
            connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(props.getConnectTimeout());
            connection.setReadTimeout(props.getReadTimeout());
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
                outputStream.flush();
            }

            // 3. 校验 HTTP 响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new OaApiException("OA 班牌数据接口调用失败，HTTP " + responseCode, responseCode);
            }

            // 4. 解析响应，提取 result.data
            String responseBody = readBody(connection);
            JsonNode rootNode = objectMapper.readTree(responseBody);
            int code = rootNode.path("code").asInt();
            String message = rootNode.path("message").asText("");
            if (code != BIZ_CODE_SUCCESS) {
                throw new OaApiException("OA 返回业务错误 code=" + code + ", message=" + message, null, code, null);
            }
            JsonNode dataNode = rootNode.path("result").path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                throw new OaApiException("OA 响应中未找到 result.data 字段");
            }
            if (!dataNode.isArray()) {
                throw new OaApiException("OA 响应的 result.data 不是数组");
            }
            int count = dataNode.size();
            log.debug("OA 班牌数据拉取完成，共 {} 条", count);
            return objectMapper.writeValueAsString(dataNode);
        } catch (OaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OaApiException("调用 OA 班牌数据接口异常：" + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 读取响应体（UTF-8，try-with-resources 保证流被释放）
     *
     * @param connection 已建立连接的 HttpURLConnection
     * @return 响应体文本
     * @throws Exception 读取失败时抛出
     */
    private String readBody(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
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
