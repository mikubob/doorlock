package com.hnkjzyxy.ab.export;

import com.hnkjzyxy.ab.service.support.EvidenceWordService;
import com.hnkjzyxy.ab.utils.UploadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 佐证材料 Word 下载组件
 * <p>
 * 负责下载响应及临时文件回收，PDF 转换由 EvidenceWordService 承担。
 * </p>
 */
@Slf4j
@Component
public class EvidenceWordExporter {
    /**
     * 佐证材料 Word 生成服务
     */
    private final EvidenceWordService evidenceWordService;

    /**
     * 初始化佐证材料 Word 下载组件
     *
     * @param evidenceWordService 负责生成临时 Word 文件的服务
     */
    public EvidenceWordExporter(EvidenceWordService evidenceWordService) {
        this.evidenceWordService = evidenceWordService;
    }

    /**
     * 将佐证材料 Word 写入 HTTP 下载响应
     * <p>
     * 调用生成服务取得临时文件，使用原下载工具输出文件内容及响应头。
     * 下载结束或下载异常时均尝试删除临时 Word 文件，生成失败的文件由生成服务回收。
     * </p>
     *
     * @param evidences 佐证材料访问路径的 JSON 字符串数组
     * @param response 接收 Word 文件的 HTTP 响应
     * @throws RuntimeException Word 生成或下载失败时抛出
     */
    public void export(String evidences, HttpServletResponse response) {
        File temporary = evidenceWordService.generate(evidences);
        try {
            UploadUtils.download(response, temporary.getPath());
        } catch (IOException e) {
            log.error("下载word文件失败！", e);
            throw new RuntimeException(e);
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                log.warn("临时文件删除失败：{}", temporary);
            }
        }
    }
}
