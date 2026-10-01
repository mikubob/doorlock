package com.hnkjzyxy.ab.service.utils;

import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.model.ProjectItemSource;
import com.hnkjzyxy.ab.model.Task;

import java.util.Objects;

/**
 * 项目任务来源字段校验及映射工具
 * <p>
 * 供子项维护与任务导入共同使用，统一原始整数解析、空值处理、字符集校验和字段差量比较。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
public final class ProjectTaskRules {
    /**
     * 工具类私有构造方法，禁止实例化
     */
    private ProjectTaskRules() { }

    /**
     * 严格解析非负 int 范围的整数字符串
     *
     * @param value 原始字符串，允许首尾空白
     * @param field 用于错误提示的字段名称
     * @return 解析后的整数
     * @throws ProjectTaskException 字段为空、格式非法或超出 int 范围时抛出
     */
    public static int integer(String value, String field) {
        if (value == null || !value.trim().matches("[0-9]+")) {
            throw new ProjectTaskException(400, field + "必须是非负整数");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ProjectTaskException(400, field + "超出整数范围");
        }
    }

    /**
     * 归一化文本并校验长度与目标字符集
     * <p>
     * 去除首尾空白，空白文本统一为 null；非空文本最多255个字符，且不得包含 utf8mb3 无法保存的字符。
     * </p>
     *
     * @param value 原始文本
     * @param field 用于错误提示的字段名称
     * @param required 是否为必填字段
     * @return 归一化后的文本，可选空字段返回 null
     * @throws ProjectTaskException 必填文本为空、超长或包含不支持的字符时抛出
     */
    public static String text(String value, String field, boolean required) {
        String normalized = value == null ? null : value.trim();
        if (normalized != null && normalized.isEmpty()) normalized = null;
        if (required && normalized == null) throw new ProjectTaskException(400, field + "不能为空");
        if (normalized != null && (normalized.codePointCount(0, normalized.length()) > 255 ||
                normalized.codePoints().anyMatch(c -> c > 0xffff || (c >= 0xd800 && c <= 0xdfff)))) {
            throw new ProjectTaskException(400, field + "超长或包含目标字符集不支持的字符");
        }
        return normalized;
    }

    /**
     * 校验0/1标志并将空值归一化为0
     *
     * @param value 原始标志值
     * @param field 用于错误提示的字段名称
     * @return 校验后的0或1
     * @throws ProjectTaskException 标志既非0也非1时抛出
     */
    public static int flag(Integer value, String field) {
        if (value == null) return 0;
        if (value != 0 && value != 1) throw new ProjectTaskException(400, field + "仅允许0或1");
        return value;
    }

    /**
     * 校验来源父子关系并显式映射任务评分字段
     *
     * @param item 数据库中的原始子项
     * @param parent 同项目的顶层父分类
     * @param projectId 已校验的目标项目ID
     * @return 含来源ID和评分字段的任务，主键由导入服务生成或沿用
     * @throws ProjectTaskException 来源归属、层级或字段不合法时抛出
     */
    public static Task map(ProjectItemSource item, ProjectItemSource parent, Integer projectId) {
        String label = "子项" + item.getId() + ": ";
        if (integer(item.getProjectId(), label + "project_id") != projectId || !Objects.equals(item.getGrade(), 2) ||
                parent == null || !Objects.equals(item.getParentId(), parent.getId()) ||
                integer(parent.getProjectId(), label + "父分类project_id") != projectId ||
                !Objects.equals(parent.getGrade(), 1) || !Objects.equals(parent.getParentId(), 0)) {
            throw new ProjectTaskException(400, label + "来源或父分类归属/层级不合法");
        }
        Task task = new Task();
        task.setPId(projectId);
        task.setSourceProjectItemId(item.getId());
        task.setCategory(text(parent.getPname(), label + "分类名", true));
        task.setTaskName(text(item.getPname(), label + "名称", true));
        task.setStandard(text(item.getStandard(), label + "标准", true));
        task.setScore(integer(item.getScore(), label + "分值"));
        task.setIsFile(flag(item.getIsFile(), label + "is_file"));
        task.setIsExtend(flag(item.getIsExtend(), label + "is_extend"));
        task.setRemark(text(item.getRemark(), label + "备注", false));
        return task;
    }

    /**
     * 比较两个任务的导入评分字段
     * <p>
     * 比较分类、名称、标准、分值、文件及扩展标志和备注，不比较主键、来源ID或结果。
     * </p>
     *
     * @param a 当前任务
     * @param b 待导入任务
     * @return 按统一空值规则比较后相同时返回 true
     */
    public static boolean same(Task a, Task b) {
        return Objects.equals(a.getCategory(), b.getCategory()) && Objects.equals(a.getTaskName(), b.getTaskName()) &&
                Objects.equals(a.getStandard(), b.getStandard()) && Objects.equals(a.getScore(), b.getScore()) &&
                Objects.equals(a.getIsFile() == null ? 0 : a.getIsFile(), b.getIsFile() == null ? 0 : b.getIsFile()) &&
                Objects.equals(a.getIsExtend() == null ? 0 : a.getIsExtend(), b.getIsExtend() == null ? 0 : b.getIsExtend()) &&
                Objects.equals(blankRemark(a.getRemark()), blankRemark(b.getRemark()));
    }

    /**
     * 归一化备注以便比较可选字段
     *
     * @param value 原始备注
     * @return 去除首尾空白的备注，空白时返回 null
     */
    private static String blankRemark(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
