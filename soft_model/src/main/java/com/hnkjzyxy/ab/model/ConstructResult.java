package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 建设项目成果
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/5/8 15:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConstructResult {

    /**
     * 成果ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 建设项目任务ID
     */
    @NotNull(message = "建设项目任务id不能为空！")
    @JsonAlias(value = "conId")
    private Integer conId;

    /**
     * 成果名称
     */
    @NotBlank(message = "成果名不能为空！")
    private String resultName;

    /**
     * 成果文件路径
     */
    private String resultPath;

    /**
     * 状态
     */
    @TableLogic
    private Integer status;

}
