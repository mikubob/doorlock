/**
 * T10项目考核列表的非唯一辅助索引
 * 发布前使用SHOW INDEX核对同前缀索引，已存在等价索引时不要重复执行。
 * 只包含查询索引，不清理重复角色、不新增业务表、不调整业务数据。
 * 在预发核对执行计划、DDL锁等待及写入开销后单独发布。
 */
ALTER TABLE sys_result ADD INDEX idx_t10_result_project_user (p_id, u_id, id);
ALTER TABLE sys_user_role ADD INDEX idx_t10_user_role_role_user (role_id, user_id);
ALTER TABLE sys_user_role ADD INDEX idx_t10_user_role_user_role (user_id, role_id);
