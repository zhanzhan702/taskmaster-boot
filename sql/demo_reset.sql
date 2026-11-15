-- =====================================================================
-- 演示数据重置脚本
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 < sql/demo_reset.sql
--
-- 目的：把库恢复到一个「每个演示点都有数据支撑」的确定状态。
-- 演示点与数据的对应关系：
--   共 12 条 + 每页 5 条        → 分页控件能展示完整形态（3 页）
--   状态 待办5 / 进行中4 / 已完成3 → 状态筛选每一项都有结果
--   标题含「报告」恰好 2 条       → 模糊查询能命中且不铺满整页
--   id=1 待办 / id=2 进行中 / id=3 已完成 → 三种状态流转各有入口
--   最后一条标题 250 字           → 边界演示（≤255 通过，+1 字被拒）
-- =====================================================================

USE taskmaster;

-- 重置任务表（自增 id 也从 1 开始，保证 id 与演示脚本一致）
TRUNCATE TABLE t_task;

INSERT INTO t_task (title, status, priority, due_date, create_date) VALUES
 ('完成环境搭建',            0, 3, '2026-11-20', '2026-11-16'),
 ('跑通第一条请求链路',      1, 3, '2026-11-21', '2026-11-16'),
 ('整理痛点清单',            2, 1, '2026-11-18', '2026-11-15'),
 ('复习 Servlet 生命周期',   0, 2, '2026-11-22', '2026-11-15'),
 ('完成实验报告',            0, 3, '2026-11-25', '2026-11-15'),
 ('复习反射机制',            1, 2, '2026-11-23', '2026-11-14'),
 ('整理笔记',                2, 1, '2026-11-19', '2026-11-14'),
 ('撰写项目报告',            0, 2, '2026-11-28', '2026-11-13'),
 ('学习 MyBatis-Plus',       1, 3, '2026-11-26', '2026-11-13'),
 ('整理课程笔记',            2, 1, '2026-11-17', '2026-11-12'),
 ('准备答辩材料',            0, 3, '2026-11-30', '2026-11-12');

-- 第 12 条：250 个字的长标题，用于演示「刚好通过 / 超 1 字被拒」的边界
INSERT INTO t_task (title, status, priority, due_date, create_date) VALUES
 (REPEAT('测', 250), 1, 1, '2026-11-29', '2026-11-11');

-- 重置用户表（演示账号固定）
TRUNCATE TABLE t_user;
INSERT INTO t_user (login_name, login_pwd, nickname) VALUES
 ('admin',    '123456', '管理员'),
 ('zhangsan', '123456', '张三');

-- 自检：把关键分布打出来，便于确认重置成功
SELECT '总条数'          AS metric, COUNT(*)            AS value FROM t_task
UNION ALL SELECT '待办(0)',        COUNT(*) FROM t_task WHERE status = 0
UNION ALL SELECT '进行中(1)',      COUNT(*) FROM t_task WHERE status = 1
UNION ALL SELECT '已完成(2)',      COUNT(*) FROM t_task WHERE status = 2
UNION ALL SELECT '标题含「报告」',  COUNT(*) FROM t_task WHERE title LIKE '%报告%'
UNION ALL SELECT '最长标题字数',    MAX(CHAR_LENGTH(title)) FROM t_task;
