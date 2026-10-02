package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 分页查询基础参数
 *
 * @author 16702
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageQueryParam {

    /**
     * 当前页码
     */
    @NotNull(message = "请传入当前页")
    private Long page = 1L;

    /**
     * 每页条数
     */
    @NotNull(message = "请传入当前页大小")
    private Long limit = 10L;
}
