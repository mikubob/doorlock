package com.hnkjzyxy.ab.params;

import com.hnkjzyxy.ab.vo.QueryPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 流程查询参数
 *
 * @author 16702
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowParam extends QueryPage implements Serializable {

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 年份
     */
    private String year;

}
