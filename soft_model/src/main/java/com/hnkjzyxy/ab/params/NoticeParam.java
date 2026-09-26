package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 公告发布参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticeParam {

    /**
     * 公告标题
     */
    @NotBlank(message = "标题不能为空！")
    private String title;

    /**
     * 公告内容
     */
    @NotBlank(message = "内容不能为空！")
    private String content;

    /**
     * 创建人姓名
     */
    private String createName;

    /**
     * 是否置顶（0=否，1=是）
     */
    private Integer isTop = 0;

}
