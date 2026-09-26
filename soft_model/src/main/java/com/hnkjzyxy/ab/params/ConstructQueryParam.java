package com.hnkjzyxy.ab.params;

import com.hnkjzyxy.ab.vo.QueryPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 建设项目查询参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/5/8 17:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConstructQueryParam extends QueryPage {

    /**
     * 建设项目ID
     */
    private Integer conId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 任务标题
     */
    private String titleName;

    /**
     * 用户ID列表
     */
    private List<Integer> userIds;

    /**
     * 年份
     */
    private String year;
}
