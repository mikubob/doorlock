-- 课表联动业务一致性：统一结构升级，适用于旧库或部分迁移的库。
-- 执行顺序：备份并停止后端 -> 01-preflight.sql -> 本文件 -> 03-verify.sql。
-- 当前用户数据库已核验通过，不需要因脚本整理而重新执行。
-- 应用不会自动执行本文件；任一 SQL 错误或 STOP 提示立即停止。
-- 只补缺失列/表/索引，不删除业务记录、不覆盖已有绑定、课程标识或来源。
-- 不自动把任务零次转为无限；旧无限语义必须人工核实后在管理端配置。
-- DDL 有隐式提交；存在但结构不完整的新增表由 03-verify.sql 报告，不能盲目猜补。
-- 需在同一连接按文件顺序执行，保留执行记录。
SELECT DATABASE() AS migration_database, @@hostname AS database_host, @@port AS database_port;

-- sys_classroom：只添加尚不存在的列。
SELECT GROUP_CONCAT(CONCAT('ADD ', expected.column_name, ' ', expected.column_definition)
  ORDER BY expected.ordinal_no SEPARATOR ', ') INTO @schedule_missing_columns
FROM (
  SELECT 1 AS ordinal_no, 'capacity' AS column_name, 'INT NULL' AS column_definition
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE() AND actual.TABLE_NAME='sys_classroom'
 AND BINARY actual.COLUMN_NAME=BINARY expected.column_name
WHERE actual.COLUMN_NAME IS NULL;
SET @schedule_migration_sql = IF(@schedule_missing_columns IS NULL,
  'SELECT ''sys_classroom: columns already present'' AS migration_step',
  CONCAT('ALTER TABLE sys_classroom ', @schedule_missing_columns));
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- sys_exam：只添加尚不存在的列。
SELECT GROUP_CONCAT(CONCAT('ADD ', expected.column_name, ' ', expected.column_definition)
  ORDER BY expected.ordinal_no SEPARATOR ', ') INTO @schedule_missing_columns
FROM (
  SELECT 1 AS ordinal_no, 'classroom_id' AS column_name, 'BIGINT NULL' AS column_definition
  UNION ALL
  SELECT 2 AS ordinal_no, 'row_version' AS column_name, 'BIGINT NOT NULL DEFAULT 0' AS column_definition
  UNION ALL
  SELECT 3 AS ordinal_no, 'actual_end_time' AS column_name, 'DATETIME NULL' AS column_definition
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE() AND actual.TABLE_NAME='sys_exam'
 AND BINARY actual.COLUMN_NAME=BINARY expected.column_name
WHERE actual.COLUMN_NAME IS NULL;
SET @schedule_migration_sql = IF(@schedule_missing_columns IS NULL,
  'SELECT ''sys_exam: columns already present'' AS migration_step',
  CONCAT('ALTER TABLE sys_exam ', @schedule_missing_columns));
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- sys_course_schedule：只添加尚不存在的列。
SELECT GROUP_CONCAT(CONCAT('ADD ', expected.column_name, ' ', expected.column_definition)
  ORDER BY expected.ordinal_no SEPARATOR ', ') INTO @schedule_missing_columns
FROM (
  SELECT 1 AS ordinal_no, 'classroom_id' AS column_name, 'BIGINT NULL' AS column_definition
  UNION ALL
  SELECT 2 AS ordinal_no, 'course_key' AS column_name, 'VARCHAR(64) NULL' AS column_definition
  UNION ALL
  SELECT 3 AS ordinal_no, 'source_type' AS column_name, 'VARCHAR(16) NOT NULL DEFAULT ''UNVERIFIED''' AS column_definition
  UNION ALL
  SELECT 4 AS ordinal_no, 'source_fingerprint' AS column_name, 'VARCHAR(64) NULL' AS column_definition
  UNION ALL
  SELECT 5 AS ordinal_no, 'local_adjusted' AS column_name, 'INT NOT NULL DEFAULT 0' AS column_definition
  UNION ALL
  SELECT 6 AS ordinal_no, 'effective' AS column_name, 'INT NOT NULL DEFAULT 1' AS column_definition
  UNION ALL
  SELECT 7 AS ordinal_no, 'parse_status' AS column_name, 'VARCHAR(16) NULL' AS column_definition
  UNION ALL
  SELECT 8 AS ordinal_no, 'row_version' AS column_name, 'BIGINT NOT NULL DEFAULT 0' AS column_definition
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE() AND actual.TABLE_NAME='sys_course_schedule'
 AND BINARY actual.COLUMN_NAME=BINARY expected.column_name
WHERE actual.COLUMN_NAME IS NULL;
SET @schedule_migration_sql = IF(@schedule_missing_columns IS NULL,
  'SELECT ''sys_course_schedule: columns already present'' AS migration_step',
  CONCAT('ALTER TABLE sys_course_schedule ', @schedule_missing_columns));
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- sys_lock_info：只添加尚不存在的列。
SELECT GROUP_CONCAT(CONCAT('ADD ', expected.column_name, ' ', expected.column_definition)
  ORDER BY expected.ordinal_no SEPARATOR ', ') INTO @schedule_missing_columns
FROM (
  SELECT 1 AS ordinal_no, 'classroom_id' AS column_name, 'BIGINT NULL' AS column_definition
  UNION ALL
  SELECT 2 AS ordinal_no, 'door_channel' AS column_name, 'VARCHAR(32) NULL' AS column_definition
  UNION ALL
  SELECT 3 AS ordinal_no, 'observed_at' AS column_name, 'DATETIME NULL' AS column_definition
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE() AND actual.TABLE_NAME='sys_lock_info'
 AND BINARY actual.COLUMN_NAME=BINARY expected.column_name
WHERE actual.COLUMN_NAME IS NULL;
SET @schedule_migration_sql = IF(@schedule_missing_columns IS NULL,
  'SELECT ''sys_lock_info: columns already present'' AS migration_step',
  CONCAT('ALTER TABLE sys_lock_info ', @schedule_missing_columns));
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- sys_schedule_task：只添加尚不存在的列。
SELECT GROUP_CONCAT(CONCAT('ADD ', expected.column_name, ' ', expected.column_definition)
  ORDER BY expected.ordinal_no SEPARATOR ', ') INTO @schedule_missing_columns
FROM (
  SELECT 1 AS ordinal_no, 'door_channel' AS column_name, 'VARCHAR(32) NULL' AS column_definition
  UNION ALL
  SELECT 2 AS ordinal_no, 'row_version' AS column_name, 'BIGINT NOT NULL DEFAULT 0' AS column_definition
  UNION ALL
  SELECT 3 AS ordinal_no, 'quartz_sync_status' AS column_name, 'VARCHAR(16) NOT NULL DEFAULT ''pending''' AS column_definition
  UNION ALL
  SELECT 4 AS ordinal_no, 'quartz_sync_message' AS column_name, 'VARCHAR(500) NULL' AS column_definition
  UNION ALL
  SELECT 5 AS ordinal_no, 'quartz_synced_at' AS column_name, 'DATETIME NULL' AS column_definition
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE() AND actual.TABLE_NAME='sys_schedule_task'
 AND BINARY actual.COLUMN_NAME=BINARY expected.column_name
WHERE actual.COLUMN_NAME IS NULL;
SET @schedule_migration_sql = IF(@schedule_missing_columns IS NULL,
  'SELECT ''sys_schedule_task: columns already present'' AS migration_step',
  CONCAT('ALTER TABLE sys_schedule_task ', @schedule_missing_columns));
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- sys_check_result：只添加尚不存在的列。
SELECT GROUP_CONCAT(CONCAT('ADD ', expected.column_name, ' ', expected.column_definition)
  ORDER BY expected.ordinal_no SEPARATOR ', ') INTO @schedule_missing_columns
FROM (
  SELECT 1 AS ordinal_no, 'course_key' AS column_name, 'VARCHAR(64) NULL' AS column_definition
  UNION ALL
  SELECT 2 AS ordinal_no, 'schedule_snapshot' AS column_name, 'LONGTEXT NULL' AS column_definition
  UNION ALL
  SELECT 3 AS ordinal_no, 'leave_source' AS column_name, 'VARCHAR(32) NULL' AS column_definition
  UNION ALL
  SELECT 4 AS ordinal_no, 'supplement_reason' AS column_name, 'VARCHAR(500) NULL' AS column_definition
) expected
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA=DATABASE() AND actual.TABLE_NAME='sys_check_result'
 AND BINARY actual.COLUMN_NAME=BINARY expected.column_name
WHERE actual.COLUMN_NAME IS NULL;
SET @schedule_migration_sql = IF(@schedule_missing_columns IS NULL,
  'SELECT ''sys_check_result: columns already present'' AS migration_step',
  CONCAT('ALTER TABLE sys_check_result ', @schedule_missing_columns));
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

CREATE TABLE IF NOT EXISTS sys_schedule_state (
  id INT PRIMARY KEY, schedule_version BIGINT NOT NULL DEFAULT 0,
  last_success DATETIME NULL, coverage_start DATE NULL, coverage_end DATE NULL,
  policy_version VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_schedule_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, schedule_version BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL, actor VARCHAR(128) NOT NULL, detail LONGTEXT NULL,
  issue_status VARCHAR(16) NULL, created_time DATETIME NOT NULL,
  INDEX idx_schedule_audit_version(schedule_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_lock_command (
  id VARCHAR(64) PRIMARY KEY, request_id VARCHAR(200) NOT NULL UNIQUE,
  lock_id INT NOT NULL, classroom_id BIGINT NULL, task_id INT NULL,
  device_sn VARCHAR(128) NOT NULL, door_channel VARCHAR(32) NOT NULL,
  operation INT NOT NULL, status VARCHAR(32) NOT NULL, actor VARCHAR(128) NOT NULL,
  error_message VARCHAR(500) NULL, created_time DATETIME NOT NULL,
  completed_time DATETIME NULL, INDEX idx_lock_command_pending(status,created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 只补缺失的状态行，不重置已有版本、覆盖范围或最近成功时间。
INSERT INTO sys_schedule_state(id)
SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM sys_schedule_state WHERE id=1);

-- 旧版本的指令表如缺绑定快照，只补可空列，不猜测历史绑定。
SET @schedule_migration_sql = IF(EXISTS(
  SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_lock_command' AND COLUMN_NAME='classroom_id'),
  'SELECT ''sys_lock_command.classroom_id: already present'' AS migration_step',
  'ALTER TABLE sys_lock_command ADD classroom_id BIGINT NULL');
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- unsigned zerofill 转 signed INT 前检查范围；超限时不修改并输出待处理信息。
SET @schedule_migration_sql = IF(EXISTS(
  SELECT 1 FROM sys_check_result WHERE people_leave > 2147483647),
  'SELECT ''STOP: people_leave exceeds signed INT; review before application startup'' AS migration_step',
  'ALTER TABLE sys_check_result MODIFY people_leave INT NULL');
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

ALTER TABLE sys_course_schedule
  MODIFY JXBRS INT NULL DEFAULT NULL,
  MODIFY QJRS INT NULL DEFAULT NULL,
  MODIFY SFYQJRS VARCHAR(10) NULL DEFAULT NULL;
-- 类型列表没有 NULL 约束信息；重复 MODIFY 不新增列，不改已有容量数值。
ALTER TABLE sys_classroom MODIFY board_sn VARCHAR(128) NULL, MODIFY capacity INT NULL;
ALTER TABLE sys_exam MODIFY board_sn VARCHAR(128) NULL;
ALTER TABLE sys_lock_info MODIFY switch_status INT NULL;

-- 只给缺失的旧课程身份分配 UUID，已有键/来源/调整保持原值。
UPDATE sys_course_schedule SET course_key=UUID() WHERE course_key IS NULL;

SET @schedule_migration_sql = IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_exam' AND INDEX_NAME='idx_exam_room_time'),
  'SELECT ''sys_exam.idx_exam_room_time: already present'' AS migration_step',
  'ALTER TABLE sys_exam ADD INDEX idx_exam_room_time(classroom_id,start_time,end_time)');
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

SET @schedule_migration_sql = IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_course_schedule' AND INDEX_NAME='idx_course_room_date'),
  'SELECT ''sys_course_schedule.idx_course_room_date: already present'' AS migration_step',
  'ALTER TABLE sys_course_schedule ADD INDEX idx_course_room_date(classroom_id,SKRQ)');
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

SET @schedule_migration_sql = IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_course_schedule' AND INDEX_NAME='uk_course_key'),
  'SELECT ''sys_course_schedule.uk_course_key: already present'' AS migration_step',
  'ALTER TABLE sys_course_schedule ADD UNIQUE INDEX uk_course_key(course_key)');
PREPARE schedule_migration_stmt FROM @schedule_migration_sql;
EXECUTE schedule_migration_stmt;
DEALLOCATE PREPARE schedule_migration_stmt;

-- 仅回填空绑定，并且仅采用唯一匹配，不覆盖人工确定的绑定。
UPDATE sys_exam e JOIN (
  SELECT BINARY board_sn AS board_sn, MIN(id) AS id FROM sys_classroom
  WHERE board_sn IS NOT NULL AND board_sn<>'' GROUP BY BINARY board_sn HAVING COUNT(*)=1
) c ON BINARY e.board_sn=c.board_sn
SET e.classroom_id=c.id WHERE e.classroom_id IS NULL;

UPDATE sys_lock_info l JOIN (
  SELECT BINARY JSH AS JSH, MIN(id) AS id FROM sys_classroom
  WHERE JSH IS NOT NULL AND JSH<>'' GROUP BY BINARY JSH HAVING COUNT(*)=1
) c ON BINARY l.JSH=c.JSH SET l.classroom_id=c.id WHERE l.classroom_id IS NULL;

UPDATE sys_lock_info SET door_channel=remarks
WHERE door_channel IS NULL AND remarks REGEXP '^[1-4]$';
UPDATE sys_schedule_task SET door_channel=remarks
WHERE door_channel IS NULL AND remarks REGEXP '^[1-4]$';

-- 只标记无新来源/关联的历史巡查，不覆盖已写入的人工或其他来源。
UPDATE sys_check_result SET leave_source='LEGACY_UNVERIFIED'
WHERE leave_source IS NULL AND course_key IS NULL AND schedule_snapshot IS NULL;

-- 完成后运行同目录 03-verify.sql；当前已核验通过的库无需重复升级。
