package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 字符串参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-14 23:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringParam {

    /**
     * 电子签名文件地址
     */
    @NotBlank(message = "电子签名删除不能为空！")
    private String url;

}
