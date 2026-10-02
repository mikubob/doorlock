package com.hnkjzyxy.ab.service.utils;

/**
 * 考核分数分档计算工具
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-17 11:53
 */
public class ResultUtils {

    /**
     * 将比例分数乘以一百后按二十分区间转换为零至五档
     *
     * @param score 评分值
     * @return 按当前分段规则计算的评分档位
     */
    public static int calc(double score) {
        score *= 100;
        if (score >= 80) {
            return 5;
        }
        if (score >= 60 && score <= 80) {
            return 4;
        }
        if (score >= 40 && score <= 60) {
            return 3;
        }
        if (score >= 20 && score <= 40) {
            return 2;
        }
        if (score >= 0 && score <= 20) {
            return 1;
        }
        return 0;
    }

}
