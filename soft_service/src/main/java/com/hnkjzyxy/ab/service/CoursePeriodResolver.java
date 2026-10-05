package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.config.SchoolScheduleProperties;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.vo.ScheduleInterval;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 课表日期及节次解析。连续节次保留内部课间，多段节次分别占用。
 * 未知日期、节次或作息边界抛出异常，调用方不得当作无课。
 */
@Component
public class CoursePeriodResolver {
    /**
     * 学校时区。
     */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /**
     * 作息配置。
     */
    private final SchoolScheduleProperties properties;

    /**
     * 创建解析器。
     * @param properties 学校作息配置
     */
    public CoursePeriodResolver(SchoolScheduleProperties properties) { this.properties = properties; }

    /**
     * 解析严格日期。
     * @param value OA 或手工日期
     * @return 学校日期
     */
    public LocalDate date(String value) {
        if (value == null) throw new IllegalArgumentException("课程日期为空");
        String text = value.trim();
        if (text.matches("\\d{8}")) return LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE);
        if (!text.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) throw new IllegalArgumentException("课程日期无法解析：" + text);
        String[] parts = text.split("[-/]");
        return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    /**
     * 生成课程占用区间。
     * @param course 课程
     * @return 每组连续节次的区间
     */
    public List<ScheduleInterval> resolve(CourseSchedule course) {
        LocalDate day = date(course.getClassDate());
        String raw = course.getClassPeriod();
        if (raw == null || !raw.trim().matches("\\d+(?:\\s*-\\s*\\d+)?(?:\\s*[,、;]\\s*\\d+(?:\\s*-\\s*\\d+)?)*")) {
            throw new IllegalArgumentException("课程节次无法解析：" + raw);
        }
        SortedSet<Integer> periods = new TreeSet<>();
        for (String segment : raw.trim().split("[,、;]")) {
            String[] ends = segment.trim().split("\\s*-\\s*");
            int first = Integer.parseInt(ends[0]);
            int last = ends.length == 1 ? first : Integer.parseInt(ends[1]);
            if (first < 1 || last < first || last > 30) throw new IllegalArgumentException("课程节次范围错误");
            for (int i = first; i <= last; i++) periods.add(i);
        }
        List<ScheduleInterval> result = new ArrayList<>();
        List<Integer> ordered = new ArrayList<>(periods);
        for (int i = 0; i < ordered.size(); i++) {
            int first = ordered.get(i), last = first;
            while (i + 1 < ordered.size() && ordered.get(i + 1) == last + 1) last = ordered.get(++i);
            String start = properties.getStarts().get(first), end = properties.getEnds().get(last);
            if (start == null || end == null) throw new IllegalArgumentException("节次 " + first + "-" + last + " 的官方作息未配置");
            LocalDateTime from = day.atTime(LocalTime.parse(start)), to = day.atTime(LocalTime.parse(end));
            if (!to.isAfter(from)) throw new IllegalArgumentException("作息结束必须晚于开始");
            result.add(new ScheduleInterval(from, to));
        }
        return result;
    }

    /**
     * 判断排程冲突，保留端点相接也冲突的现行规则。
     * @param a 第一安排
     * @param b 第二安排
     * @return 是否冲突
     */
    public static boolean overlaps(ScheduleInterval a, ScheduleInterval b) {
        return !a.getEnd().isBefore(b.getStart()) && !a.getStart().isAfter(b.getEnd());
    }

    /**
     * 判断当前占用，采用半开区间。
     * @param interval 安排
     * @param now 学校当前时间
     * @return 是否正在占用
     */
    public static boolean contains(ScheduleInterval interval, LocalDateTime now) {
        return !now.isBefore(interval.getStart()) && now.isBefore(interval.getEnd());
    }
}
