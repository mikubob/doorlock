package com.hnkjzyxy.ab.exception;

import com.hnkjzyxy.ab.result.ApiResult;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResult handler(NoHandlerFoundException e) {
        return ApiResult.error(404, "您访问的页面崩溃了/(ㄒoㄒ)/~~");
    }

    @ExceptionHandler(IllegalAccessException.class)
    public ApiResult handler(IllegalAccessException e) {
        int code = 400;
        if ("token异常".equals(e.getMessage())) code = 401;
        else if ("请求达到限制".equals(e.getMessage())) code = 429;
        return ApiResult.error(code, e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult handler(HttpRequestMethodNotSupportedException e) {
        return ApiResult.error(405, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult handler(MethodArgumentNotValidException e) {
        return ApiResult.error(422, e.getFieldError().getDefaultMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult handler(MaxUploadSizeExceededException e) {
        e.printStackTrace();
        return ApiResult.error(400, "pdf文件大小不能超过50MB！");
    }

    @ExceptionHandler(BindException.class)
    public ApiResult handler(BindException e) {
        e.printStackTrace();
        return ApiResult.error(400, "数据绑定异常!");
    }

    @ExceptionHandler(RuntimeException.class)
    public ApiResult handler(RuntimeException e) {
        e.printStackTrace();
        System.out.println(e.getMessage());
        return ApiResult.error(400, e.getMessage());
    }


}
