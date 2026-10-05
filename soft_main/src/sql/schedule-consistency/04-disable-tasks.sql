-- 应用回滚前停止所有排程写入，保留新增列、审计及课程调整记录。
-- 不把 VARCHAR SN 强制转回整数；字母/前导零会丢失。
-- 不删除本地课程、不恢复已发送设备动作。恢复备份须同时恢复所有排程表。
-- 仅关闭定时任务，保留数据供核对；已发生的物理动作不能回滚。
UPDATE sys_schedule_task SET task_status=0 WHERE task_status=1;
