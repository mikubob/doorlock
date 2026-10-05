package com.hnkjzyxy.ab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学校作息配置。默认仅采用现有项目已知的双节边界，不猜测单节作息。
 * 校方核实后可配置每一节的起止时间，并更换规则版本。
 */
@Data
@Component
@ConfigurationProperties(prefix = "school.schedule")
public class SchoolScheduleProperties {
    /**
     * 创建服务器学校时钟，测试可替换为固定时刻。
     * @return 上海业务时钟
     */
    @org.springframework.context.annotation.Bean("schoolBusinessClock")
    public java.time.Clock schoolBusinessClock() { return java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai")); }
    /**
     * 作息版本，修改配置后必须重新同步课表。
     */
    private String version = "legacy-pairs-v1";
    /**
     * 快照有效秒数。
     */
    private int snapshotTtlSeconds = 60;
    /**
     * 完整课表覆盖最长可信小时数。
     */
    private int coverageMaxAgeHours = 24;
    /**
     * 完整来源范围经核实后才开启可信空档判断。
     */
    private boolean coverageConfirmed = false;
    /**
     * 已核实的完整学期覆盖开始日期。
     */
    private String coverageStart;
    /**
     * 已核实的完整学期覆盖结束日期。
     */
    private String coverageEnd;
    /**
     * OA 校区编码到规范校区名称的已核实映射。
     */
    private Map<String, String> campusAliases = new LinkedHashMap<>();
    /**
     * 各节开始时间，未配置的节次保持未知。
     */
    private Map<Integer, String> starts = defaults(true);
    /**
     * 各节结束时间，未配置的节次保持未知。
     */
    private Map<Integer, String> ends = defaults(false);

    /**
     * 将真实规则内容纳入发布版本，避免改作息但忘记改版本号后沿用旧覆盖。
     * @return 规则版本及内容指纹
     */
    public String policyKey() {
        String value = new java.util.TreeMap<>(starts) + "|" + new java.util.TreeMap<>(ends) + "|"
                + new java.util.TreeMap<>(campusAliases) + "|" + coverageStart + "|" + coverageEnd;
        return version + ":" + org.springframework.util.DigestUtils.md5DigestAsHex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 建立仓库现有的作息边界。
     *
     * @param start 是否生成开始边界
     * @return 已知边界
     */
    private static Map<Integer, String> defaults(boolean start) {
        String[] values = start ? new String[]{"08:20", "10:15", "14:00", "15:55", "17:50", "19:55"}
                : new String[]{"09:55", "11:50", "15:35", "17:30", "19:35", "21:40"};
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            result.put(i * 2 + (start ? 1 : 2), values[i]);
        }
        return result;
    }
}
