package com.hnkjzyxy.ab.result;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一接口响应，使用 code、msg 及业务数据字段描述请求结果
 */
public class ApiResult extends HashMap<String, Object> {
    /**
     * 序列化版本标识
     */
    private static final long serialVersionUID = 1L;

    /**
     * 初始化统一接口响应，使用 code、msg 及业务数据字段描述请求结果
     */
    public ApiResult() {
        put("code", 200);
        put("msg", "success");
    }

    /**
     * 构建失败接口响应
     *
     * @return 统一接口响应
     */
    public static ApiResult error() {
        return error(444, "未知异常，请联系管理员");
    }

    /**
     * 构建失败接口响应
     *
     * @param msg 提示信息
     * @return 统一接口响应
     */
    public static ApiResult error(String msg) {
        return error(400, msg);
    }

    /**
     * 构建失败接口响应
     *
     * @param code 业务状态码
     * @param msg 提示信息
     * @return 统一接口响应
     */
    public static ApiResult error(int code, String msg) {
        ApiResult apiResult = new ApiResult();
        apiResult.put("code", code);
        apiResult.put("msg", msg);
        return apiResult;
    }

    /**
     * 构建成功接口响应
     *
     * @param msg 提示信息
     * @return 统一接口响应
     */
    public static ApiResult ok(String msg) {
        ApiResult apiResult = new ApiResult();
        apiResult.put("msg", msg);
        return apiResult;
    }

    /**
     * 构建成功接口响应
     *
     * @param map 待写入的数据映射
     * @return 统一接口响应
     */
    public static ApiResult ok(Map<String, Object> map) {
        ApiResult apiResult = new ApiResult();
        apiResult.putAll(map);
        return apiResult;
    }

    /**
     * 构建成功接口响应
     *
     * @return 统一接口响应
     */
    public static ApiResult ok() {
        return new ApiResult();
    }

    /**
     * 构建成功接口响应
     *
     * @param key 数据键
     * @param value 数据值
     * @return 统一接口响应
     */
    public static ApiResult ok(String key, Object value) {
        ApiResult apiResult = new ApiResult();
        apiResult.put("code", 200);
        apiResult.put("msg", "success");
        apiResult.put(key, value);
        return apiResult;
    }

    /**
     * 添加响应字段并返回当前响应对象
     *
     * @param key 数据键
     * @param value 数据值
     * @return 统一接口响应
     */
    public ApiResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
