package com.hnkjzyxy.ab.service.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.utils.SnowFlowUtils;
import org.springframework.beans.BeanUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 21:16
 */
public class CheckResultDataListener extends AnalysisEventListener<CheckResultModel> {

    private static final int BATCH_COUNT = 100;
    /**
     * 记录解析的数据总数
     */
    int count = 0;
    private CheckResultService checkResultService;
    private SnowFlowUtils snowFlowUtils;

    /**
     * 用于接收解析的所有数据
     */
    private List<CheckResultModel> data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public CheckResultDataListener(CheckResultService checkResultService, SnowFlowUtils snowFlowUtils) {
        this.checkResultService = checkResultService;
        this.snowFlowUtils = snowFlowUtils;
    }

    /**
     * 提供一个对外访问的方法
     *
     * @return 已解析的数据列表
     */
    public List<CheckResultModel> getData() {
        return data;
    }

    /**
     * 每解析一行调用一次
     *
     * @param objects         当前行解析结果
     * @param analysisContext 解析上下文
     */
    @Override
    public void invoke(CheckResultModel objects, AnalysisContext analysisContext) {
        //log.info("解析到一条数据:{}", JSON.toJSONString(data));
        data.add(objects);
        count++;
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (data.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    /**
     * 所有数据解析完毕后执行的操作
     *
     * @param analysisContext 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        System.out.println("解析完毕，共" + (count - 1) + "条数据");
    }

    public void saveData() {
        List<CheckResult> checkResults = data.stream().map(item -> {
            CheckResult checkResult = new CheckResult();
            checkResult.setId(snowFlowUtils.nextId());


            //TODO 这里应该是没有读到数据的，需要排查原因
            BeanUtils.copyProperties(item, checkResult);

            System.out.println(checkResult + "---?");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            try {
                System.out.println(item.getDate());
                Date parse = format.parse(item.getDate());
                System.out.println(parse);
                checkResult.setDate(parse);
            } catch (ParseException e) {
                throw new RuntimeException("日期格式不正确，请使用2024-05-05这种格式");
            }

            checkResult.setShouldArrival(Integer.parseInt(item.getShouldArrival()));
            checkResult.setArrival(Integer.parseInt(item.getArrival()));
            checkResult.setFoodBringPerson(Integer.parseInt(item.getFoodBringPerson()));


            checkResult.setIsViolate(0);
            checkResult.setIsNormal(0);
            checkResult.setIsLate(0);
            if ("是".equals(item.getIsLate())) {
                checkResult.setIsLate(1);

            }
            if ("是".equals(item.getIsNormal())) {
                checkResult.setIsNormal(1);
            }


            if ("是".equals(item.getIsViolate())) {
                checkResult.setIsViolate(1);
            }


            //判断是否有违规记录，如果有的话，要添加到remark字段中
            StringJoiner joiner = new StringJoiner(",");
            if (item.getIsLate().length() > 2) {
                joiner.add(item.getIsLate());
            }
            if (item.getIsNormal().length() > 2) {
                joiner.add(item.getIsNormal());
            }
            if (item.getIsViolate().length() > 2) {
                joiner.add(item.getIsViolate());
            }

            checkResult.setRemark(joiner.toString());
            return checkResult;
        }).collect(Collectors.toList());
        checkResultService.saveBatch(checkResults);
        // 存储完成清理 list
        data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }
}
