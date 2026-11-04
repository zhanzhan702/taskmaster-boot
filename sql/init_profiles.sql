-- =====================================================================
-- 实验四 · 多环境数据库初始化
--   taskmaster        开发库（dev，与实验二/三共用）
--   taskmaster_test   测试库（test）
--   taskmaster_prod   生产库（prod）
--
-- 三个库结构完全相同，但数据不同 —— 这样切换 profile 后能"肉眼看出"
-- 连的确实是另一个库，而不是同一份数据换了层皮。
--
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 < sql/init_profiles.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 开发库
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS taskmaster DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE taskmaster;

DROP TABLE IF EXISTS t_task;
CREATE TABLE t_task (
  id          INT           NOT NULL AUTO_INCREMENT,
  title       VARCHAR(255)  NOT NULL,
  status      TINYINT       NOT NULL DEFAULT 0,
  priority    TINYINT       NOT NULL DEFAULT 1,
  due_date    DATE          NULL,
  create_date DATE          NULL DEFAULT (CURRENT_DATE),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
  id         INT          NOT NULL AUTO_INCREMENT,
  login_name VARCHAR(50)  NOT NULL,
  login_pwd  VARCHAR(100) NOT NULL,
  nickname   VARCHAR(50)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_login_name (login_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_task (title, status, priority, due_date, create_date) VALUES
 ('完成环境搭建',        0, 3, '2026-10-28', '2026-10-27'),
 ('跑通第一条请求链路',  1, 3, '2026-10-29', '2026-10-27'),
 ('整理痛点清单',        0, 2, '2026-11-02', '2026-10-27'),
 ('复习 Servlet 生命周期', 2, 1, '2026-10-26', '2026-10-25');

INSERT INTO t_user (login_name, login_pwd, nickname) VALUES
 ('admin', '123456', '管理员'),
 ('zhangsan', '123456', '张三');

-- ---------------------------------------------------------------------
-- 测试库：数据与开发库不同，便于验证 profile 切换生效
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS taskmaster_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE taskmaster_test;

DROP TABLE IF EXISTS t_task;
CREATE TABLE t_task LIKE taskmaster.t_task;

DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user LIKE taskmaster.t_user;

INSERT INTO t_task (title, status, priority, due_date, create_date) VALUES
 ('【测试库】用例覆盖检查', 0, 3, '2026-11-10', '2026-11-04'),
 ('【测试库】回归测试',     1, 2, '2026-11-12', '2026-11-04'),
 ('【测试库】缺陷复现',     2, 1, '2026-11-08', '2026-11-04');

INSERT INTO t_user (login_name, login_pwd, nickname) SELECT login_name, login_pwd, nickname FROM taskmaster.t_user;

-- ---------------------------------------------------------------------
-- 生产库
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS taskmaster_prod DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE taskmaster_prod;

DROP TABLE IF EXISTS t_task;
CREATE TABLE t_task LIKE taskmaster.t_task;

DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user LIKE taskmaster.t_user;

INSERT INTO t_task (title, status, priority, due_date, create_date) VALUES
 ('【生产库】版本发布',     0, 3, '2026-12-01', '2026-11-04'),
 ('【生产库】线上巡检',     1, 2, '2026-11-30', '2026-11-04');

INSERT INTO t_user (login_name, login_pwd, nickname) SELECT login_name, login_pwd, nickname FROM taskmaster.t_user;

SELECT 'taskmaster' AS db, COUNT(*) AS tasks FROM taskmaster.t_task
UNION ALL SELECT 'taskmaster_test', COUNT(*) FROM taskmaster_test.t_task
UNION ALL SELECT 'taskmaster_prod', COUNT(*) FROM taskmaster_prod.t_task;
