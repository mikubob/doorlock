package com.hnkjzyxy.ab.result;

import java.util.HashMap;
import java.util.Map;

public class ApiResult extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public ApiResult() {
        put("code", 200);
        put("msg", "success");
    }

    public static ApiResult error() {
        return error(444, "未知异常，请联系管理员");
    }

    public static ApiResult error(String msg) {
        return error(400, msg);
    }

    public static ApiResult error(int code, String msg) {
        ApiResult apiResult = new ApiResult();
        apiResult.put("code", code);
        apiResult.put("msg", msg);
        return apiResult;
    }

    public static ApiResult ok(String msg) {
        ApiResult apiResult = new ApiResult();
        apiResult.put("msg", msg);
        return apiResult;
    }

    public static ApiResult ok(Map<String, Object> map) {
        ApiResult apiResult = new ApiResult();
        apiResult.putAll(map);
        return apiResult;
    }

    public static ApiResult ok() {
        return new ApiResult();
    }

    public static ApiResult ok(String key, Object value) {
        ApiResult apiResult = new ApiResult();
        apiResult.put("code", 200);
        apiResult.put("msg", "success");
        apiResult.put(key, value);
        return apiResult;
    }

    public ApiResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
