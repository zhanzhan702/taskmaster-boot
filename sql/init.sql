-- =====================================================================
-- TaskMaster 原生版 · 建库建表（环节一 + 环节二合并版）
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 < sql/init.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS taskmaster DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE taskmaster;

-- ---------------------------------------------------------------------
-- 环节一：任务表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_task;

CREATE TABLE t_task (
  id          INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(255)  NOT NULL               COMMENT '任务名称',
  status      TINYINT       NOT NULL DEFAULT 0     COMMENT '0待办 1进行中 2已完成',
  priority    TINYINT       NOT NULL DEFAULT 1     COMMENT '1低 2中 3高',
  due_date    DATE          NULL                   COMMENT '截止日期',
  create_date DATE          NULL DEFAULT (CURRENT_DATE) COMMENT '创建日期',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

INSERT INTO t_task (title, status, priority, due_date, create_date) VALUES
 ('完成环境搭建',        0, 3, '2026-10-28', '2026-10-27'),
 ('跑通第一条请求链路',  1, 3, '2026-10-29', '2026-10-27'),
 ('整理痛点清单',        0, 2, '2026-11-02', '2026-10-27'),
 ('复习 Servlet 生命周期', 2, 1, '2026-10-26', '2026-10-25');

-- ---------------------------------------------------------------------
-- 环节二：用户表（登录验证用）
-- 注意：密码以明文存储，这是刻意的简化，已列入痛点清单（安全性）。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_user;

CREATE TABLE t_user (
  id         INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  login_name VARCHAR(50)  NOT NULL               COMMENT '登录名',
  login_pwd  VARCHAR(100) NOT NULL               COMMENT '登录密码（明文，仅用于教学演示）',
  nickname   VARCHAR(50)  NULL                   COMMENT '显示名称',
  PRIMARY KEY (id),
  UNIQUE KEY uk_login_name (login_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

INSERT INTO t_user (login_name, login_pwd, nickname) VALUES
 ('admin', '123456', '管理员'),
 ('zhangsan', '123456', '张三');
