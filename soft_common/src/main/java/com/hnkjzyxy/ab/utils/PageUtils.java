package com.hnkjzyxy.ab.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集合内存分页工具
 *
 * @param <T> 分页数据类型
 * @author 16702
 */
public class PageUtils<T> {

    /**
     * 对内存集合进行分页
     *
     * @param <T> 数据类型
     * @param list 原始数据集合
     * @param page 当前页码，从一开始
     * @param rows 每页条数
     * @return 包含 total 总条数及 list 当前页数据的映射
     * @throws RuntimeException 分页参数或集合操作导致分页失败时抛出
     */
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
