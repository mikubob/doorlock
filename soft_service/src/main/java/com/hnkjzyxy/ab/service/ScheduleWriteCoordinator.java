package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.config.SchoolScheduleProperties;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.utils.TransactionalMysqlLock;
import com.hnkjzyxy.ab.vo.ScheduleInterval;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课表和考试共用的排程事务协议。所有写入必须先取得同一锁，再读取最终排程。
 * 同步保留原租约保护，业务冲突照实记录，不隐式取消课程。
 */
@Service
public class ScheduleWriteCoordinator {
    /**
     * 共同事务锁，沿用已有同步锁名称。
     */
    public static final String LOCK_NAME = "assessment:course_schedule:sync";
    /**
     * 数据库会话锁。
     */
    private final TransactionalMysqlLock databaseLock;
    /**
     * 排程状态访问。
     */
    private final ScheduleStateMapper stateMapper;
    /**
     * 审计访问。
     */
    private final ScheduleAuditMapper auditMapper;
    /**
     * 教室访问。
     */
    private final ClassroomMapper classroomMapper;
    /**
     * 课程访问。
     */
    private final CourseScheduleMapper courseMapper;
    /**
     * 考试访问。
     */
    private final ExamMapper examMapper;
    /**
     * 统一作息解析。
     */
    private final CoursePeriodResolver resolver;
    /**
     * 可信度及别名配置。
     */
    private final SchoolScheduleProperties properties;

    /**
     * 创建共同写协调器。
     * @param databaseLock 数据库事务锁
     * @param stateMapper 排程状态
     * @param auditMapper 排程审计
     * @param classroomMapper 教室
     * @param courseMapper 课表
     * @param examMapper 考试
     * @param resolver 时间解析
     * @param properties 学校配置
     */
    public ScheduleWriteCoordinator(TransactionalMysqlLock databaseLock, ScheduleStateMapper stateMapper,
            ScheduleAuditMapper auditMapper, ClassroomMapper classroomMapper, CourseScheduleMapper courseMapper,
            ExamMapper examMapper, CoursePeriodResolver resolver, SchoolScheduleProperties properties) {
        this.databaseLock = databaseLock; this.stateMapper = stateMapper; this.auditMapper = auditMapper;
        this.classroomMapper = classroomMapper; this.courseMapper = courseMapper; this.examMapper = examMapper;
        this.resolver = resolver; this.properties = properties;
    }

    /**
     * 在事务首个业务读取前获取共同锁。
     * @return 最新状态
     */
    public ScheduleState lock() {
        databaseLock.acquire(LOCK_NAME);
        ScheduleState state = stateMapper.lockState();
        if (state == null) throw new IllegalStateException("请先执行课表联动数据库迁移");
        return state;
    }

    /**
     * 原子增加版本及记录审计。
     * @param action 操作
     * @param detail 变更明细
     */
    public void changed(String action, String detail) {
        ScheduleState state = stateMapper.selectById(1);
        state.setScheduleVersion(state.getScheduleVersion() + 1);
        if (stateMapper.updateById(state) != 1) throw new IllegalStateException("更新排程版本失败");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ScheduleAudit audit = new ScheduleAudit();
        audit.setScheduleVersion(state.getScheduleVersion()); audit.setAction(action);
        audit.setActor(auth == null ? "system" : auth.getName()); audit.setDetail(detail);
        audit.setCreatedTime(LocalDateTime.now(CoursePeriodResolver.ZONE));
        if (auditMapper.insert(audit) != 1) throw new IllegalStateException("保存排程审计失败");
    }

    /**
     * 判断覆盖可信度，不以无记录证明无课。
     * @param state 发布状态
     * @param start 开始日期
     * @param end 结束日期
     * @return 是否可确认排程
     */
    public boolean covered(ScheduleState state, LocalDate start, LocalDate end) {
        return properties.isCoverageConfirmed() && state != null && state.getLastSuccess() != null && state.getCoverageStart() != null
                && state.getCoverageEnd() != null && properties.policyKey().equals(state.getPolicyVersion())
                && !start.isBefore(state.getCoverageStart()) && !end.isAfter(state.getCoverageEnd())
                && state.getLastSuccess().plusHours(properties.getCoverageMaxAgeHours())
                        .isAfter(LocalDateTime.now(CoursePeriodResolver.ZONE));
    }

    /**
     * 解析唯一教室，优先内部主键，旧 SN 仅作兼容。
     * @param id 内部教室主键
     * @param sn 班牌 SN
     * @return 唯一教室
     */
    public Classroom room(Long id, String sn) {
        List<Classroom> rooms = classroomMapper.selectList(null);
        List<Classroom> matches = rooms.stream().filter(r -> id != null ? id.equals(r.getId())
                : sn != null && sn.equals(r.getBoardSn())).collect(Collectors.toList());
        if (matches.size() != 1) throw new IllegalArgumentException("教室不存在或班牌关联不唯一，请处理绑定");
        return matches.get(0);
    }

    /**
     * 用完整位置唯一绑定课表，不匹配第一条同号教室。
     * @param course 课程
     * @param rooms 教室目录
     * @return 唯一主键，歧义返回 null
     */
    public Long binding(CourseSchedule course, List<Classroom> rooms) {
        if (course.getClassroomId() != null && rooms.stream().anyMatch(r -> r.getId().equals(course.getClassroomId()))) return course.getClassroomId();
        String campus = properties.getCampusAliases().getOrDefault(course.getCampus(), course.getCampus());
        List<Classroom> found = rooms.stream().filter(r -> equal(course.getClassroomNumber(), r.getClassroomNumber())
                && equal(campus, r.getCampusName()) && equal(course.getBuildingName(), r.getBuildingName())).collect(Collectors.toList());
        return found.size() == 1 ? found.get(0).getId() : null;
    }

    /**
     * 判断未绑定课程是否可能影响该教室；缺少教室号时影响全部教室。
     * @param course 课程
     * @param room 目标教室
     * @return 是否需要阻止可用判断
     */
    public boolean mayAffect(CourseSchedule course, Classroom room) {
        return course.getClassroomNumber() == null || course.getClassroomNumber().trim().isEmpty()
                || equal(course.getClassroomNumber(), room.getClassroomNumber());
    }

    /**
     * 验证考试课程冲突及课表可信度。
     * @param exam 最终考试
     */
    public void validateCourseConflict(Exam exam) {
        if (Integer.valueOf(2).equals(exam.getStatus())) return;
        if (!covered(stateMapper.selectById(1), exam.getStartTime().toLocalDate(), exam.getEndTime().toLocalDate())) {
            throw new IllegalArgumentException("课表状态无法确认，请刷新或处理后重试（目标日期未在可信覆盖范围内）");
        }
        Classroom target = room(exam.getClassroomId(), exam.getBoardSn());
        List<Classroom> rooms = classroomMapper.selectList(null);
        for (CourseSchedule course : courseMapper.selectList(null)) {
            LocalDate day;
            try { day = resolver.date(course.getClassDate()); }
            catch (RuntimeException e) {
                if (mayAffect(course, target)) throw new IllegalArgumentException("课表日期无法确认，请处理后重试");
                continue;
            }
            if (day.isBefore(exam.getStartTime().toLocalDate()) || day.isAfter(exam.getEndTime().toLocalDate())) continue;
            Long roomId = binding(course, rooms);
            if (roomId == null && mayAffect(course, target)) throw new IllegalArgumentException("课表教室绑定无法确认，请处理后重试");
            if (!target.getId().equals(roomId)) continue;
            if ("PENDING".equals(course.getParseStatus())) throw new IllegalArgumentException("本地课程调整来源改变，须先独立复核");
            if (Integer.valueOf(0).equals(course.getEffective())) continue;
            List<ScheduleInterval> intervals;
            try { intervals = resolver.resolve(course); }
            catch (RuntimeException e) { throw new IllegalArgumentException("课表状态无法确认：" + e.getMessage()); }
            for (ScheduleInterval interval : intervals) {
                if (CoursePeriodResolver.overlaps(interval, new ScheduleInterval(exam.getStartTime(), exam.getEndTime()))) {
                    throw new IllegalArgumentException("该教室此时段有课程，不能安排考试，请更换时间或教室："
                            + course.getCourseName() + "（" + interval.getStart() + " 至 " + interval.getEnd() + "）");
                }
            }
        }
    }

    /**
     * 验证手工课程的最终目标与全部有效课程、考试。
     * @param course 最终课程
     */
    public void validateCourseWrite(CourseSchedule course) {
        List<Classroom> rooms = classroomMapper.selectList(null);
        Long id = binding(course, rooms);
        if (id == null) throw new IllegalArgumentException("课表教室绑定无法确认");
        course.setClassroomId(id);
        List<ScheduleInterval> intervals = resolver.resolve(course);
        if (Integer.valueOf(0).equals(course.getEffective())) return;
        for (Exam exam : examMapper.selectList(null)) {
            Long examRoom = exam.getClassroomId();
            if (examRoom == null) examRoom = room(null, exam.getBoardSn()).getId();
            if (!id.equals(examRoom) || Integer.valueOf(2).equals(exam.getStatus())) continue;
            if (exam.getStartTime() == null || exam.getEndTime() == null) throw new IllegalArgumentException("已有考试时间无法确认");
            for (ScheduleInterval interval : intervals) if (CoursePeriodResolver.overlaps(interval,
                    new ScheduleInterval(exam.getStartTime(), exam.getActualEndTime() == null ? exam.getEndTime() : exam.getActualEndTime()))) {
                throw new IllegalArgumentException("课程目标时段已有考试：" + exam.getExamCode());
            }
        }
        for (CourseSchedule other : courseMapper.selectList(null)) {
            if (Objects.equals(course.getId(), other.getId()) || Integer.valueOf(0).equals(other.getEffective())) continue;
            if (!id.equals(binding(other, rooms))) continue;
            if (sameEvent(course, other)) continue;
            if (!resolver.date(course.getClassDate()).equals(resolver.date(other.getClassDate()))) continue;
            for (ScheduleInterval a : intervals) for (ScheduleInterval b : resolver.resolve(other)) {
                if (CoursePeriodResolver.overlaps(a, b)) throw new IllegalArgumentException("课程目标时段已有课程：" + other.getCourseName());
            }
        }
    }

    /**
     * 合班课程的事件判定；仅教师及课程、日期、节次完全一致时合并。
     * @param a 第一条
     * @param b 第二条
     * @return 是否同一教学事件
     */
    public boolean sameEvent(CourseSchedule a, CourseSchedule b) {
        return equal(a.getTeacherId(), b.getTeacherId()) && equal(a.getCourseName(), b.getCourseName())
                && equal(a.getClassDate(), b.getClassDate()) && equal(a.getClassPeriod(), b.getClassPeriod());
    }

    /**
     * 发布完整 OA 覆盖；仅有效日期产生范围，无效日期留在原数据等待处理。
     * @param courses 本次 OA 来源
     */
    public void published(List<CourseSchedule> courses) {
        List<LocalDate> dates = new ArrayList<>();
        for (CourseSchedule course : courses) {
            try { dates.add(resolver.date(course.getClassDate())); } catch (RuntimeException ignored) { }
        }
        if (dates.isEmpty()) throw new IllegalArgumentException("同步课表没有可确认的覆盖日期");
        ScheduleState state = stateMapper.selectById(1);
        state.setLastSuccess(LocalDateTime.now(CoursePeriodResolver.ZONE));
        if (properties.isCoverageConfirmed()) {
            if (properties.getCoverageStart() == null || properties.getCoverageEnd() == null) throw new IllegalArgumentException("请配置已核实的完整课表覆盖范围");
            LocalDate from = LocalDate.parse(properties.getCoverageStart()), to = LocalDate.parse(properties.getCoverageEnd());
            if (to.isBefore(from) || Collections.min(dates).isBefore(from) || Collections.max(dates).isAfter(to)) throw new IllegalArgumentException("课表超出已核实的完整来源范围");
            state.setCoverageStart(from); state.setCoverageEnd(to);
        }
        state.setPolicyVersion(properties.policyKey());
        if (stateMapper.updateById(state) != 1) throw new IllegalStateException("发布课表覆盖失败");
    }

    /**
     * 按同一事务的最终数据重建当前问题清单，保留历次处理历史。
     * 课程按教室分组后扫过排序区间，避免每条同步课程重新全表查询。
     */
    public void reconcileConflicts() {
        auditMapper.resolveOpen();
        List<Classroom> rooms = classroomMapper.selectList(null);
        Map<Long, List<ConflictEvent>> groups = new HashMap<>();
        for (CourseSchedule course : courseMapper.selectList(null)) {
            if ("PENDING".equals(course.getParseStatus())) issue("COURSE_SOURCE_REVIEW", course.toString());
            if (Integer.valueOf(0).equals(course.getEffective())) continue;
            Long roomId = binding(course, rooms);
            if (roomId == null) { issue("COURSE_BINDING_UNKNOWN", course.toString()); continue; }
            try {
                for (ScheduleInterval interval : resolver.resolve(course)) groups.computeIfAbsent(roomId, k -> new ArrayList<>())
                        .add(new ConflictEvent("course", course.getCourseKey(), course.getCourseName(), interval, course));
            } catch (RuntimeException error) { issue("COURSE_TIME_UNKNOWN", "courseKey=" + course.getCourseKey() + "; " + error.getMessage()); }
        }
        for (Exam exam : examMapper.selectList(null)) {
            if (Integer.valueOf(2).equals(exam.getStatus())) continue;
            try {
                Long roomId = room(exam.getClassroomId(), exam.getBoardSn()).getId();
                if (exam.getStartTime() == null || exam.getEndTime() == null) throw new IllegalArgumentException("考试时间未知");
                groups.computeIfAbsent(roomId, k -> new ArrayList<>()).add(new ConflictEvent("exam", String.valueOf(exam.getId()),
                        exam.getExamCode(), new ScheduleInterval(exam.getStartTime(), exam.getEndTime()), null));
            } catch (IllegalArgumentException error) { issue("EXAM_BINDING_UNKNOWN", "examId=" + exam.getId() + "; " + error.getMessage()); }
        }
        for (Map.Entry<Long, List<ConflictEvent>> entry : groups.entrySet()) {
            List<ConflictEvent> events = entry.getValue();
            events.sort(Comparator.comparing(e -> e.interval.getStart()));
            List<ConflictEvent> active = new ArrayList<>();
            for (ConflictEvent event : events) {
                active.removeIf(e -> e.interval.getEnd().isBefore(event.interval.getStart()));
                for (ConflictEvent previous : active) {
                    if (event.course != null && previous.course != null && sameEvent(event.course, previous.course)) continue;
                    if (CoursePeriodResolver.overlaps(previous.interval, event.interval)) issue("SCHEDULE_CONFLICT",
                            "classroomId=" + entry.getKey() + "; first=" + previous + "; second=" + event);
                }
                active.add(event);
            }
        }
    }

    /**
     * 保存当前版本问题，不重复增加业务版本。
     * @param action 问题类型
     * @param detail 双方身份、区间或待处理原因
     */
    private void issue(String action, String detail) {
        ScheduleAudit audit = new ScheduleAudit();
        audit.setScheduleVersion(stateMapper.selectById(1).getScheduleVersion()); audit.setAction(action);
        audit.setActor("system"); audit.setDetail(detail); audit.setIssueStatus("OPEN");
        audit.setCreatedTime(LocalDateTime.now(CoursePeriodResolver.ZONE));
        if (auditMapper.insert(audit) != 1) throw new IllegalStateException("登记排程问题失败，已回滚");
    }

    /**
     * 用于全量排程冲突扫描的事件，不替代来源持久化。
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ConflictEvent {
        /**
         * 来源类型。
         */
        private String type;
        /**
         * 来源身份。
         */
        private String id;
        /**
         * 安排名称。
         */
        private String name;
        /**
         * 时间区间。
         */
        private ScheduleInterval interval;
        /**
         * 课程事件判定依据。
         */
        @lombok.ToString.Exclude
        private CourseSchedule course;
    }

    /**
     * 比较非空规范字段。
     * @param a 字段一
     * @param b 字段二
     * @return 是否相等
     */
    private boolean equal(String a, String b) { return a != null && b != null && !a.trim().isEmpty() && a.trim().equals(b.trim()); }
}
