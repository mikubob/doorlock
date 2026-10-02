package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.service.excel.UserExcelImportService;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.dto.excel.TaskModel;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.service.TaskService;
import com.hnkjzyxy.ab.service.listener.TaskDataListener;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

/**
 * 原有应用集成调试测试，依赖本地模板文件及数据库
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 20:15
 */
@SpringBootTest
public class TestMain {

    /**
     * 导入时使用的角色名称与角色ID映射
     */
    Map<String, Integer> roleNames = new HashMap<String, Integer>() {{
        put("云计算教研室", 10);
        put("软件技术教研室", 11);
        put("虚拟现实教研室", 15);
        put("区块链教研室", 16);
    }};
    /**
     * 用户 Excel 导入服务
     */
    @Autowired
    private UserExcelImportService userExcelImportService;
    /**
     * TaskService业务服务
     */
    @Resource
    private TaskService taskService;
    /**
     * 雪花ID生成工具
     */
    @Resource
    private SnowFlowUtils snowFlowUtils;

    /**
     * 使用本地用户模板执行用户导入集成调试
     */
    @Test
//    @Transactional
    void readUsers() {
        String fileUrl = "E:\\桌面\\软件学院教职员工工作考核管理系统\\用户基础数据导入V1.0.xls";
        userExcelImportService.readUserExcel(fileUrl);
    }

    /**
     * 使用本地任务模板执行 Excel 读取集成调试
     */
    @Test
    void readExcel() {
        TaskDataListener listener = new TaskDataListener(taskService, 1, snowFlowUtils);
        String path = "E:\\桌面\\软件学院教职员工工作考核管理系统\\项目导入模板文件.xlsx";
        EasyExcel.read(path, TaskModel.class, listener).doReadAll();
        List<TaskModel> excels = listener.getData();
        excels.forEach(System.out::println);
    }

    /**
     * 输出缓存查询键的 MD5 调试值
     */
    @Test
    void testMd5Hex() {
        String s = DigestUtils.md5Hex("2462452984:3765039755:com.hnkjzyxy.ab.mapper.MenuMapper.getNoticeListByPage:0:2147483647:select id,title,content,create_name createName,read_count readCount,status,create_time createTime from sys_notice where status = 1 order by createTime desc limit ?,?:0:10:MybatisSqlSessionFactoryBean");
        System.out.println(s);
    }

    /**
     * 按项目任务分类查询聚合结果并输出调试信息
     */
    @Test
    void test1() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getPId, 32);
        wrapper.groupBy(Task::getCategory);
        Map<String, Object> map = taskService.getMap(wrapper);
        for (Map.Entry entry : map.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
    }

}
