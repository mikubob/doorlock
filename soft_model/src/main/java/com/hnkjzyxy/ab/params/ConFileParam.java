package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 建设项目成果文件查询参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/5/9 14:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConFileParam {

    /**
     * 建设项目ID
     */
    @NotBlank(message = "建设项目id不能为空！")
    private String conId;

    /**
     * 任务ID
     */
    private String taskId;

}
