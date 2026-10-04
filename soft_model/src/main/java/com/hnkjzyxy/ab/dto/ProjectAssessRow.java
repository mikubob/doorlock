package com.hnkjzyxy.ab.dto;

import com.hnkjzyxy.ab.vo.ProjectVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据库当前页考核记录，使用宽类型接收聚合分数
 * <p>
 * 服务层校验总分范围后转换为原ProjectVo，不将内部聚合字段暴露给接口。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectAssessRow extends ProjectVo {
    /**
     * 包含全部结果状态的汇总分数，空分数归零
     */
    private Long totalScore;
}
