package com.hnkjzyxy.ab;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.client.OaApiClient;
import com.hnkjzyxy.ab.config.*;
import com.hnkjzyxy.ab.controller.ExamController;
import com.hnkjzyxy.ab.controller.ExamRecordController;
import com.hnkjzyxy.ab.controller.MenuController;
import com.hnkjzyxy.ab.dto.CourseSourceRebindDto;
import com.hnkjzyxy.ab.dto.LockCommandReviewDto;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.*;
import com.hnkjzyxy.ab.service.*;
import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import com.hnkjzyxy.ab.service.impl.CheckResultServiceImpl;
import com.hnkjzyxy.ab.service.impl.CourseScheduleServiceImpl;
import com.hnkjzyxy.ab.service.impl.ExamServiceImpl;
import com.hnkjzyxy.ab.service.support.LockCommandReceiptService;
import com.hnkjzyxy.ab.service.support.LockCommandService;
import com.hnkjzyxy.ab.utils.RedisLockUtils;
import com.hnkjzyxy.ab.utils.TransactionalMysqlLock;
import com.hnkjzyxy.ab.vo.ExamRecordQuery;
import com.hnkjzyxy.ab.vo.ScheduleInterval;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.InputStream;
import java.time.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 统一时间、课程冲突、最终批次、快照及回执的确定性回归，不连接业务数据库或设备。
 */
public class ScheduleConsistencyTest {
    /**
     * 学校规则。
     */
    private SchoolScheduleProperties properties;
    /**
     * 时间解析器。
     */
    private CoursePeriodResolver resolver;
    /**
     * 模拟目录及业务数据访问。
     */
    private ClassroomMapper rooms;
    /**
     * 模拟课程访问。
     */
    private CourseScheduleMapper courses;
    /**
     * 模拟考试访问。
     */
    private ExamMapper exams;
    /**
     * 模拟排程状态访问。
     */
    private ScheduleStateMapper states;
    /**
     * 模拟审计访问。
     */
    private ScheduleAuditMapper audits;
    /**
     * 共同规则。
     */
    private ScheduleWriteCoordinator coordinator;
    /**
     * 考试写入服务。
     */
    private ExamServiceImpl service;

    /**
     * 初始化可信完整来源及两个无班牌也合法的教室。
     */
    @BeforeEach
    void setup() {
        properties = new SchoolScheduleProperties(); properties.setCoverageConfirmed(true);
        resolver = new CoursePeriodResolver(properties);
        rooms = mock(ClassroomMapper.class); courses = mock(CourseScheduleMapper.class); exams = mock(ExamMapper.class);
        states = mock(ScheduleStateMapper.class); audits = mock(ScheduleAuditMapper.class);
        Classroom room = new Classroom(); room.setId(1L); room.setCampusName("北院"); room.setBuildingName("A"); room.setClassroomNumber("101"); room.setBoardSn("00Ab501");
        Classroom other = new Classroom(); other.setId(2L); other.setCampusName("南院"); other.setBuildingName("B"); other.setClassroomNumber("101");
        when(rooms.selectList(any())).thenReturn(Arrays.asList(room, other));
        when(courses.selectList(any())).thenReturn(new ArrayList<>()); when(exams.selectList(any())).thenReturn(new ArrayList<>());
        ScheduleState state = new ScheduleState(); state.setId(1); state.setScheduleVersion(1L); state.setPolicyVersion(properties.policyKey());
        state.setCoverageStart(LocalDate.of(2026, 1, 1)); state.setCoverageEnd(LocalDate.of(2027, 1, 1)); state.setLastSuccess(LocalDateTime.now(CoursePeriodResolver.ZONE));
        when(states.selectById(any())).thenReturn(state); when(states.lockState()).thenReturn(state);
        when(states.updateById(any())).thenReturn(1); when(audits.insert(any())).thenReturn(1);
        when(exams.insert(any())).thenReturn(1); when(exams.updateById(any())).thenReturn(1);
        coordinator = new ScheduleWriteCoordinator(mock(TransactionalMysqlLock.class), states, audits, rooms, courses, exams, resolver, properties);
        service = new ExamServiceImpl(); ReflectionTestUtils.setField(service, "baseMapper", exams); ReflectionTestUtils.setField(service, "coordinator", coordinator);
        setExamTime("2026-10-05T00:00:00");
    }

    /**
     * 构造课程事件。
     * @param period 节次
     * @return 课程
     */
    private CourseSchedule course(String period) {
        CourseSchedule course = new CourseSchedule(); course.setId(9); course.setClassroomId(1L); course.setCourseKey("stable-course");
        course.setCourseName("数学"); course.setTeacherId("T1"); course.setClassDate("20261005"); course.setClassPeriod(period); course.setEffective(1);
        course.setCampus("北院"); course.setBuildingName("A"); course.setClassroomNumber("101"); return course;
    }
    /**
     * 构造考试。
     * @param start 开始学校当地时间
     * @param end 结束学校当地时间
     * @return 考试
     */
    private Exam exam(String start, String end) {
        Exam exam = new Exam(); exam.setClassroomId(1L); exam.setExamCode("E1"); exam.setExamContent("数学考试"); exam.setStatus(0); exam.setRowVersion(0L);
        exam.setStartTime(LocalDateTime.parse(start)); exam.setEndTime(LocalDateTime.parse(end)); return exam;
    }

    /**
     * 连续课间保留与显示半开边界。
     */
    @Test
    void continuousBreakAndHalfOpenEnd() {
        ScheduleInterval interval = resolver.resolve(course("1-4")).get(0);
        assertTrue(CoursePeriodResolver.contains(interval, LocalDateTime.parse("2026-10-05T10:00:00")));
        assertFalse(CoursePeriodResolver.contains(interval, interval.getEnd()));
        assertTrue(CoursePeriodResolver.overlaps(interval, new ScheduleInterval(interval.getEnd(), interval.getEnd().plusHours(1))));
    }
    /**
     * 不连续课程不把中间空档占用。
     */
    @Test
    void nonContinuousSegments() { assertEquals(2, resolver.resolve(course("1-2,5-6")).size()); }
    /**
     * 未知格式、单节和日期不猜测。
     */
    @Test
    void unknownPeriodsAndInvalidDate() {
        for (String period : Arrays.asList("1", "2-3", "第1节", "4-1", "31", "")) assertThrows(RuntimeException.class, () -> resolver.resolve(course(period)));
        assertThrows(RuntimeException.class, () -> resolver.date("20260230"));
        assertEquals(LocalDate.of(2026, 10, 5), resolver.date("2026/10/5"));
    }
    /**
     * 官方配置后单节及跨双节课程按相同规则解析。
     */
    @Test
    void explicitOfficialPeriods() {
        properties.getStarts().put(2, "09:10"); properties.getEnds().put(3, "11:00");
        assertEquals(LocalTime.of(9, 10), resolver.resolve(course("2-3")).get(0).getStart().toLocalTime());
        assertEquals(1, resolver.resolve(course("2")).size());
    }
    /**
     * 完全包含、被包含、部分相交、端点和跨日都拒绝。
     */
    @Test
    void rejectsEveryCourseOverlap() {
        when(courses.selectList(any())).thenReturn(Collections.singletonList(course("1-4")));
        String[][] intervals = {{"2026-10-05T07:00:00","2026-10-05T13:00:00"}, {"2026-10-05T09:00:00","2026-10-05T10:00:00"},
                {"2026-10-05T11:40:00","2026-10-05T12:30:00"}, {"2026-10-05T11:50:00","2026-10-05T12:30:00"},
                {"2026-10-04T23:00:00","2026-10-05T09:00:00"}};
        for (String[] value : intervals) assertTrue(service.batchAdd(Collections.singletonList(exam(value[0], value[1]))).contains("有课程"));
        verify(exams, never()).insert(any());
    }
    /**
     * 未确认来源、歧义绑定、未知节次均不能放行。
     */
    @Test
    void unknownCannotPassScheduling() {
        Exam row = exam("2026-10-05T09:00:00","2026-10-05T10:00:00");
        properties.setCoverageConfirmed(false); assertTrue(service.batchAdd(Collections.singletonList(row)).contains("无法确认"));
        properties.setCoverageConfirmed(true); when(courses.selectList(any())).thenReturn(Collections.singletonList(course("2-3")));
        assertTrue(service.batchAdd(Collections.singletonList(row)).contains("无法确认")); verify(exams, never()).insert(any());
    }
    /**
     * 批量修改排除本批旧安排，合法互换时间成功。
     */
    @Test
    void finalBatchAllowsSwap() {
        Exam a = exam("2026-10-05T12:00:00","2026-10-05T13:00:00"); a.setId(1L);
        Exam b = exam("2026-10-05T14:00:00","2026-10-05T15:00:00"); b.setId(2L);
        when(exams.selectList(any())).thenReturn(Arrays.asList(a,b));
        Exam pa = new Exam(); pa.setId(1L); pa.setRowVersion(0L); pa.setStartTime(b.getStartTime()); pa.setEndTime(b.getEndTime());
        Exam pb = new Exam(); pb.setId(2L); pb.setRowVersion(0L); pb.setStartTime(a.getStartTime()); pb.setEndTime(a.getEndTime());
        assertNull(service.batchUpdate(Arrays.asList(pa,pb))); verify(exams,times(2)).updateById(any());
        assertEquals(LocalDateTime.parse("2026-10-05T12:00:00"), a.getStartTime());
    }
    /**
     * 重复 ID 和旧行版本在任何持久化之前拒绝。
     */
    @Test
    void duplicateAndStaleIdsRejected() {
        Exam row = exam("2026-10-05T12:00:00","2026-10-05T13:00:00"); row.setId(1L);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(row));
        Exam patch = new Exam(); patch.setId(1L); patch.setRowVersion(9L);
        assertTrue(service.batchUpdate(Collections.singletonList(patch)).contains("已被修改"));
        patch.setRowVersion(0L); assertTrue(service.batchUpdate(Arrays.asList(patch,patch)).contains("重复"));
        verify(exams,never()).updateById(any());
    }
    /**
     * 重新启用和延长也验证课程。
     */
    @Test
    void reenableMustCheckCourse() {
        Exam old = exam("2026-10-05T09:00:00","2026-10-05T10:00:00"); old.setId(1L); old.setStatus(2);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(old)); when(courses.selectList(any())).thenReturn(Collections.singletonList(course("1-4")));
        Exam patch = new Exam(); patch.setId(1L); patch.setRowVersion(0L); patch.setStatus(0);
        assertTrue(service.batchUpdate(Collections.singletonList(patch)).contains("有课程")); verify(exams,never()).updateById(any());
    }
    /**
     * 无班牌教室可排考，预览没有写操作。
     */
    @Test
    void noBoardRoomAndPreviewAreSupported() {
        Exam row = exam("2026-10-05T12:00:00","2026-10-05T13:00:00"); row.setClassroomId(2L);
        assertNull(service.preview(Collections.singletonList(row), false)); verify(exams,never()).insert(any());
        assertNull(service.batchAdd(Collections.singletonList(row))); verify(exams).insert(argThat(e -> e.getClassroomId().equals(2L) && e.getBoardSn() == null));
    }
    /**
     * 等时长、空元素、超量、空白内容统一拒绝。
     */
    @Test
    void invalidInputsAreRejected() {
        assertNotNull(service.batchAdd(Collections.singletonList(null)));
        assertNotNull(service.batchAdd(Collections.emptyList()));
        Exam row=exam("2026-10-05T12:00:00","2026-10-05T12:00:00"); assertNotNull(service.batchAdd(Collections.singletonList(row)));
        row.setEndTime(row.getStartTime().plusHours(1)); row.setExamContent("  "); assertNotNull(service.batchAdd(Collections.singletonList(row)));
        assertNotNull(service.batchAdd(Collections.nCopies(201,row))); verify(exams,never()).insert(any());
    }
    /**
     * 任一数据库写入失败抛出异常，交由事务代理整批回滚。
     */
    @Test
    void writeFailureThrowsForRollback() {
        when(exams.insert(any())).thenReturn(1,0);
        assertThrows(IllegalStateException.class, () -> service.batchAdd(Arrays.asList(exam("2026-10-05T12:00:00","2026-10-05T13:00:00"),exam("2026-10-05T14:00:00","2026-10-05T15:00:00"))));
        verify(audits,never()).insert(any());
    }
    /**
     * 合班识别和跨校区教室身份。
     */
    @Test
    void coTeachingAndRoomBinding() {
        CourseSchedule a=course("1-2"), b=course("1-2"); b.setClassName("第二班"); assertTrue(coordinator.sameEvent(a,b));
        a.setClassroomId(null); assertEquals(1L,coordinator.binding(a,rooms.selectList(null)));
        a.setCampus("未知编码"); assertNull(coordinator.binding(a,rooms.selectList(null)));
    }
    /**
     * 重复回执只按真实 taskId 扣减一次，失败回执不扣减。
     */
    @Test
    void receiptIsIdempotent() {
        LockCommandMapper mapper=mock(LockCommandMapper.class); when(mapper.complete(any())).thenReturn(1,0,1);
        LockCommandReceiptService receipts=new LockCommandReceiptService(mapper);
        LockCommand command=new LockCommand(); command.setId("c1"); command.setTaskId(77);
        receipts.complete(command,null); receipts.complete(command,null); receipts.complete(command,new IllegalStateException("timeout"));
        verify(mapper,times(1)).consume(77); assertEquals("failed",command.getStatus());
    }
    /**
     * Quartz 同锁多任务不覆盖，星期使用同一 JSON 数组和上海时区。
     */
    @Test
    void quartzIdentityAndCalendar() {
        QuartzConfig config=new QuartzConfig();
        assertNotEquals(config.createJobDetail(1,1,10,"1").getKey(),config.createJobDetail(1,1,11,"1").getKey());
        assertEquals("0 30 8 ? * 4,6",config.generateWeeklyCronExpression(8,30,new int[]{2,4}));
        assertEquals("0 30 8 ? * 1",config.generateWeeklyCronExpression(8,30,new int[]{6}));
        assertThrows(IllegalArgumentException.class,()->config.generateWeeklyCronExpression(24,30,new int[]{1}));
        CronTrigger trigger=(CronTrigger)config.createCronTrigger(config.createJobDetail(1,1,10,"1"),"0 30 8 ? * 1");
        assertEquals("Asia/Shanghai",trigger.getTimeZone().getID());
    }
    /**
     * A01 当前考试由区间派生，状态零也必须显示考试。
     */
    @Test
    void snapshotUsesCurrentExamDespiteStoredZero() {
        Exam row=exam("2026-10-05T01:04:00","2026-10-05T04:05:00"); row.setId(1L);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(row));
        ClassroomOccupancyService occupancy=new ClassroomOccupancyService(rooms,courses,exams,states,coordinator,resolver,properties,
                Clock.fixed(Instant.parse("2026-10-04T17:05:30Z"),ZoneOffset.UTC));
        List<?> snapshots=(List<?>)occupancy.snapshot(null,null).get("classrooms");
        assertEquals("exam",((Map<?,?>)snapshots.get(0)).get("occupancyStatus"));
        assertEquals("2026-10-05T01:05:30+08:00",occupancy.snapshot(null,null).get("serverNow"));
    }
    /**
     * A02 未来只预告，A13 旧状态二永不复活。
     */
    @Test
    void snapshotSeparatesFutureAndLegacyEnded() {
        Exam future=exam("2026-10-10T01:04:00","2026-10-10T04:05:00"); future.setId(1L);
        Exam ended=exam("2026-10-05T01:04:00","2026-10-05T04:05:00"); ended.setId(2L); ended.setStatus(2);
        when(exams.selectList(any())).thenReturn(Arrays.asList(future,ended));
        ClassroomOccupancyService occupancy=new ClassroomOccupancyService(rooms,courses,exams,states,coordinator,resolver,properties,
                Clock.fixed(Instant.parse("2026-10-04T17:05:30Z"),ZoneOffset.UTC));
        Map<?,?> room=(Map<?,?>)((List<?>)occupancy.snapshot(1L,null).get("classrooms")).get(0);
        assertEquals("idle",room.get("occupancyStatus")); assertEquals(1,((List<?>)room.get("upcomingEvents")).size());
        properties.setCoverageConfirmed(false);
        assertEquals("unknown",((Map<?,?>)((List<?>)occupancy.snapshot(1L,null).get("classrooms")).get(0)).get("occupancyStatus"));
    }
    /**
     * 结束瞬间不延长一分钟，未知作息保留待处理。
     */
    @Test
    void snapshotEndsAtExactSecondAndUnknownPeriod() {
        Exam row=exam("2026-10-05T01:04:00","2026-10-05T04:05:00"); row.setId(1L);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(row));
        ClassroomOccupancyService occupancy=new ClassroomOccupancyService(rooms,courses,exams,states,coordinator,resolver,properties,
                Clock.fixed(Instant.parse("2026-10-04T20:05:00Z"),ZoneOffset.UTC));
        assertEquals("idle",((Map<?,?>)((List<?>)occupancy.snapshot(1L,null).get("classrooms")).get(0)).get("occupancyStatus"));
        when(courses.selectList(any())).thenReturn(Collections.singletonList(course("2-3")));
        assertEquals("unknown",((Map<?,?>)((List<?>)occupancy.snapshot(1L,null).get("classrooms")).get(0)).get("occupancyStatus"));
    }
    /**
     * XML 均可被 MyBatis 实际解析，而非只检查文本。
     */
    @Test
    void mapperXmlContractsParse() throws Exception {
        MybatisConfiguration config=new MybatisConfiguration();
        for (String name : Arrays.asList("ExamMapper","ClassroomMapper","CourseScheduleMapper","ScheduleMapper","SmartLockMapper","ScheduleStateMapper","ScheduleAuditMapper","LockCommandMapper")) {
            String resource="mapper/"+name+".xml";
            try (InputStream stream=getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream,resource);
                new XMLMapperBuilder(stream,config,resource,config.getSqlFragments()).parse();
            }
        }
        assertTrue(config.hasStatement("com.hnkjzyxy.ab.mapper.LockCommandMapper.consume"));
    }

    /**
     * 来源复核保留稳定课程键和停课决定，只吸收被明确确认的来源版本。
     */
    @Test
    void sourceRebindKeepsApprovedCancellation() {
        CourseSchedule old = course("1-2"); old.setLocalAdjusted(1); old.setParseStatus("PENDING");
        old.setRowVersion(3L); old.setEffective(0); old.setSourceType("OA"); old.setSourceFingerprint("old-source");
        CourseSchedule source = course("3-4"); source.setId(10); source.setCourseKey("new-source-key");
        source.setLocalAdjusted(0); source.setRowVersion(5L); source.setSourceType("OA"); source.setSourceFingerprint("new-source");
        when(courses.selectList(any())).thenReturn(Arrays.asList(old, source));
        when(courses.deleteById(10)).thenReturn(1); when(courses.updateById(any())).thenReturn(1);
        ScheduleWriteCoordinator protocol = mock(ScheduleWriteCoordinator.class);
        CourseScheduleServiceImpl courseService = courseService(protocol);
        CourseSourceRebindDto request = new CourseSourceRebindDto();
        request.setCourseKey(old.getCourseKey()); request.setRowVersion(3L); request.setSourceCourseKey(source.getCourseKey());
        request.setSourceRowVersion(5L); request.setReason("教务确认课程来源变更，原停课继续有效");
        assertTrue(courseService.rebindSource(request));
        verify(courses).updateById(argThat(c -> "stable-course".equals(c.getCourseKey()) && c.getEffective() == 0
                && "1-2".equals(c.getClassPeriod()) && "new-source".equals(c.getSourceFingerprint())
                && c.getRowVersion() == 4L && "RESOLVED".equals(c.getParseStatus())));
        request.setSourceRowVersion(4L);
        assertThrows(IllegalArgumentException.class, () -> courseService.rebindSource(request));
        verify(courses, times(1)).deleteById(10);
    }

    /**
     * 普通调课不能冒充来源关联复核。
     */
    @Test
    void ordinaryEditKeepsPendingSourceReview() {
        CourseSchedule old = course("1-2"); old.setLocalAdjusted(1); old.setParseStatus("PENDING"); old.setRowVersion(3L);
        when(courses.selectById(9)).thenReturn(old); when(courses.updateById(any())).thenReturn(1);
        CourseSchedule patch = new CourseSchedule(); patch.setId(9); patch.setRowVersion(3L); patch.setChangeReason("调整记录");
        assertTrue(courseService(mock(ScheduleWriteCoordinator.class)).updateCourseSchedule(patch));
        verify(courses).updateById(argThat(c -> "PENDING".equals(c.getParseStatus())));
    }

    /**
     * 构造仅替换事务协议的课程服务，来源数据访问仍由真实服务方法执行。
     *
     * @param protocol 模拟共同事务协议
     * @return 课程服务
     */
    private CourseScheduleServiceImpl courseService(ScheduleWriteCoordinator protocol) {
        CourseScheduleServiceImpl result = new CourseScheduleServiceImpl(
                courses, new ScheduleSyncProperties(), mock(RedisLockUtils.class),
                mock(StringRedisTemplate.class),
                mock(PlatformTransactionManager.class),
                mock(OaApiClient.class), mock(TransactionalMysqlLock.class));
        ReflectionTestUtils.setField(result, "coordinator", protocol);
        return result;
    }

    /**
     * 回执超时不能证明动作失败，保持未知并且不扣减次数。
     */
    @Test
    void timeoutReceiptStaysUnknown() {
        LockCommandMapper mapper = mock(LockCommandMapper.class); when(mapper.complete(any())).thenReturn(1);
        LockCommand command = new LockCommand(); command.setTaskId(77);
        new LockCommandReceiptService(mapper).complete(command, new TimeoutException());
        assertEquals("unknown", command.getStatus()); verify(mapper, never()).consume(any());
    }

    /**
     * 历史编辑保留原课程及请假快照，不调用 OA 或读取今日课表。
     */
    @Test
    void patrolHistoryPreservesUnknownAndSavedLeave() {
        CheckResultMapper mapper = mock(CheckResultMapper.class);
        CheckResult old = new CheckResult(); old.setId(1L); old.setCourseKey("historical-course");
        old.setScheduleSnapshot("historical-snapshot"); old.setPeopleLeave(3); old.setLeaveSource("MANUAL_CONFIRMED");
        old.setClasses("原班级"); old.setSection("1-2"); old.setClassroom("101");
        when(mapper.selectById(1L)).thenReturn(old); when(mapper.updateById(any())).thenReturn(1);
        CheckResultServiceImpl patrol = new CheckResultServiceImpl();
        ReflectionTestUtils.setField(patrol, "checkresultMapper", mapper); ReflectionTestUtils.setField(patrol, "courseScheduleMapper", courses);
        CheckResult patch = new CheckResult(); patch.setId(1L); patrol.addOrEdit(patch);
        assertEquals(3, patch.getPeopleLeave()); assertEquals("historical-snapshot", patch.getScheduleSnapshot());
        patch.setClasses("不同班级"); assertThrows(IllegalArgumentException.class, () -> patrol.addOrEdit(patch));
        verify(courses, never()).selectList(any());
    }

    /**
     * 排考日期预览包含跨日重叠考试，不把已取消安排当作占用。
     */
    @Test
    void rangePreviewIncludesCrossDayAndCourses() {
        when(courses.selectList(any())).thenReturn(Collections.singletonList(course("1-4")));
        Exam cross = exam("2026-10-04T23:00:00", "2026-10-05T01:00:00"); cross.setId(1L);
        Exam cancelled = exam("2026-10-05T12:00:00", "2026-10-05T13:00:00"); cancelled.setId(2L); cancelled.setStatus(2);
        when(exams.selectList(any())).thenReturn(Arrays.asList(cross, cancelled));
        ClassroomOccupancyService occupancy = new ClassroomOccupancyService(rooms, courses, exams, states, coordinator, resolver, properties,
                Clock.fixed(Instant.parse("2026-10-04T17:05:30Z"), ZoneOffset.UTC));
        Map<String, Object> result = occupancy.schedule(1L, LocalDate.of(2026,10,5), LocalDate.of(2026,10,5));
        assertEquals(2, ((List<?>) result.get("events")).size());
        verify(courses, never()).insert(any()); verify(exams, never()).updateById(any());
    }

    /**
     * 实际方法安全代理拒绝匿名、普通账号及旧全局门锁令牌的考试写入。
     */
    @Test
    void methodSecurityRejectsUnauthorizedExamWrites() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(SecurityFixture.class)) {
            ExamController controller = context.getBean(ExamController.class);
            ExamService backend = context.getBean(ExamService.class);
            SecurityContextHolder.clearContext();
            assertThrows(AuthenticationCredentialsNotFoundException.class,
                    () -> controller.batchAdd(Collections.emptyList()));
            for (String role : Arrays.asList("ROLE_user", "ROLE_LOCK_ONLY")) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("user", "unused",
                                Collections.singletonList(new SimpleGrantedAuthority(role))));
                assertThrows(AccessDeniedException.class,
                        () -> controller.batchAdd(Collections.emptyList()));
            }
            verifyNoInteractions(backend);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin", "unused",
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_admin"))));
            controller.batchAdd(Collections.singletonList(exam("2026-10-05T12:00:00", "2026-10-05T13:00:00")));
            verify(backend).batchAdd(anyList());
        } finally { SecurityContextHolder.clearContext(); }
    }

    /**
     * OA 非法或缺失人数不转换成零，明确提供的零人数仍保留。
     *
     * @throws Exception 测试 JSON 解析失败时抛出
     */
    @Test
    void oaUnknownAttendanceDoesNotBecomeZero() throws Exception {
        ObjectMapper json = new ObjectMapper();
        CourseScheduleServiceImpl importer = courseService(mock(ScheduleWriteCoordinator.class));
        CourseSchedule unknown = ReflectionTestUtils.invokeMethod(importer, "convertToCourseSchedule", json.readTree(
                "{\"KCMC\":\"数学\",\"BJMC\":\"一班\",\"SKRQ\":\"20261005\",\"JXBRS\":\"unknown\",\"QJRS\":-1}"));
        assertNotNull(unknown); assertNull(unknown.getClassSize()); assertNull(unknown.getLeaveCount()); assertNull(unknown.getHasLeave());
        CourseSchedule zero = ReflectionTestUtils.invokeMethod(importer, "convertToCourseSchedule", json.readTree(
                "{\"KCMC\":\"数学\",\"BJMC\":\"一班\",\"SKRQ\":\"20261005\",\"JXBRS\":40,\"QJRS\":0}"));
        assertNotNull(zero); assertEquals(40, zero.getClassSize()); assertEquals(0, zero.getLeaveCount());
    }

    /**
     * 注册失败的同一数据库任务能再次对账成功，并持久化各次结果。
     *
     * @throws Exception Quartz 模拟失败时抛出
     */
    @Test
    void quartzRegistrationFailureRemainsRetryable() throws Exception {
        ScheduleService tasks = mock(ScheduleService.class); ScheduleMapper mapper = mock(ScheduleMapper.class);
        SchedulerFactoryBean factory = mock(SchedulerFactoryBean.class);
        Scheduler scheduler = mock(Scheduler.class); when(factory.getScheduler()).thenReturn(scheduler);
        ScheduleTask task = new ScheduleTask(); task.setTaskId(77); task.setLockId(9); task.setUserId(1);
        task.setTaskStatus(1); task.setLoopCount(2); task.setDoorChannel("1"); task.setCountDay("[2,4]");
        task.setHour(8); task.setMinute(30); task.setTimedOperation(1);
        when(tasks.getAll()).thenReturn(Collections.singletonList(task));
        when(scheduler.getJobKeys(any())).thenReturn(Collections.emptySet());
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenThrow(new SchedulerException("模拟注册失败")).thenReturn(new Date());
        LockScheduleReconcileService reconciliation = new LockScheduleReconcileService(tasks, new QuartzConfig(), factory, mapper);
        reconciliation.reconcile(); reconciliation.reconcile();
        verify(mapper).markSyncState(eq(77), eq(0L), eq(1), eq("failed"), anyString(), isNull());
        verify(mapper).markSyncState(eq(77), eq(0L), eq(1), eq("synced"), anyString(), any(LocalDateTime.class));
        assertEquals(2, task.getLoopCount());
    }

    /**
     * 人工复核未知回执只记一次，不发送新的物理动作。
     */
    @Test
    void unknownReceiptReviewCountsOnce() {
        LockCommandMapper mapper = mock(LockCommandMapper.class); LockCommand command = new LockCommand();
        command.setId("unknown-command"); command.setTaskId(77); command.setStatus("unknown");
        when(mapper.selectById("unknown-command")).thenReturn(command); when(mapper.reviewUnknown(any())).thenReturn(1);
        LockCommandReceiptService receipts = new LockCommandReceiptService(mapper);
        ReflectionTestUtils.setField(receipts, "coordinator", mock(ScheduleWriteCoordinator.class));
        LockCommandReviewDto review = new LockCommandReviewDto();
        review.setExecuted(true); review.setReason("现场确认原指令已执行，确认来源为值班人员核查");
        receipts.review("unknown-command", review);
        assertEquals("acknowledged", command.getStatus());
        assertThrows(IllegalArgumentException.class, () -> receipts.review("unknown-command", review));
        verify(mapper, times(1)).consume(77); verify(mapper, never()).invalidateObservation(any());
    }

    /**
     * 任务在触发读取后被更新，登记事务核实新版本并拦截旧设备动作。
     */
    @Test
    void commandReservationRejectsChangedTaskVersion() {
        LockCommandMapper mapper = mock(LockCommandMapper.class);
        SmartLockGateway gateway = mock(SmartLockGateway.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        ScheduleTask current = new ScheduleTask(); current.setTaskId(77); current.setRowVersion(3L);
        current.setLockId(9); current.setTaskStatus(1); current.setLoopCount(2); current.setDoorChannel("1"); current.setTimedOperation(1);
        when(mapper.lockTask(77)).thenReturn(current);
        LockInfo lock = new LockInfo(); lock.setLockId(9); lock.setClassroomId(1L); lock.setDoorChannel("1");
        lock.setIpAddress("127.0.0.1"); lock.setPortNumber(8000); lock.setSnCode("001A");
        LockCommandService commands = new LockCommandService(
                mapper, gateway, new LockCommandReceiptService(mapper), manager);
        assertThrows(IllegalStateException.class, () -> commands.submit(lock, "1", true, "user:1", 77, "old-task-fire", 2L));
        verifyNoInteractions(gateway); verify(mapper, never()).insert(any());
    }

    /**
     * 未开始和进行中接收旧值，非法状态码拒绝保存。
     */
    @Test
    void examStatusOnlyAllowsActiveOrCancelled() {
        Exam row = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00");
        row.setStatus(5);
        assertTrue(service.preview(Arrays.asList(row), false).contains("状态不合法"));
        assertTrue(service.batchAdd(Arrays.asList(row)).contains("状态不合法"));
        verify(exams, never()).insert(any());
        verify(exams, never()).updateById(any());
        row.setStatus(1); assertNull(service.preview(Arrays.asList(row), false));
        row.setStatus(0); assertNull(service.preview(Arrays.asList(row), false));
    }

    /**
     * 普通编辑校正未来考试为未开始，保留媒体和时间并递增行版本。
     */
    @Test
    void editingExamUnifiesStatusWithoutLosingOtherFields() {
        Exam old = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00");
        old.setId(19L); old.setStatus(1); old.setRowVersion(7L); old.setImageUrl("existing.png");
        when(exams.selectList(any())).thenReturn(Arrays.asList(old));
        Exam zero = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00");
        zero.setId(20L); zero.setClassroomId(2L); zero.setRowVersion(7L); zero.setImageUrl("zero.png");
        when(exams.selectList(any())).thenReturn(Arrays.asList(old, zero));
        Exam patch = new Exam(); patch.setId(19L); patch.setRowVersion(7L); patch.setExamContent("修改考试内容");
        Exam zeroPatch = new Exam(); zeroPatch.setId(20L); zeroPatch.setRowVersion(7L); zeroPatch.setExamContent("修改另一考试内容");
        assertNull(service.batchUpdate(Arrays.asList(patch, zeroPatch)));
        ArgumentCaptor<Exam> saved = ArgumentCaptor.forClass(Exam.class);
        verify(exams, times(2)).updateById(saved.capture());
        Exam first = saved.getAllValues().get(0), second = saved.getAllValues().get(1);
        assertEquals(Integer.valueOf(0), first.getStatus());
        assertEquals(Integer.valueOf(0), second.getStatus());
        assertEquals(Long.valueOf(8L), first.getRowVersion());
        assertEquals("existing.png", first.getImageUrl());
        assertEquals("zero.png", second.getImageUrl());
        assertEquals(old.getStartTime(), first.getStartTime());
        assertEquals(old.getEndTime(), first.getEndTime());
    }

    /**
     * 新考试未传状态时按时间确定未开始状态。
     */
    @Test
    void newExamDefaultsToExistingActiveStatus() {
        Exam row = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); row.setStatus(null);
        assertNull(service.batchAdd(Arrays.asList(row)));
        ArgumentCaptor<Exam> saved = ArgumentCaptor.forClass(Exam.class);
        verify(exams).insert(saved.capture());
        assertEquals(Integer.valueOf(0), saved.getValue().getStatus());
    }

    /**
     * 设置确定的学校时刻。
     * @param value 学校当地时间
     */
    private void setExamTime(String value) {
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(LocalDateTime.parse(value).atZone(CoursePeriodResolver.ZONE).toInstant(), CoursePeriodResolver.ZONE));
    }

    /**
     * 自动跨越边界、停机补齐和旧终态不复活，重复运行不重复审计。
     */
    @Test
    void lifecycleRefreshPersistsBoundariesAndPreservesTerminalStates() {
        Exam future = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); future.setId(1L); future.setStatus(1);
        Exam running = exam("2026-10-04T09:00:00", "2026-10-04T10:00:00"); running.setId(2L);
        Exam legacy = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); legacy.setId(3L); legacy.setStatus(2);
        Exam early = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); early.setId(4L); early.setStatus(3);
        Exam cancelled = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); cancelled.setId(5L); cancelled.setStatus(4);
        List<Exam> stored = new ArrayList<>(Arrays.asList(future, running, legacy, early, cancelled));
        when(exams.selectList(any())).thenAnswer(invocation -> new ArrayList<>(stored));
        when(exams.updateById(any())).thenAnswer(invocation -> {
            Exam row = invocation.getArgument(0); stored.removeIf(item -> item.getId().equals(row.getId())); stored.add(row); return 1;
        });
        assertEquals(2, service.refreshStatuses());
        assertEquals(0, stored.stream().filter(row -> row.getId().equals(1L)).findFirst().get().getStatus());
        assertEquals(2, stored.stream().filter(row -> row.getId().equals(2L)).findFirst().get().getStatus());
        assertEquals(0, service.refreshStatuses());
        setExamTime("2026-10-05T09:00:00"); assertEquals(1, service.refreshStatuses());
        setExamTime("2026-10-05T10:00:00"); assertEquals(1, service.refreshStatuses());
        assertEquals(0, service.refreshStatuses());
        assertEquals(3, early.getStatus()); assertEquals(4, cancelled.getStatus()); assertEquals(2, legacy.getStatus());
        ArgumentCaptor<ScheduleAudit> audit = ArgumentCaptor.forClass(ScheduleAudit.class);
        verify(audits, times(3)).insert(audit.capture());
        for (ScheduleAudit record : audit.getAllValues()) {
            assertEquals("EXAM_STATUS_AUTO", record.getAction()); assertEquals("system", record.getActor());
            assertTrue(record.getDetail().contains("\"before\"")); assertTrue(record.getDetail().contains("\"after\""));
        }
    }

    /**
     * 只有进行中的考试允许提前结束，取消和重新启用保留正确时间及版本。
     */
    @Test
    void earlyEndCancellationAndReactivationHaveDistinctStates() {
        Exam old = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); old.setId(10L);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(old));
        Exam patch = new Exam(); patch.setId(10L); patch.setRowVersion(0L); patch.setStatus(3);
        assertTrue(service.batchUpdate(Collections.singletonList(patch)).contains("只有进行中"));
        patch.setStatus(2); assertTrue(service.batchUpdate(Collections.singletonList(patch)).contains("尚未到结束时间"));
        setExamTime("2026-10-05T09:30:00"); patch.setStatus(3);
        assertNull(service.batchUpdate(Collections.singletonList(patch)));
        ArgumentCaptor<Exam> saved = ArgumentCaptor.forClass(Exam.class);
        verify(exams).updateById(saved.capture());
        assertEquals(3, saved.getValue().getStatus()); assertEquals(LocalDateTime.parse("2026-10-05T09:30:00"), saved.getValue().getActualEndTime());
        patch.setStatus(4); assertNull(service.batchUpdate(Collections.singletonList(patch)));
        verify(exams, times(2)).updateById(saved.capture());
        assertEquals(4, saved.getValue().getStatus()); assertNull(saved.getValue().getActualEndTime());
        old.setStatus(4); old.setActualEndTime(LocalDateTime.parse("2026-10-05T09:20:00"));
        patch.setStatus(0); assertNull(service.batchUpdate(Collections.singletonList(patch)));
        verify(exams, times(3)).updateById(saved.capture());
        assertEquals(1, saved.getValue().getStatus()); assertNull(saved.getValue().getActualEndTime());
    }

    /**
     * 自动更新失败必须抛出异常，让事务回滚并允许下一轮重试。
     */
    @Test
    void automaticRefreshFailureDoesNotWriteAudit() {
        Exam old = exam("2026-10-04T09:00:00", "2026-10-04T10:00:00"); old.setId(10L);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(old)); when(exams.updateById(any())).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.refreshStatuses()); verify(audits, never()).insert(any());
    }

    /**
     * 考试记录菜单只从获授权的开锁记录派生，同一权限且不重复添加。
     */
    @Test
    void examRecordMenuInheritsUnlockRecordPermission() {
        Menu lock = new Menu(); lock.setMenuId(12); lock.setComponent("smartlockrecord"); lock.setCode("smart:record"); lock.setParentId(8); lock.setIsNav(true);
        List<Menu> tree = new ArrayList<>(Collections.singletonList(lock));
        MenuController.appendExamRecords(tree);
        assertEquals(2, tree.size()); assertEquals("examrecord", tree.get(1).getComponent()); assertEquals("/home/examrecord", tree.get(1).getPath());
        assertEquals(lock.getCode(), tree.get(1).getCode()); assertEquals(lock.getParentId(), tree.get(1).getParentId());
        MenuController.appendExamRecords(tree); assertEquals(2, tree.size());
        List<Menu> denied = new ArrayList<>(); MenuController.appendExamRecords(denied); assertTrue(denied.isEmpty());
    }

    /**
     * 考试记录分页参数及时间范围须合法，读取不产生业务写入。
     */
    @Test
    void examRecordQueryIsBoundedAndReadOnly() {
        ExamRecordService records = new ExamRecordService(audits);
        ExamRecordQuery query = new ExamRecordQuery();
        query.setSize(101); assertThrows(IllegalArgumentException.class, () -> records.list(query));
        query.setSize(20); query.setFrom(LocalDateTime.parse("2026-10-05T10:00:00")); query.setTo(LocalDateTime.parse("2026-10-05T09:00:00"));
        assertThrows(IllegalArgumentException.class, () -> records.list(query));
        query.setTo(null); query.setPage(2); when(audits.countExamRecords(query)).thenReturn(21L);
        assertEquals(21L, records.list(query).get("total")); verify(audits).pageExamRecords(query, 20L);
        verify(audits, never()).insert(any());
    }

    /**
     * 考试记录与开锁记录采用登录用户权限，匿名不得读取。
     */
    @Test
    void examRecordReadUsesAuthenticatedPermissionRatherThanAdminWritePermission() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(SecurityFixture.class)) {
            ExamRecordController controller = context.getBean(ExamRecordController.class);
            UserService users = context.getBean(UserService.class); ExamRecordService records = context.getBean(ExamRecordService.class);
            SecurityContextHolder.clearContext();
            assertThrows(AuthenticationCredentialsNotFoundException.class,
                    () -> controller.list(new ExamRecordQuery(), null));
            Authentication authenticated = new UsernamePasswordAuthenticationToken("user", "unused",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_user")));
            SecurityContextHolder.getContext().setAuthentication(authenticated);
            when(users.getUserByName("user")).thenReturn(new User());
            controller.list(new ExamRecordQuery(), authenticated); verify(records).list(any());
            when(users.getUserByName("user")).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> controller.list(new ExamRecordQuery(), authenticated));
            verify(records, times(1)).list(any());
        } finally { SecurityContextHolder.clearContext(); }
    }

    /**
     * 记录筛选 SQL 只包含考试操作，所有输入均以绑定参数传入。
     * @throws Exception XML 解析失败
     */
    @Test
    void examRecordSqlBindsFiltersAndExcludesScheduleIssues() throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        String resource = "mapper/ScheduleAuditMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        ExamRecordQuery query = new ExamRecordQuery(); query.setActor("' OR 1=1 --"); query.setAction("EXAM_UPDATE");
        query.setFrom(LocalDateTime.parse("2026-10-05T09:00:00")); query.setTo(LocalDateTime.parse("2026-10-05T10:00:00"));
        Map<String, Object> params = new HashMap<>(); params.put("query", query); params.put("offset", 20L);
        BoundSql sql = configuration.getMappedStatement("com.hnkjzyxy.ab.mapper.ScheduleAuditMapper.pageExamRecords").getBoundSql(params);
        assertTrue(sql.getSql().contains("EXAM_STATUS_AUTO")); assertFalse(sql.getSql().contains("EXAM_BINDING_UNKNOWN"));
        assertFalse(sql.getSql().contains(query.getActor())); assertEquals(6, sql.getParameterMappings().size());
    }

    /**
     * 三种终态释放教室且不再要求已删除设备的身份绑定。
     */
    @Test
    void allTerminalStatesReleaseOccupancyAndAllowCourseWrite() {
        ClassroomOccupancyService occupancy = new ClassroomOccupancyService(rooms, courses, exams, states, coordinator, resolver, properties,
                Clock.fixed(LocalDateTime.parse("2026-10-05T09:30:00").atZone(CoursePeriodResolver.ZONE).toInstant(), CoursePeriodResolver.ZONE));
        for (int status : Arrays.asList(2, 3, 4)) {
            Exam old = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); old.setId(10L); old.setStatus(status);
            old.setClassroomId(null); old.setBoardSn("removed-device");
            when(exams.selectList(any())).thenReturn(Collections.singletonList(old));
            Map<?, ?> room = (Map<?, ?>) ((List<?>) occupancy.snapshot(1L, null).get("classrooms")).get(0);
            assertEquals("idle", room.get("occupancyStatus")); assertTrue(((List<?>) room.get("upcomingEvents")).isEmpty());
            assertTrue(((List<?>) occupancy.schedule(1L, LocalDate.of(2026,10,5), LocalDate.of(2026,10,5)).get("events")).isEmpty());
            assertDoesNotThrow(() -> coordinator.validateCourseWrite(course("1-2")));
            assertNull(service.preview(Collections.singletonList(exam("2026-10-05T09:00:00", "2026-10-05T10:00:00")), false));
        }
    }

    /**
     * 单场详情和列表筛选读取前均持久化最新状态及版本。
     */
    @Test
    void examReadsRefreshBeforeReturningOrFiltering() {
        Exam old = exam("2026-10-05T09:00:00", "2026-10-05T10:00:00"); old.setId(10L);
        when(exams.selectList(any())).thenReturn(Collections.singletonList(old));
        when(exams.updateById(any())).thenAnswer(invocation -> {
            Exam row = invocation.getArgument(0); old.setStatus(row.getStatus()); old.setRowVersion(row.getRowVersion()); old.setActualEndTime(row.getActualEndTime()); return 1;
        });
        when(exams.selectById(10L)).thenReturn(old); when(exams.getExamList(any())).thenReturn(Collections.singletonList(old));
        setExamTime("2026-10-05T09:00:00"); assertEquals(1, service.getById(10L).getStatus());
        setExamTime("2026-10-05T10:00:00"); Exam filter = new Exam(); filter.setStatus(2);
        assertEquals(2, service.getExamList(filter).get(0).getStatus()); assertEquals("ended", old.getTimeStatus());
        assertEquals(LocalDateTime.parse("2026-10-05T10:00:00"), old.getActualEndTime());
        assertEquals(2L, old.getRowVersion());
    }

    /**
     * 仅加载方法安全及模拟考试服务，不连接应用数据源、Redis 或设备。
     */
    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    public static class SecurityFixture {
        /**
         * 创建模拟业务服务。
         * @return 考试业务替身
         */
        @Bean
        public ExamService exams() { return mock(ExamService.class); }

        /**
         * 模拟记录查询服务。
         * @return 记录服务替身
         */
        @Bean
        public ExamRecordService records() { return mock(ExamRecordService.class); }

        /**
         * 模拟账号目录。
         * @return 用户服务替身
         */
        @Bean
        public UserService users() { return mock(UserService.class); }

        /**
         * 实际记录控制器，加载真实方法权限。
         * @return 考试记录控制器
         */
        @Bean
        public ExamRecordController recordController(ExamRecordService records, UserService users) {
            return new ExamRecordController(records, users);
        }


        /**
         * 使用实际控制器与方法权限表达式。
         * @return 考试控制器
         */
        @Bean
        public ExamController controller() { return new ExamController(); }
    }
}
