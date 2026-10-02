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
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 20:15
 */
@SpringBootTest
public class TestMain {

    Map<String, Integer> roleNames = new HashMap<String, Integer>() {{
        put("云计算教研室", 10);
        put("软件技术教研室", 11);
        put("虚拟现实教研室", 15);
        put("区块链教研室", 16);
    }};
    @Autowired
    private UserExcelImportService userExcelImportService;
    @Resource
    private TaskService taskService;
    @Resource
    private SnowFlowUtils snowFlowUtils;

    @Test
//    @Transactional
    void readUsers() {
        String fileUrl = "E:\\桌面\\软件学院教职员工工作考核管理系统\\用户基础数据导入V1.0.xls";
        userExcelImportService.readUserExcel(fileUrl);
    }

    @Test
    void readExcel() {
        TaskDataListener listener = new TaskDataListener(taskService, 1, snowFlowUtils);
        String path = "E:\\桌面\\软件学院教职员工工作考核管理系统\\项目导入模板文件.xlsx";
        EasyExcel.read(path, TaskModel.class, listener).doReadAll();
        List<TaskModel> excels = listener.getData();
        excels.forEach(System.out::println);
    }

    @Test
    void testMd5Hex() {
        String s = DigestUtils.md5Hex("2462452984:3765039755:com.hnkjzyxy.ab.mapper.MenuMapper.getNoticeListByPage:0:2147483647:select id,title,content,create_name createName,read_count readCount,status,create_time createTime from sys_notice where status = 1 order by createTime desc limit ?,?:0:10:MybatisSqlSessionFactoryBean");
        System.out.println(s);
    }

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
