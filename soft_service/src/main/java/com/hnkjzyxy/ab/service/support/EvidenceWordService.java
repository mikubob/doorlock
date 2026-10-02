package com.hnkjzyxy.ab.service.support;

import com.alibaba.fastjson.JSON;
import com.hnkjzyxy.ab.utils.FilePathUtils;
import com.hnkjzyxy.ab.utils.PdfToWordConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * 佐证材料 Word 生成服务
 * <p>
 * 负责本地临时文件生成，不操作 HTTP 响应；生成成功后由调用方回收文件。
 * </p>
 */
@Slf4j
@Service
public class EvidenceWordService {
    /**
     * 佐证材料文件存储根目录
     */
    private final String fileRoot;

    /**
     * 初始化佐证材料 Word 生成服务
     *
     * @param fileRoot upload.fileUrl 配置指定的佐证材料文件存储根目录
     */
    public EvidenceWordService(@Value("${upload.fileUrl}") String fileRoot) {
        this.fileRoot = fileRoot;
    }

    /**
     * 生成佐证材料 Word 临时文件
     * <p>
     * 逐个读取佐证材料所在目录并调用 PDF 转 Word 工具，保留原有目录转换规则。
     * 生成过程中发生异常时删除临时 Word 文件；生成成功后由下载组件负责回收。
     * </p>
     *
     * @param evidences 佐证材料访问路径的 JSON 字符串数组，路径以 /pdf/file/ 为前缀
     * @return 生成的 Word 临时文件；调用方使用后负责删除
     * @throws RuntimeException 路径解析、临时文件创建或 PDF 转换失败时抛出
     */
    public File generate(String evidences) {
        File temporary = null;
        try {
            temporary = File.createTempFile("tempFile", ".docx");
            List<String> paths = JSON.parseArray(evidences, String.class);
            String prefix = FilePathUtils.getRealFilePath("/pdf/file/");
            for (String path : paths) {
                File file = new File(fileRoot + path.substring(prefix.length()));
                // 保留原有按佐证材料所在目录生成 Word 的规则。
                PdfToWordConverter.pdfFilesToWordFile(file.getParentFile().getAbsolutePath(),
                        temporary.getAbsolutePath());
            }
            return temporary;
        } catch (Exception e) {
            if (temporary != null && temporary.exists() && !temporary.delete()) {
                log.warn("生成失败后的临时文件删除失败：{}", temporary);
            }
            log.error("生成word文件失败！", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
