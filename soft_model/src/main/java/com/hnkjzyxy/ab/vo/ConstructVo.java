package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建设项目分类项
 * 用于建设项目成果填报时的层级选择
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/5/8 17:21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConstructVo {

    /**
     * 分类/任务名称
     */
    private String titleName;

    /**
     * 是否为任务项（0=否，1=是）
     */
    private Integer isTask;

}
