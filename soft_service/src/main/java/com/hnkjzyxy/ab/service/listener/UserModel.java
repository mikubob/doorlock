package com.hnkjzyxy.ab.service.listener;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 20:18
 */
public class UserModel {
    @ExcelProperty("姓名")
    private String nickName;
    @ExcelProperty("联系电话")
    private String phone;
    @ExcelProperty("财务工号")
    private String userName;
    @ExcelProperty("教研室")
    private String major;
    @ExcelProperty("角色")
    private String roleName;
    @ExcelProperty("是否为教研室主任")
    private String isDirector;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getIsDirector() {
        return isDirector;
    }

    public void setIsDirector(String isDirector) {
        this.isDirector = isDirector;
    }

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
