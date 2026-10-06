-- 只读核验：第一组缺失字段应为 0 行；不得忽略查询错误。
-- 若仍缺字段/表，停止启动后端，保留结果用于按现表结构续补。
SELECT DATABASE() AS verified_database, @@hostname AS database_host, @@port AS database_port;

SELECT expected.table_name AS missing_table, expected.column_name AS missing_column
FROM (
  SELECT 'sys_classroom' AS table_name, 'board_sn' AS column_name
  UNION ALL SELECT 'sys_classroom' AS table_name, 'capacity' AS column_name
  UNION ALL SELECT 'sys_exam' AS table_name, 'classroom_id' AS column_name
  UNION ALL SELECT 'sys_exam' AS table_name, 'row_version' AS column_name
  UNION ALL SELECT 'sys_exam' AS table_name, 'actual_end_time' AS column_name
  UNION ALL SELECT 'sys_exam' AS table_name, 'board_sn' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'classroom_id' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'course_key' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'source_type' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'source_fingerprint' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'local_adjusted' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'effective' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'parse_status' AS column_name
  UNION ALL SELECT 'sys_course_schedule' AS table_name, 'row_version' AS column_name
  UNION ALL SELECT 'sys_lock_info' AS table_name, 'classroom_id' AS column_name
  UNION ALL SELECT 'sys_lock_info' AS table_name, 'door_channel' AS column_name
  UNION ALL SELECT 'sys_lock_info' AS table_name, 'observed_at' AS column_name
  UNION ALL SELECT 'sys_schedule_task' AS table_name, 'door_channel' AS column_name
  UNION ALL SELECT 'sys_schedule_task' AS table_name, 'row_version' AS column_name
  UNION ALL SELECT 'sys_schedule_task' AS table_name, 'quartz_sync_status' AS column_name
  UNION ALL SELECT 'sys_schedule_task' AS table_name, 'quartz_sync_message' AS column_name
  UNION ALL SELECT 'sys_schedule_task' AS table_name, 'quartz_synced_at' AS column_name
  UNION ALL SELECT 'sys_check_result' AS table_name, 'people_leave' AS column_name
  UNION ALL SELECT 'sys_check_result' AS table_name, 'course_key' AS column_name
  UNION ALL SELECT 'sys_check_result' AS table_name, 'schedule_snapshot' AS column_name
  UNION ALL SELECT 'sys_check_result' AS table_name, 'leave_source' AS column_name
  UNION ALL SELECT 'sys_check_result' AS table_name, 'supplement_reason' AS column_name
  UNION ALL SELECT 'sys_schedule_state' AS table_name, 'id' AS column_name
  UNION ALL SELECT 'sys_schedule_state' AS table_name, 'schedule_version' AS column_name
  UNION ALL SELECT 'sys_schedule_state' AS table_name, 'last_success' AS column_name
  UNION ALL SELECT 'sys_schedule_state' AS table_name, 'coverage_start' AS column_name
  UNION ALL SELECT 'sys_schedule_state' AS table_name, 'coverage_end' AS column_name
  UNION ALL SELECT 'sys_schedule_state' AS table_name, 'policy_version' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'id' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'schedule_version' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'action' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'actor' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'detail' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'issue_status' AS column_name
  UNION ALL SELECT 'sys_schedule_audit' AS table_name, 'created_time' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'id' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'request_id' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'lock_id' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'classroom_id' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'task_id' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'device_sn' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'door_channel' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'operation' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'status' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'actor' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'error_message' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'created_time' AS column_name
  UNION ALL SELECT 'sys_lock_command' AS table_name, 'completed_time' AS column_name
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE()
 AND actual.TABLE_NAME=expected.table_name AND actual.COLUMN_NAME=expected.column_name
WHERE actual.COLUMN_NAME IS NULL ORDER BY expected.table_name,expected.column_name;

-- 查看类型、NULL 约束及默认值，不能只凭 COLUMN_TYPE 判断迁移完整。
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND (
  (TABLE_NAME IN ('sys_classroom','sys_exam') AND COLUMN_NAME='board_sn')
  OR (TABLE_NAME='sys_classroom' AND COLUMN_NAME='capacity')
  OR (TABLE_NAME='sys_course_schedule' AND COLUMN_NAME IN ('JXBRS','QJRS','SFYQJRS','source_type','effective','row_version'))
  OR (TABLE_NAME='sys_check_result' AND COLUMN_NAME='people_leave')
  OR (TABLE_NAME='sys_lock_info' AND COLUMN_NAME='switch_status')
  OR (TABLE_NAME='sys_schedule_task' AND COLUMN_NAME IN ('row_version','quartz_sync_status'))
) ORDER BY TABLE_NAME,COLUMN_NAME;

SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
AND TABLE_NAME IN ('sys_classroom','sys_exam','sys_course_schedule','sys_lock_info','sys_schedule_task','sys_check_result','sys_schedule_state','sys_schedule_audit','sys_lock_command');

-- course_key 单列唯一索引、request_id 单列唯一索引必须存在；其余索引用于查询性能。
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
AND TABLE_NAME IN ('sys_course_schedule','sys_exam','sys_schedule_audit','sys_lock_command')
GROUP BY TABLE_NAME,INDEX_NAME,NON_UNIQUE ORDER BY TABLE_NAME,INDEX_NAME;

SELECT * FROM sys_schedule_state WHERE id=1;
SELECT course_key,COUNT(*) AS duplicate_count FROM sys_course_schedule
GROUP BY course_key HAVING course_key IS NULL OR COUNT(*)>1;

-- 以下结果是需要人工核实的业务数据，不自动恢复/启用任务，不修改物理门锁。
SELECT task_id,task_status,loop_count,door_channel FROM sys_schedule_task
WHERE loop_count=0 OR door_channel IS NULL OR door_channel NOT REGEXP '^[1-4]$';
SELECT lock_id,JSH,classroom_id,door_channel FROM sys_lock_info
WHERE classroom_id IS NULL OR door_channel IS NULL OR door_channel NOT REGEXP '^[1-4]$';

-- 0 未开始、1 进行中、2 已结束、3 提前结束、4 取消安排；正常状态由应用自动更新。
SELECT id,status,row_version FROM sys_exam WHERE status IS NULL OR status NOT IN (0,1,2,3,4);
