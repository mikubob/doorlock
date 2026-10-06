package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.config.SchoolScheduleProperties;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.vo.ScheduleInterval;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同一数据库快照内派生教室状态，排程占用和设备物理状态分开返回。
 */
@Service
public class ClassroomOccupancyService {
    /**
     * 教室目录。
     */
    private final ClassroomMapper roomMapper;
    /**
     * 课程目录。
     */
    private final CourseScheduleMapper courseMapper;
    /**
     * 考试目录。
     */
    private final ExamMapper examMapper;
    /**
     * 排程版本。
     */
    private final ScheduleStateMapper stateMapper;
    /**
     * 统一身份及覆盖规则。
     */
    private final ScheduleWriteCoordinator coordinator;
    /**
     * 时间解析。
     */
    private final CoursePeriodResolver resolver;
    /**
     * 学校策略。
     */
    private final SchoolScheduleProperties properties;
    /**
     * 可在确定性验证中替换的学校时钟。
     */
    private final Clock clock;

    /**
     * 创建占用查询服务。
     * @param roomMapper 教室访问
     * @param courseMapper 课程访问
     * @param examMapper 考试访问
     * @param stateMapper 状态访问
     * @param coordinator 共同规则
     * @param resolver 时间规则
     * @param properties 学校策略
     * @param clock 学校时钟
     */
    public ClassroomOccupancyService(ClassroomMapper roomMapper, CourseScheduleMapper courseMapper,
            ExamMapper examMapper, ScheduleStateMapper stateMapper, ScheduleWriteCoordinator coordinator,
            CoursePeriodResolver resolver, SchoolScheduleProperties properties,
            @Qualifier("schoolBusinessClock") Clock clock) {
        this.roomMapper = roomMapper; this.courseMapper = courseMapper; this.examMapper = examMapper;
        this.stateMapper = stateMapper; this.coordinator = coordinator; this.resolver = resolver; this.properties = properties; this.clock = clock;
    }

    /**
     * 获取当前统一快照。快照范围由服务端认证能力限制，不接收客户端当前时刻。
     * @param classroomId 可选教室主键
     * @param boardSn 可选班牌 SN
     * @return 状态、当前/未来事件及可信度
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> snapshot(Long classroomId, String boardSn) {
        ScheduleState state = stateMapper.selectById(1);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), CoursePeriodResolver.ZONE);
        List<Classroom> allRooms = roomMapper.selectList(null);
        List<CourseSchedule> courses = courseMapper.selectList(null);
        List<Exam> exams = examMapper.selectList(null);
        List<Classroom> selected = allRooms.stream().filter(r -> (classroomId == null || classroomId.equals(r.getId()))
                && (boardSn == null || boardSn.equals(r.getBoardSn()))).collect(Collectors.toList());
        if (boardSn != null && selected.size() != 1) throw new IllegalArgumentException("班牌未绑定唯一教室");
        List<Map<String, Object>> resultRooms = new ArrayList<>();
        for (Classroom room : selected) resultRooms.add(roomSnapshot(room, allRooms, courses, exams, state, now));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverNow", iso(now)); result.put("validUntil", iso(now.plusSeconds(properties.getSnapshotTtlSeconds())));
        result.put("timezone", CoursePeriodResolver.ZONE.getId());
        result.put("scheduleVersion", state == null ? null : state.getScheduleVersion());
        result.put("policyVersion", properties.policyKey()); result.put("lastSuccess", state == null || state.getLastSuccess() == null ? null : iso(state.getLastSuccess()));
        result.put("coverageStart", state == null ? null : state.getCoverageStart()); result.put("coverageEnd", state == null ? null : state.getCoverageEnd());
        result.put("classrooms", resultRooms);
        return result;
    }

    /**
     * 在一致读快照中查询指定教室日期范围内的有效课程及跨日考试。
     *
     * @param classroomId 内部教室主键
     * @param from 开始日期
     * @param to 结束日期（含当天）
     * @return 已知安排及无法确认的原因
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> schedule(Long classroomId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusDays(30))) {
            throw new IllegalArgumentException("排程预览范围须为一至三十一天");
        }
        ScheduleState state = stateMapper.selectById(1);
        List<Classroom> rooms = roomMapper.selectList(null);
        Classroom room = rooms.stream().filter(r -> classroomId != null && classroomId.equals(r.getId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("教室不存在"));
        List<Map<String, Object>> events = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (!coordinator.covered(state, from, to)) problems.add("目标日期课表覆盖缺失、过期或作息版本改变");
        for (CourseSchedule course : courseMapper.selectList(null)) {
            LocalDate day;
            try { day = resolver.date(course.getClassDate()); }
            catch (RuntimeException error) { if (coordinator.mayAffect(course, room)) problems.add("课程日期未知：" + course.getCourseKey()); continue; }
            if (day.isBefore(from) || day.isAfter(to)) continue;
            Long bound = coordinator.binding(course, rooms);
            if (bound == null && coordinator.mayAffect(course, room)) problems.add("课程绑定存在歧义：" + course.getCourseKey());
            if (!classroomId.equals(bound)) continue;
            if ("PENDING".equals(course.getParseStatus())) problems.add("独立调整待来源复核：" + course.getCourseKey());
            if (Integer.valueOf(0).equals(course.getEffective())) continue;
            try {
                for (ScheduleInterval interval : resolver.resolve(course)) {
                    Map<String, Object> item = event("course", course.getCourseKey(), course.getCourseName(), interval);
                    item.put("course", course); events.add(item);
                }
            } catch (RuntimeException error) { problems.add("课程时间未知：" + course.getCourseKey()); }
        }
        LocalDateTime start = from.atStartOfDay(), end = to.plusDays(1).atStartOfDay();
        for (Exam exam : examMapper.selectList(null)) {
            if (ExamLifecycle.terminal(exam.getStatus())) continue;
            Long bound = exam.getClassroomId();
            if (bound == null) {
                List<Classroom> matches = rooms.stream().filter(r -> exam.getBoardSn() != null && exam.getBoardSn().equals(r.getBoardSn())).collect(Collectors.toList());
                if (matches.size() == 1) bound = matches.get(0).getId();
                else if (Objects.equals(room.getBoardSn(), exam.getBoardSn())) problems.add("考试绑定未知：" + exam.getId());
            }
            if (!classroomId.equals(bound)) continue;
            LocalDateTime effectiveEnd = exam.getActualEndTime() == null ? exam.getEndTime() : exam.getActualEndTime();
            if (exam.getStartTime() == null || effectiveEnd == null || !effectiveEnd.isAfter(exam.getStartTime())) {
                problems.add("考试时间未知：" + exam.getId()); continue;
            }
            if (exam.getStartTime().isBefore(end) && effectiveEnd.isAfter(start)) {
                Map<String, Object> item = event("exam", String.valueOf(exam.getId()), exam.getExamContent(),
                        new ScheduleInterval(exam.getStartTime(), effectiveEnd));
                item.put("exam", exam); events.add(item);
            }
        }
        events.sort(Comparator.comparing(e -> OffsetDateTime.parse(String.valueOf(e.get("start")))));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classroomId", classroomId); result.put("events", events); result.put("problems", problems);
        result.put("scheduleVersion", state == null ? null : state.getScheduleVersion());
        result.put("policyVersion", properties.policyKey()); result.put("timezone", CoursePeriodResolver.ZONE.getId());
        return result;
    }

    /**
     * 生成单个教室当前状态及当天课程，未知绝不计为空闲。
     * @param room 教室
     * @param allRooms 全部身份目录
     * @param courses 全部课程
     * @param exams 全部考试
     * @param state 来源版本
     * @param now 学校时刻
     * @return 单室快照
     */
    private Map<String, Object> roomSnapshot(Classroom room, List<Classroom> allRooms, List<CourseSchedule> courses,
            List<Exam> exams, ScheduleState state, LocalDateTime now) {
        List<Map<String, Object>> active = new ArrayList<>(), upcoming = new ArrayList<>(), today = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (!coordinator.covered(state, now.toLocalDate(), now.toLocalDate())) problems.add("课表覆盖缺失、过期或作息版本改变");
        List<CourseSchedule> activeCourses = new ArrayList<>();
        for (CourseSchedule course : courses) {
            Long bound = coordinator.binding(course, allRooms);
            LocalDate date;
            try { date = resolver.date(course.getClassDate()); }
            catch (RuntimeException e) { if (coordinator.mayAffect(course, room)) problems.add("课程日期未知"); continue; }
            if (!date.equals(now.toLocalDate())) continue;
            if (bound == null && coordinator.mayAffect(course, room)) problems.add("课程教室关联存在歧义");
            if (!room.getId().equals(bound)) continue;
            if ("PENDING".equals(course.getParseStatus())) problems.add("本地调整的来源已改变，待人工复核");
            if (Integer.valueOf(0).equals(course.getEffective())) continue;
            try {
                for (ScheduleInterval interval : resolver.resolve(course)) {
                    Map<String, Object> event = event("course", course.getCourseKey(), course.getCourseName(), interval);
                    event.put("course", course); event.put("leaveSource", "UNVERIFIED_OA"); today.add(event);
                    if (CoursePeriodResolver.contains(interval, now)) {
                        boolean duplicate = activeCourses.stream().anyMatch(c -> coordinator.sameEvent(c, course));
                        if (!duplicate) { active.add(event); activeCourses.add(course); }
                    } else if (interval.getStart().isAfter(now)) upcoming.add(event);
                }
            } catch (RuntimeException e) { problems.add(e.getMessage()); }
        }
        for (Exam exam : exams) {
            if (ExamLifecycle.terminal(exam.getStatus())) continue;
            Long bound = exam.getClassroomId();
            if (bound == null) {
                List<Classroom> matches = allRooms.stream().filter(r -> exam.getBoardSn() != null && exam.getBoardSn().equals(r.getBoardSn())).collect(Collectors.toList());
                if (matches.size() == 1) bound = matches.get(0).getId();
                else if (Objects.equals(room.getBoardSn(), exam.getBoardSn())) problems.add("考试教室关联存在歧义");
            }
            if (!room.getId().equals(bound) || ExamLifecycle.terminal(exam.getStatus())) continue;
            if (exam.getStartTime() == null || exam.getEndTime() == null || !exam.getEndTime().isAfter(exam.getStartTime())) { problems.add("考试时间未知"); continue; }
            ScheduleInterval interval = new ScheduleInterval(exam.getStartTime(), exam.getActualEndTime() == null ? exam.getEndTime() : exam.getActualEndTime());
            Map<String, Object> event = event("exam", String.valueOf(exam.getId()), exam.getExamContent(), interval);
            event.put("exam", exam);
            if (CoursePeriodResolver.contains(interval, now)) active.add(event);
            else if (interval.getStart().isAfter(now)) upcoming.add(event);
        }
        upcoming.sort(Comparator.comparing(e -> OffsetDateTime.parse(String.valueOf(e.get("start")))));
        String status = active.size() > 1 ? "conflict" : !problems.isEmpty() ? "unknown" : active.isEmpty() ? "idle" : String.valueOf(active.get(0).get("type"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classroom", room); result.put("classroomId", room.getId()); result.put("occupancyStatus", status);
        result.put("activeEvents", active); result.put("upcomingEvents", upcoming); result.put("todayCourses", today);
        result.put("knownOccupied", !active.isEmpty());
        result.put("currentCourses", active.stream().filter(e -> "course".equals(e.get("type"))).collect(Collectors.toList()));
        result.put("currentExams", active.stream().filter(e -> "exam".equals(e.get("type"))).collect(Collectors.toList()));
        result.put("nextCourse", upcoming.stream().filter(e -> "course".equals(e.get("type"))).findFirst().orElse(null));
        result.put("nextExam", upcoming.stream().filter(e -> "exam".equals(e.get("type"))).findFirst().orElse(null));
        List<OffsetDateTime> boundaries = new ArrayList<>();
        active.forEach(e -> boundaries.add(OffsetDateTime.parse(String.valueOf(e.get("end")))));
        upcoming.forEach(e -> boundaries.add(OffsetDateTime.parse(String.valueOf(e.get("start")))));
        result.put("nextBoundaryAt", boundaries.stream().min(Comparator.naturalOrder()).map(OffsetDateTime::toString).orElse(null));
        result.put("conflicts", active.size() > 1 ? active : Collections.emptyList());
        result.put("problems", problems); result.put("lockStatus", "unknown"); result.put("automaticLockEnabled", false);
        List<Map<String, Object>> preview = new ArrayList<>();
        List<Map<String, Object>> arranged = new ArrayList<>(today);
        arranged.addAll(upcoming.stream().filter(e -> "exam".equals(e.get("type"))).collect(Collectors.toList()));
        arranged.addAll(active.stream().filter(e -> "exam".equals(e.get("type"))).collect(Collectors.toList()));
        arranged.sort(Comparator.comparing(e -> OffsetDateTime.parse(String.valueOf(e.get("start")))));
        for (Map<String, Object> event : arranged) {
            if (!preview.isEmpty() && !OffsetDateTime.parse(String.valueOf(event.get("start")))
                    .isAfter(OffsetDateTime.parse(String.valueOf(preview.get(preview.size() - 1).get("closeAt"))))) {
                Map<String, Object> last = preview.get(preview.size() - 1);
                if (OffsetDateTime.parse(String.valueOf(event.get("end"))).isAfter(OffsetDateTime.parse(String.valueOf(last.get("closeAt"))))) last.put("closeAt", event.get("end"));
            } else {
                Map<String, Object> window = new LinkedHashMap<>();
                window.put("openAt", event.get("start")); window.put("closeAt", event.get("end"));
                window.put("executable", false); window.put("reason", "联动尚未启用；需确认策略、绑定、回执及关门安全条件");
                preview.add(window);
            }
        }
        result.put("lockPreview", preview);
        result.put("capacity", room.getCapacity()); result.put("actualAttendance", null);
        return result;
    }

    /**
     * 创建带显式时区的事件。
     * @param type 类型
     * @param id 稳定身份
     * @param name 名称
     * @param interval 区间
     * @return 事件
     */
    private Map<String, Object> event(String type, String id, String name, ScheduleInterval interval) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type); event.put("id", id); event.put("name", name);
        event.put("start", iso(interval.getStart())); event.put("end", iso(interval.getEnd()));
        return event;
    }

    /**
     * 格式化学校时间。
     * @param value 学校当地时间
     * @return 带偏移的 ISO 时间
     */
    private String iso(LocalDateTime value) { return value.atZone(CoursePeriodResolver.ZONE).toOffsetDateTime().toString(); }
}
