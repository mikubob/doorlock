package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 项目考核查询参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-12 14:31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectQueryParam {

    /**
     * 当前页码
     */
    private int page = 1;

    /**
     * 每页条数
     */
    private int limit = 10;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    private Integer projectId;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 用户ID
     */
    @NotNull(message = "用户id不能为空！")
    private Integer userId;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 教研室（角色）ID
     */
    private Integer roleId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 状态
     */
    private Integer status;
}
