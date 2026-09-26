/*
 Navicat Premium Data Transfer

 Source Server         : long
 Source Server Type    : MySQL
 Source Server Version : 80017
 Source Host           : 192.168.1.214:3306
 Source Schema         : soft_manage

 Target Server Type    : MySQL
 Target Server Version : 80017
 File Encoding         : 65001

 Date: 29/09/2025 09:21:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_check_result
-- ----------------------------
DROP TABLE IF EXISTS `sys_check_result`;
CREATE TABLE `sys_check_result`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL COMMENT '上课周次 正常的格式是2-4 表示2到4周上课\r\n多个用逗号隔开\r\n如果只有一个数字，表示只有当周',
  `weeks` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '周次 比如第8周',
  `section` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节次 比如第8节',
  `classroom` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '教室的名称 比如1-308物联网实验室',
  `classes` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '班级名称',
  `should_arrival` int(11) DEFAULT NULL COMMENT '应到人数',
  `arrival` int(11) DEFAULT NULL COMMENT '实到人数',
  `arrival_rate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '出勤率',
  `discipline` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '课堂纪律 如三人玩游戏这种',
  `food_bring_person` int(11) DEFAULT 0 COMMENT '带食物进教室的人数',
  `food_bring_rate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '带食物的比率',
  `is_late` int(11) DEFAULT NULL COMMENT '0表示否 1表示是',
  `is_violate` int(11) DEFAULT NULL COMMENT '是否违反八不准',
  `is_normal` int(11) DEFAULT NULL COMMENT '0表示否 1表示是',
  `counsellor` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '辅导员\r\n',
  `teacher` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '老师名称',
  `check_person` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '巡查人',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `is_consist` int(11) DEFAULT 1 COMMENT '0表示否，1表示一致',
  `is_stand` int(11) DEFAULT 1 COMMENT '0表示否，1表示是站着上课',
  `college` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '学院',
  `people_leave` int(11) DEFAULT NULL COMMENT '请假人数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1279 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_construct
-- ----------------------------
DROP TABLE IF EXISTS `sys_construct`;
CREATE TABLE `sys_construct`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `p_id` int(11) DEFAULT 0 COMMENT '项目父ID',
  `u_id` int(11) DEFAULT 0 COMMENT '创建人id',
  `title_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '项目名称',
  `is_task` tinyint(1) DEFAULT 0 COMMENT '是否为任务',
  `task_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '任务名称',
  `target` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '2' COMMENT '目标值',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int(11) DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '项目收集表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_construct_result
-- ----------------------------
DROP TABLE IF EXISTS `sys_construct_result`;
CREATE TABLE `sys_construct_result`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `con_id` int(11) NOT NULL COMMENT '收集ID',
  `result_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '成果名',
  `result_path` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '佐证材料',
  `status` int(11) DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '项目收集结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_construct_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_construct_user`;
CREATE TABLE `sys_construct_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `con_id` int(11) NOT NULL COMMENT '项目id',
  `u_id` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `con_id`(`con_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_course
-- ----------------------------
DROP TABLE IF EXISTS `sys_course`;
CREATE TABLE `sys_course`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `week` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '星期',
  `weeks` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '周次',
  `section` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节次',
  `classroom` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '教室名称',
  `classes` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '上课班级',
  `should_arrival` int(11) NOT NULL COMMENT '应到人数',
  `counsellor` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '辅导员\r\n',
  `teacher` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任课老师',
  `course` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '课程名称',
  `state` int(11) DEFAULT 1 COMMENT '是否是上学期的课表 0表示是上学期的 1表示是刚刚新增的即这学期的',
  `college` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学院',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_flow
-- ----------------------------
DROP TABLE IF EXISTS `sys_flow`;
CREATE TABLE `sys_flow`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '流程唯一ID',
  `flow_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '流程名称',
  `p_id` int(11) NOT NULL COMMENT '项目ID',
  `describe` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '流程描述',
  `user_id` int(11) NOT NULL COMMENT '用户id',
  `create_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` int(11) DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '流程表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_flow_task
-- ----------------------------
DROP TABLE IF EXISTS `sys_flow_task`;
CREATE TABLE `sys_flow_task`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '流程任务ID',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '任务序号（第几步）',
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '任务类型',
  `u_id` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '用户Ids',
  `role_id` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '角色IDS',
  `start_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
  `parent_id` int(11) DEFAULT NULL COMMENT '父ID',
  `status` int(11) DEFAULT 1 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 183 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_info
-- ----------------------------
DROP TABLE IF EXISTS `sys_info`;
CREATE TABLE `sys_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一ID',
  `p_id` int(11) NOT NULL COMMENT '项目ID',
  `info_key` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '以[\'姓名\',\'年龄\']这种json数据格式存储',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_lock_info
-- ----------------------------
DROP TABLE IF EXISTS `sys_lock_info`;
CREATE TABLE `sys_lock_info`  (
  `lock_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '智能锁唯一标识符',
  `ip_address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '智能锁IP地址',
  `sn_code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '智能锁SN序列号',
  `port_number` int(11) DEFAULT NULL COMMENT '智能锁通信端口号',
  `switch_status` int(11) DEFAULT NULL COMMENT '当前开关状态（0-关 1-开）',
  `college` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '智能锁所属学院',
  `classroom` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '智能锁所属教室',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '备注信息',
  PRIMARY KEY (`lock_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22212 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储智能锁设备详细信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_id` int(11) DEFAULT NULL,
  `menu_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `icon` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `path` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `component` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `type` tinyint(1) DEFAULT NULL,
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  `status` int(11) NOT NULL,
  `sort` int(11) DEFAULT NULL,
  `is_nav` tinyint(1) DEFAULT NULL COMMENT '是否显示在左侧菜单',
  PRIMARY KEY (`menu_id`) USING BTREE,
  INDEX `menu_URL`(`path`) USING BTREE,
  INDEX `component`(`component`) USING BTREE,
  INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 98 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `create_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  `status` int(11) DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '内容',
  `create_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_count` int(11) NOT NULL DEFAULT 10 COMMENT '浏览量',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `is_top` int(11) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 831 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `switch_id` int(255) NOT NULL AUTO_INCREMENT COMMENT '开关记录唯一标识符',
  `lock_id` int(255) DEFAULT NULL COMMENT '关联的智能锁ID',
  `user_id` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '操作用户ID',
  `operation_method` int(11) DEFAULT NULL COMMENT '操作方式（0-关 1-开）',
  `operation_time` datetime(0) DEFAULT NULL COMMENT '操作时间（精确到年月日时分秒）',
  PRIMARY KEY (`switch_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1631 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '记录智能锁开关操作历史日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_project
-- ----------------------------
DROP TABLE IF EXISTS `sys_project`;
CREATE TABLE `sys_project`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `title` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '标题',
  `describe` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '描述',
  `send_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '发布者',
  `create_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '创建人',
  `start_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
  `create_time` timestamp(0) DEFAULT NULL COMMENT '创建时间',
  `is_collect` tinyint(1) DEFAULT 0 COMMENT '是否需要收集信息',
  `status` tinyint(1) DEFAULT 0 COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '项目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_project_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_project_item`;
CREATE TABLE `sys_project_item`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目ID',
  `pname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '名称:可以是分类的名称，也可以是子任务的名称',
  `parent_id` int(11) DEFAULT NULL COMMENT '父id。分类的parentId为0，子任务的parentId为分类的id',
  `grade` int(11) DEFAULT NULL COMMENT '层级的意思：分类的层级为1 ， 子任务的层级为2',
  `standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '评分标准，只有子任务才有',
  `score` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最高分数',
  `is_file` tinyint(1) DEFAULT 0 COMMENT '是否需要佐证材料',
  `is_extend` tinyint(1) DEFAULT NULL COMMENT '是否需要扩展项',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `order_by` int(11) DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_result
-- ----------------------------
DROP TABLE IF EXISTS `sys_result`;
CREATE TABLE `sys_result`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一ID',
  `u_id` int(11) NOT NULL COMMENT '用户唯一编号',
  `p_id` int(11) NOT NULL COMMENT '项目ID',
  `task_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '任务ID',
  `score` int(11) DEFAULT NULL COMMENT '评分分数',
  `evidence` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '佐证材料 格式：[{任务ID:材料路径}]',
  `evidence_count` int(11) DEFAULT NULL COMMENT '佐证材料数量',
  `step` int(11) DEFAULT 0 COMMENT '步骤',
  `create_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time` timestamp(0) DEFAULT NULL COMMENT '修改时间',
  `is_finish` int(11) NOT NULL DEFAULT 0 COMMENT '是否完成答题',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16456 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_result_extend
-- ----------------------------
DROP TABLE IF EXISTS `sys_result_extend`;
CREATE TABLE `sys_result_extend`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一ID',
  `u_id` int(11) DEFAULT NULL COMMENT '用户id',
  `task_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务ID',
  `result_id` int(11) DEFAULT NULL COMMENT '结果ID',
  `item_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展项类别：1：国家级、2：省级、3：校级、4：其他',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展项名称',
  `create_time` datetime(0) DEFAULT NULL COMMENT '发布时间',
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展项备注',
  `score` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展项评分',
  `evidence` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '佐证材料 格式：[{任务ID:材料路径}]',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12971 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_result_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_result_item`;
CREATE TABLE `sys_result_item`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一ID',
  `u_id` int(11) NOT NULL COMMENT '用户id',
  `p_id` int(11) DEFAULT NULL COMMENT '项目id',
  `step` int(11) NOT NULL COMMENT '步骤',
  `score` int(11) DEFAULT NULL COMMENT '评分',
  `create_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `user_id` int(11) NOT NULL COMMENT '审批人id',
  `opinion` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '打回意见',
  `is_flag` int(11) NOT NULL DEFAULT 0 COMMENT '是否打回',
  `status` int(11) DEFAULT 1 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 580 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '结果表子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` int(11) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `role_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `role_desc` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP,
  `role_status` tinyint(1) DEFAULT NULL,
  `weight` tinyint(4) DEFAULT 1,
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE INDEX `role_name`(`role_name`) USING BTREE,
  UNIQUE INDEX `role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_id` int(11) NOT NULL,
  `menu_id` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2735 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_schedule_task
-- ----------------------------
DROP TABLE IF EXISTS `sys_schedule_task`;
CREATE TABLE `sys_schedule_task`  (
  `task_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键自增',
  `lock_id` int(11) DEFAULT 0 COMMENT '锁ID，用于分布式锁',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `timed_operation` int(11) DEFAULT 0 COMMENT '定时操作类型',
  `task_details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '任务详情',
  `task_status` int(11) DEFAULT 0 COMMENT '任务状态（0-待执行，1-执行中，2-已完成，3-已取消）',
  `is_loop` int(11) DEFAULT 0 COMMENT '是否循环任务（0-否，1-是）',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `created_time` datetime(0) DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `hour` int(11) DEFAULT NULL COMMENT '小时（0-23）',
  `minute` int(11) DEFAULT NULL COMMENT '分钟（0-59）',
  `updated_time` datetime(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `count_day` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计数日期（如：2024-01-01）',
  `loop_count` int(255) DEFAULT NULL COMMENT '循环次数',
  PRIMARY KEY (`task_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_task_status`(`task_status`) USING BTREE,
  INDEX `idx_created_time`(`created_time`) USING BTREE,
  INDEX `idx_lock_id`(`lock_id`) USING BTREE,
  INDEX `idx_count_day`(`count_day`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 243 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统定时任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_task
-- ----------------------------
DROP TABLE IF EXISTS `sys_task`;
CREATE TABLE `sys_task`  (
  `id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '唯一ID',
  `p_id` int(11) NOT NULL COMMENT '项目ID',
  `category` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '类别',
  `task_name` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '任务标题',
  `standard` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '评分标准内容',
  `score` int(11) DEFAULT NULL COMMENT '最高分数值',
  `is_file` tinyint(1) DEFAULT 0 COMMENT '是否需要佐证材料',
  `is_extend` tinyint(1) DEFAULT NULL COMMENT '是否需要扩展项',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_task_back
-- ----------------------------
DROP TABLE IF EXISTS `sys_task_back`;
CREATE TABLE `sys_task_back`  (
  `id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '唯一ID',
  `p_id` int(11) NOT NULL COMMENT '项目ID',
  `category` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '类别',
  `task_name` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '任务标题',
  `standard` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '评分标准内容',
  `score` int(11) DEFAULT NULL COMMENT '最高分数值',
  `is_file` tinyint(1) DEFAULT 0 COMMENT '是否需要佐证材料',
  `is_extend` tinyint(1) DEFAULT NULL COMMENT '是否需要扩展项',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户Id',
  `avatar` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '头像',
  `nick_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '姓名',
  `user_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '工号',
  `password` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '密码',
  `email` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '邮箱号',
  `phone` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '手机号',
  `sex` enum('男','女') CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '男' COMMENT '性别',
  `major` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '所学专业',
  `create_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `entry_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入职时间',
  `last_login` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态',
  `college` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '学院',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 231 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `role_id` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 913 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
