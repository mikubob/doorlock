package com.hnkjzyxy.ab.handler;

import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import com.hnkjzyxy.ab.result.ApiResult;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器，将控制器相关异常转换为统一的接口响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 PDF 转 Word 专用异常，生成失败时返回 JSON 而非附件。
     *
     * @param e 携带安全公开提示的转换异常
     * @return HTTP 与业务码一致的响应；并发繁忙时提示重试时间
     */
    @ExceptionHandler(PdfConversionException.class)
    public ResponseEntity<ApiResult> handler(PdfConversionException e) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(e.getStatus());
        if ("busy".equals(e.getStage())) {
            response.header("Retry-After", "5");
        }
        return response.body(ApiResult.error(e.getStatus(), e.getMessage()));
    }

    /**
     * 处理成绩入口身份无效及权限不足异常
     * <p>
     * 同时设置实际 HTTP 状态与响应体业务码，不将预期权限拒绝包装成通用参数错误。
     * 本处理器只覆盖进入 MVC 异常处理链的专用异常，不改变认证过滤器的响应行为。
     * </p>
     *
     * @param e 携带401或403状态码及可读提示的身份权限异常
     * @return HTTP 状态与业务错误码一致的统一错误响应
     */
    @ExceptionHandler(AuthPermissionException.class)
    public ResponseEntity<ApiResult> handler(AuthPermissionException e) {
        // 由异常类型精确分流，保留数据库及其他运行时异常原有的处理契约。
        return ResponseEntity.status(e.getStatus())
                .body(ApiResult.error(e.getStatus(), e.getMessage()));
    }

    /**
     * 处理项目任务导入及生命周期校验异常
     *
     * @param e 项目任务业务异常
     * @return HTTP 状态与业务错误码一致的响应
     */
    @ExceptionHandler(ProjectTaskException.class)
    public ResponseEntity<ApiResult> handler(ProjectTaskException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ApiResult.error(e.getStatus(), e.getMessage()));
    }

    /**
     * 将请求路径不存在转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResult handler(NoHandlerFoundException e) {
        return ApiResult.error(404, "您访问的页面崩溃了/(ㄒoㄒ)/~~");
    }

    /**
     * 将非法访问转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(IllegalAccessException.class)
    public ApiResult handler(IllegalAccessException e) {
        int code = 400;
        if ("token异常".equals(e.getMessage())) code = 401;
        else if ("请求达到限制".equals(e.getMessage())) code = 429;
        return ApiResult.error(code, e.getMessage());
    }

    /**
     * 将请求方法不支持转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult handler(HttpRequestMethodNotSupportedException e) {
        return ApiResult.error(405, e.getMessage());
    }

    /**
     * 将请求体参数校验失败转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult handler(MethodArgumentNotValidException e) {
        return ApiResult.error(422, e.getFieldError().getDefaultMessage());
    }

    /**
     * 将上传文件超过大小限制转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult handler(MaxUploadSizeExceededException e) {
        e.printStackTrace();
        return ApiResult.error(400, "pdf文件大小不能超过50MB！");
    }

    /**
     * 将请求参数绑定失败转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResult handler(BindException e) {
        e.printStackTrace();
        return ApiResult.error(400, "数据绑定异常!");
    }

    /**
     * 将业务运行异常转换为统一接口响应
     *
     * @param e 待处理异常
     * @return 统一接口响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResult handler(RuntimeException e) {
        e.printStackTrace();
        System.out.println(e.getMessage());
        return ApiResult.error(400, e.getMessage());
    }


}
