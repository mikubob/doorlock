package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户列表查询参数
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserQueryParam extends PageQueryParam {

    /**
     * 用户名称（工号 / 姓名，模糊查询）
     */
    private String name;

}
