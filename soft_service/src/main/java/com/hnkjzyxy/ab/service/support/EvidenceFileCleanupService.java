package com.hnkjzyxy.ab.service.support;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 佐证材料文件清理服务，按数据库引用的文件名筛选本地冗余文件
 *
 * @author 16702
 */
@Service
public class EvidenceFileCleanupService {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(EvidenceFileCleanupService.class);
    /**
     * 佐证材料文件存储根目录
     */
    @Value("${upload.fileUrl}")
    private String fileUrl;
    /**
     * 考核结果数据访问接口
     */
    @Resource
    private ResultMapper resultMapper;

    /**
     * 清理未被数据库佐证材料引用的本地文件
     * <p>
     * 递归遍历配置的上传目录，按文件名与数据库记录进行比对并删除未引用的文件。
     * 本方法自身不检查启用开关，应由明确启用的文件清理任务调用。
     * </p>
     */
    public void clearFiles() {
        logger.info("---------正在清理文件！---------------");
        //数据库文件路径
        List<String> doFiles = new ArrayList<>();
        //本地文件路径
        List<File> toFiles = new ArrayList<>();

        //拿到数据库所有文件名
        List<String> resultVos = resultMapper.getResultEvidence();
        resultVos.forEach(item -> {
            List<Map<String, Object>> list = JSONArray.parseObject(item, List.class);
            list.forEach(map -> {
                Object o = map.entrySet().stream().findFirst().get().getValue();
                if (ObjectUtil.isNotNull(o)) {
                    List<String> strings = JSONArray.parseArray(o.toString(), String.class);
                    strings.forEach(value -> {
                        String url = value;
                        int i = url.lastIndexOf('/');
                        doFiles.add(url.substring(i + 1));
                    });
                }
            });
        });

        //拿到本地目录下的文件名
        getFileNames(fileUrl, toFiles);

        //过滤出数据库文件路径 不存在本地文件路径
        List<File> deleteFiles = toFiles.stream().filter(o -> !doFiles.contains(o.getName())).collect(Collectors.toList());
        deleteFiles.forEach(file -> {
            // 判断文件是否存在
            if (file.exists()) {
                // 判断是否为文件
                if (file.isFile()) {  //为文件时调用删除文件方法
                    file.delete();
                }
            }
        });
        logger.info("---------本次清理冗余文件共" + deleteFiles.size() + "个---------");
        logger.info("---------清理文件完成！---------------");
    }

    /**
     * 递归收集目录中的文件对象
     *
     * @param fileUrl 待遍历的本地目录路径
     * @param files 接收文件对象的列表，本方法将结果追加到该列表
     */
    public void getFileNames(String fileUrl, List<File> files) {
        File file = new File(fileUrl);
        if (file.exists() && file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                return;
            }
            for (File listFile : listFiles) {
                if (listFile.isDirectory()) {
                    getFileNames(listFile.getPath(), files);
                } else {
                    files.add(listFile);
                }
            }
        }
    }

}