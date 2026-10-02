-- T-04 第一阶段。维护窗口停写、备份并核对真实 DDL 后人工执行一次；MySQL 8 / InnoDB。
-- 不删除、不按名称推断历史来源、不改变任何 task.id。不由应用自动执行。
ALTER TABLE sys_task
  ADD COLUMN source_project_item_id INT NULL COMMENT '经核验的来源子项ID；其他来源/未知来源为空',
  ADD UNIQUE KEY uk_task_project_source (p_id, source_project_item_id);

CREATE TABLE sys_project_import_state (
  project_id INT NOT NULL,
  legacy_reviewed TINYINT NOT NULL DEFAULT 0 COMMENT '历史来源及旧Redis暂存全部核验后才设为1',
  first_staged_at DATETIME NULL COMMENT '首次暂存冻结标记，永久保留',
  review_reference VARCHAR(255) NULL COMMENT '受控审核报告编号；新项目为created-by-t04',
  PRIMARY KEY (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 所有历史项目默认关闭任务变更，包括没有任务的项目；旧缓存核验不能省略。
INSERT INTO sys_project_import_state(project_id,legacy_reviewed)
SELECT id,0 FROM sys_project;

-- 只读核验，输出交给项目负责人及数据管理员审核。
SELECT p.id,p.status,s.legacy_reviewed,s.first_staged_at,
       COUNT(t.id) AS tasks, SUM(t.source_project_item_id IS NULL AND t.id IS NOT NULL) AS unknown_sources
FROM sys_project p JOIN sys_project_import_state s ON s.project_id=p.id
LEFT JOIN sys_task t ON t.p_id=p.id GROUP BY p.id,p.status,s.legacy_reviewed,s.first_staged_at;

SELECT r.id,r.p_id,r.task_id,t.p_id AS task_project
FROM sys_result r LEFT JOIN sys_task t ON t.id=r.task_id
WHERE t.id IS NULL OR t.p_id<>r.p_id;

SELECT e.id,e.task_id,e.result_id,t.p_id AS task_project,r.p_id AS result_project
FROM sys_result_extend e LEFT JOIN sys_task t ON t.id=e.task_id LEFT JOIN sys_result r ON r.id=e.result_id
WHERE t.id IS NULL OR r.id IS NULL OR t.p_id<>r.p_id OR e.task_id<>r.task_id OR e.u_id<>r.u_id;

-- 核验旧Redis键的JSON归属并回填 first_staged_at；审核来源/异常/缓存均完成后，
-- 在独立受控事务中回填可信 source_project_item_id 和逐项目 legacy_reviewed/review_reference。
-- 本脚本故意不提供自动开放或自动历史映射 SQL。
