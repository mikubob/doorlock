/*
 Navicat Premium Dump SQL

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80028 (8.0.28)
 Source Host           : localhost:3306
 Source Schema         : soft_manage

 Target Server Type    : MySQL
 Target Server Version : 80028 (8.0.28)
 File Encoding         : 65001

 Date: 08/06/2026 14:23:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_exam
-- ----------------------------
DROP TABLE IF EXISTS `sys_exam`;
CREATE TABLE `sys_exam`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exam_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '考试号',
  `board_sn` int NULL DEFAULT NULL COMMENT 'sn码',
  `exam_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '考试内容',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片路径',
  `video_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频路径',
  `start_time` datetime NOT NULL COMMENT '考试开始时间',
  `end_time` datetime NOT NULL COMMENT '考试结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态 0=未开始 1=进行中 2=已结束',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考试信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_exam
-- ----------------------------
INSERT INTO `sys_exam` VALUES (10, NULL, 403, '111111', NULL, NULL, '2026-05-28 19:59:00', '2026-05-29 00:00:00', '2026-05-28 20:00:21', '2026-05-28 20:00:21', 0);

SET FOREIGN_KEY_CHECKS = 1;
