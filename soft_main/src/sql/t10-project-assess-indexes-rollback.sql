/**
 * T10索引独立回退脚本
 * 仅回退本次实际创建的索引，执行前核对名称和其他查询依赖。
 * 应用代码回滚不要求立即删除这些非唯一索引。
 */
ALTER TABLE sys_result DROP INDEX idx_t10_result_project_user;
ALTER TABLE sys_user_role DROP INDEX idx_t10_user_role_role_user;
ALTER TABLE sys_user_role DROP INDEX idx_t10_user_role_user_role;
