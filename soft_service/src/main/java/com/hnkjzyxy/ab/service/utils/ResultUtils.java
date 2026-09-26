package com.hnkjzyxy.ab.service.utils;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-17 11:53
 */
public class ResultUtils {

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
