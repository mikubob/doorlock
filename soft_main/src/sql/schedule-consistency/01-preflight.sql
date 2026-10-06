-- 课表联动业务一致性：升级前只读核查，不修改数据库。
SELECT DATABASE() AS checked_database, @@hostname AS database_host, @@port AS database_port;

-- 只读核查。先保存结果并核对现库与迁移字段；不自动修改任何绑定。
SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN
 ('sys_classroom','sys_exam','sys_course_schedule','sys_lock_info','sys_schedule_task','sys_check_result',
  'sys_schedule_state','sys_schedule_audit','sys_lock_command');

SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN
 ('sys_classroom','sys_exam','sys_course_schedule','sys_lock_info','sys_schedule_task','sys_check_result',
  'sys_schedule_state','sys_schedule_audit','sys_lock_command')
ORDER BY TABLE_NAME,ORDINAL_POSITION;

-- 部分迁移后需核对索引；不能仅根据字段存在判断迁移成功。
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,
 GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
AND TABLE_NAME IN ('sys_exam','sys_course_schedule','sys_lock_command','sys_schedule_audit')
GROUP BY TABLE_NAME,INDEX_NAME,NON_UNIQUE ORDER BY TABLE_NAME,INDEX_NAME;

-- 位置重复会使无主键课程绑定未知，须人工核实不同校区/楼栋和教室编号。
SELECT XQMC,JZWMC,JSH,COUNT(*) records FROM sys_classroom
GROUP BY XQMC,JZWMC,JSH HAVING COUNT(*)>1;

-- SN 按原样、大小写敏感核查；不可用数字强制转换恢复已经丢失的前导零。
SELECT BINARY board_sn board_sn,COUNT(*) records FROM sys_classroom
WHERE board_sn IS NOT NULL AND board_sn<>'' GROUP BY BINARY board_sn HAVING COUNT(*)>1;

SELECT e.id,e.board_sn FROM sys_exam e LEFT JOIN sys_classroom c
 ON BINARY e.board_sn=BINARY c.board_sn WHERE c.id IS NULL;

-- 同号不同教室禁止自动回填；同教室多锁须分别指定通道。
SELECT JSH,COUNT(*) records FROM sys_classroom GROUP BY JSH HAVING COUNT(*)>1;
SELECT lock_id,JSH,remarks FROM sys_lock_info WHERE remarks IS NULL OR remarks NOT REGEXP '^[1-4]$';
SELECT task_id,lock_id,count_day,remarks,loop_count,task_status FROM sys_schedule_task
WHERE remarks IS NULL OR remarks NOT REGEXP '^[1-4]$' OR loop_count<0;

-- 历史状态二及实际结束来源需业务复核，迁移不能据时间擅自恢复。
SELECT status,COUNT(*) records FROM sys_exam GROUP BY status;
SELECT id,start_time,end_time,status FROM sys_exam
WHERE start_time IS NULL OR end_time IS NULL OR end_time<=start_time OR status NOT IN (0,1,2,3,4);
