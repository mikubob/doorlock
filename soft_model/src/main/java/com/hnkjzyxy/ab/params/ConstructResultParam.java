package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建设项目成果提交参数
 * 提交建设项目任务成果时的请求体
 *
 * @version 1.0
 * @author Spell a
 * @date 2023-05-12 15:10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConstructResultParam {

    /**
     * 建设项目ID
     */
    private Integer conId;

    /**
     * 任务ID
     */
    private Integer taskId;

}
