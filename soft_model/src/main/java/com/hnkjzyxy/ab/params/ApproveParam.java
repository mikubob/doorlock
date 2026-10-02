package com.hnkjzyxy.ab.params;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hnkjzyxy.ab.params.PageQueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 审批查询参数
 *
 * @author 16702
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApproveParam extends PageQueryParam {

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    @JsonAlias("pId")
    @Min(value = 0, message = "项目id不能为空")
    private Integer pId;

    /**
     * 用户ID
     */
    @JsonAlias("uId")
    private Integer uId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 状态
     */
    @JsonAlias("status")
    private Integer status;

}
