package com.hnkjzyxy.ab.params;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 项目提醒参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/4/17 16:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticeQueryParam {

    /**
     * 接收用户ID
     */
    @NotNull(message = "用户id不能为空！")
    @JsonAlias(value = "uId")
    private Integer uId;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    @JsonAlias(value = "pId")
    private Integer pId;

}
