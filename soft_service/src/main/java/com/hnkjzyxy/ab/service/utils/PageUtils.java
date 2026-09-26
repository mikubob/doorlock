package com.hnkjzyxy.ab.service.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 16702
 */
public class PageUtils<T> {

    public static <T> Map<String, Object> page(List<T> list, int page, int rows) {
        //总页数
        int totalPage = 0;
        //数据
        List<T> listSort = new ArrayList<>();
        try {
            int size = list.size();
            int pageStart = page == 1 ? 0 : (page - 1) * rows;//截取的开始位置
            int pageEnd = Math.min(size, page * rows);//截取的结束位置
            if (size > pageStart) {
                listSort = list.subList(pageStart, pageEnd);
            }
            if (rows != 0) {
                if (list.size() % rows == 0) {
                    totalPage = list.size() / rows;
                } else {
                    totalPage = list.size() / rows + 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("分页数据错误！请联系管理员！");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("total", list.size());
        map.put("list", listSort);
        return map;
    }


}
