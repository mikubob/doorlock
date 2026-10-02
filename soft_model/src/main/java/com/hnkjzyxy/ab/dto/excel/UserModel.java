package com.hnkjzyxy.ab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户基础信息 Excel 导入行模型
 * 保留模板列名、列索引及单元格原始值，供导入监听器解析
 *
 * @version 1.0
 * @email 1670203784@qq.com
 * @author Spell a
 * @date 2024-01-04 20:18
 */
@Getter
@Setter
@NoArgsConstructor
public class UserModel {
    /**
     * 姓名
     */
    @ExcelProperty("姓名")
    private String nickName;

    /**
     * 联系电话
     */
    @ExcelProperty("联系电话")
    private String phone;

    /**
     * 财务工号
     */
    @ExcelProperty("财务工号")
    private String userName;

    /**
     * 教研室
     */
    @ExcelProperty("教研室")
    private String major;

    /**
     * 角色
     */
    @ExcelProperty("角色")
    private String roleName;

    /**
     * 是否为教研室主任
     */
    @ExcelProperty("是否为教研室主任")
    private String isDirector;

    /**
     * 返回导入行的原有日志描述
     *
     * @return 行字段描述
     */
    @Override
    public String toString() {
        return "UserModel{" +
                "nickName='" + nickName + '\'' +
                ", phone='" + phone + '\'' +
                ", userName='" + userName + '\'' +
                ", major='" + major + '\'' +
                ", roleName='" + roleName + '\'' +
                ", isDirector='" + isDirector + '\'' +
                '}';
    }
}
