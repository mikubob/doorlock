package com.hnkjzyxy.ab.params;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hnkjzyxy.ab.params.PageQueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 未提交人员查询参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/4/18 17:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserParam extends PageQueryParam {

    /**
     * 用户名（工号）
     */
    private String name;

    /**
     * 用户昵称（姓名）
     */
    private String nickName;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    @JsonAlias(value = "pId")
    private Integer pId;
}
