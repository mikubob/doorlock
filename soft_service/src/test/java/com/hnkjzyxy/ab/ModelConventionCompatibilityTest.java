package com.hnkjzyxy.ab;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.dto.excel.CheckResultModel;
import com.hnkjzyxy.ab.dto.excel.CourseModel;
import com.hnkjzyxy.ab.dto.excel.TaskModel;
import com.hnkjzyxy.ab.exception.ImportRejectedException;
import com.hnkjzyxy.ab.exception.RowParseException;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.SwitchRecord;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.impl.ProjectTaskGuardImpl;
import com.hnkjzyxy.ab.vo.ResultVo;
import com.hnkjzyxy.ab.vo.StudentInfoVo;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 目录与 Lombok 整理的行为兼容性验证
 * <p>
 * 验证原有构造签名、JSON 属性、Excel 映射、特殊实体语义和接口代理事务边界。
 * </p>
 */
class ModelConventionCompatibilityTest {

    /**
     * 验证首字母缩写属性及兼容构造方式仍可供调用方使用
     *
     * @throws Exception JSON 转换失败时抛出
     */
    @Test
    void preservesConstructorSignaturesAndJsonProperties() throws Exception {
        Task task = new Task("task-1", 7, "分类", "任务", "标准", 10, 0, 1, "备注", null);
        task.setSourceProjectItemId(8);
        assertEquals(7, task.getPId());
        assertEquals(8, task.getSourceProjectItemId());
        ResultVo result = new ResultVo(9, 7, "材料", 1, "10", "意见", 0,
                "user", "姓名", Collections.<Result>emptyList());
        assertEquals(9, result.getUId());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode taskJson = mapper.valueToTree(task);
        assertEquals(7, taskJson.get("pid").asInt());
        assertEquals(8, taskJson.get("sourceProjectItemId").asInt());
        JsonNode resultJson = mapper.valueToTree(result);
        assertEquals(9, resultJson.get("uid").asInt());
        assertEquals("task-1", mapper.treeToValue(taskJson, Task.class).getId());
        assertEquals(9, mapper.treeToValue(resultJson, ResultVo.class).getUId());
        CourseModel course = new CourseModel("1", "2", "3", "A101", "一班", "40", "课程", "辅导员", "教师");
        assertEquals("40", course.getShould_arrival());
        assertNull(course.getCollege());
    }

    /**
     * 验证主键相等规则及两种录取状态赋值方式不受 Lombok 影响
     */
    @Test
    void preservesIdentityEqualityAndStatusConversion() {
        SwitchRecord first = new SwitchRecord(1, 2, 3, 0, LocalDateTime.of(2026, 10, 2, 9, 0));
        SwitchRecord second = new SwitchRecord(1, 9, 8, 1, null);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        SwitchRecord unsaved = new SwitchRecord(2, 3, 1, null);
        assertNull(unsaved.getSwitchId());
        assertEquals(unsaved, unsaved);
        assertNotEquals(unsaved, new SwitchRecord());
        StudentInfoVo student = new StudentInfoVo();
        student.setStatus(Integer.valueOf(0));
        assertEquals("未录取", student.getStatus());
        student.setStatus(Integer.valueOf(1));
        assertEquals("已录取", student.getStatus());
        student.setStatus("人工结果");
        assertEquals("人工结果", student.getStatus());
        student.setStatus(Integer.valueOf(2));
        assertEquals("人工结果", student.getStatus());
    }

    /**
     * 验证巡查与任务模型迁移后的 Excel 写入读取仍使用原列映射
     */
    @Test
    void preservesExcelColumnMapping() {
        CheckResultModel row = new CheckResultModel();
        row.setDate("2026-10-02");
        row.setShouldArrival("40");
        row.setArrival("39");
        row.setIsLate("否");
        row.setCheckPerson("巡查人");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, CheckResultModel.class).sheet("巡查").doWrite(Collections.singletonList(row));
        List<CheckResultModel> rows = EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .head(CheckResultModel.class).sheet().doReadSync();
        assertEquals(1, rows.size());
        assertEquals("2026-10-02", rows.get(0).getDate());
        assertEquals("40", rows.get(0).getShouldArrival());
        assertEquals("39", rows.get(0).getArrival());
        assertEquals("否", rows.get(0).getIsLate());
        assertEquals("巡查人", rows.get(0).getCheckPerson());
        TaskModel task = new TaskModel("分类", "任务", "标准", 10, 1, 0, "备注");
        ByteArrayOutputStream taskOutput = new ByteArrayOutputStream();
        EasyExcel.write(taskOutput, TaskModel.class).sheet("任务").doWrite(Collections.singletonList(task));
        List<TaskModel> tasks = EasyExcel.read(new ByteArrayInputStream(taskOutput.toByteArray()))
                .head(TaskModel.class).sheet().doReadSync();
        assertEquals(1, tasks.size());
        assertEquals("任务", tasks.get(0).getTaskName());
        assertEquals(10, tasks.get(0).getScore());
        assertEquals(1, tasks.get(0).getIsFile());
    }

    /**
     * 验证移入通用模块的异常仍保留原消息、原因及字段信息
     */
    @Test
    void preservesImportExceptionDetails() {
        RowParseException row = new RowParseException(12, "应到人数", "格式不正确");
        assertEquals("第 12 行：格式不正确", row.getMessage());
        assertEquals(12, row.getRowIndex());
        assertEquals("应到人数", row.getFieldName());
        assertEquals("格式不正确", row.getReason());
        assertEquals("模板错误", new RowParseException(0, "模板错误").getMessage());
        RuntimeException cause = new RuntimeException("原始异常");
        ImportRejectedException rejected = new ImportRejectedException("导入拒绝", cause);
        assertEquals("导入拒绝", rejected.getReason());
        assertEquals("导入拒绝", rejected.getMessage());
        assertSame(cause, rejected.getCause());
    }

    /**
     * 验证服务接口代理仍能读取实现类的强制事务及独立事务配置
     *
     * @throws Exception 方法签名不存在时抛出
     */
    @Test
    void preservesTransactionBoundariesThroughServiceInterface() throws Exception {
        AnnotationTransactionAttributeSource source = new AnnotationTransactionAttributeSource();
        TransactionAttribute lock = source.getTransactionAttribute(
                ProjectTaskGuard.class.getMethod("lock", Integer.class), ProjectTaskGuardImpl.class);
        assertNotNull(lock);
        assertEquals(TransactionDefinition.PROPAGATION_MANDATORY, lock.getPropagationBehavior());
        TransactionAttribute staging = source.getTransactionAttribute(
                ProjectTaskGuard.class.getMethod("recordStaging", Integer.class, User.class, List.class),
                ProjectTaskGuardImpl.class);
        assertNotNull(staging);
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, staging.getPropagationBehavior());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, staging.getIsolationLevel());
        assertTrue(staging.rollbackOn(new Exception("失败")));
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(lock)).thenThrow(new IllegalTransactionStateException("必须已有事务"));
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(staging)).thenReturn(status);
        ProxyFactory factory = new ProxyFactory(new ProjectTaskGuardImpl());
        factory.setInterfaces(ProjectTaskGuard.class);
        factory.addAdvice(new TransactionInterceptor(manager, source));
        ProjectTaskGuard proxy = (ProjectTaskGuard) factory.getProxy();
        assertThrows(IllegalTransactionStateException.class, () -> proxy.lock(1));
        assertThrows(RuntimeException.class, () -> proxy.recordStaging(null, new User(), Collections.emptyList()));
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
    }
}
