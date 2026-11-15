# TaskMaster

> 一个极简任务管理系统，也是一条完整的 Java Web 技术演进样本：
> 从原生 Servlet + JDBC 一路走到 SpringBoot + Thymeleaf + MyBatis-Plus，并配齐 CI 与自动化文档。

![Java](https://img.shields.io/badge/Java-8-blue?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?logo=springboot&logoColor=white)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.3.2-1E90FF)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.0.15-005F0F)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Tests](https://img.shields.io/badge/tests-71%20passed-brightgreen)
![Coverage](https://img.shields.io/badge/Service%20coverage-100%25-brightgreen)
[![CI](https://github.com/zhanzhan702/taskmaster-boot/actions/workflows/ci.yml/badge.svg)](https://github.com/zhanzhan702/taskmaster-boot/actions/workflows/ci.yml)

---

## 一、这个项目是什么

TaskMaster 是一个「任务清单」：可以新增、修改、删除任务，按名称模糊与状态筛选，分页浏览，
并让任务在 **待办 → 进行中 → 已完成** 之间流转。

它的真正价值不在功能，而在于**同一套业务被实现了四遍**，每遍针对上一遍的痛点换一次技术：

| 阶段 | 技术形态 | 当时的痛点 |
|---|---|---|
| ① 原生 | Servlet + JSP + JDBC | 硬编码、无分层、SQL 与页面混杂 |
| ② SSM | Spring + Spring MVC + MyBatis（全 XML） | 三套 XML 手工装配，异常层层上抛 |
| ③ Boot + JSP | SpringBoot + MyBatis（注解配置） | 仍是 war，因为 JSP 进不了 jar |
| ④ 当前 | **SpringBoot + Thymeleaf + MyBatis-Plus** | 配置最少、可 `java -jar` 一键部署 |

> 三次演进的过程与量化数据见 `docs/` 下的各实验报告；本仓库的 Git tag
> `stage3-class01`（JSP 版）、`stage4-class01`（当前版）可以直接对照。

---

## 二、架构

```
                       ┌──────────────────────────────┐
                       │        浏览器 / API 客户端      │
                       └───────────────┬──────────────┘
                                       │ HTTP
                                       ▼
        ┌──────────────────────────────────────────────────────────┐
        │  LoginInterceptor（/task/**、/api/** 需要登录）            │
        └──────────────────────────┬───────────────────────────────┘
                                   │
          ┌────────────────────────┴─────────────────────────┐
          ▼                                                  ▼
┌───────────────────────┐                        ┌───────────────────────┐
│  页面接口 @Controller  │                        │  JSON 接口 @RestController │
│  TaskController        │                        │  TaskApiController     │
│  AuthController        │                        │  → /api/tasks          │
│  → 返回 Thymeleaf 视图  │                        │  → ApiResponse<T>      │
└───────────┬───────────┘                        └───────────┬───────────┘
            └────────────────────┬───────────────────────────┘
                                 ▼
                    ┌────────────────────────┐
                    │   Service 层            │
                    │   TaskService / UserService │
                    │   业务校验 · 状态机 · @Transactional │
                    └───────────┬────────────┘
                                ▼
                    ┌────────────────────────┐
                    │   Mapper 层（MyBatis-Plus） │
                    │   TaskMapper extends BaseMapper │
                    └───────────┬────────────┘
                                ▼
                    ┌────────────────────────┐
                    │   MySQL  · taskmaster   │
                    │   t_task / t_user       │
                    └────────────────────────┘
```

**分层职责**（一句话版）：

- **Controller**：取参数、调 Service、选视图 / 拼响应 —— 不写 SQL、不写业务规则
- **Service**：业务校验、状态流转判定、事务边界 —— 不出现 `HttpServletRequest`
- **Mapper**：对象 ↔ SQL —— 不做业务判断
- **依赖方向只能向下**，所以第 ③ 步把持久层从 MyBatis 换成 MyBatis-Plus 时，业务代码一行未改

---

## 三、技术栈

| 分类 | 选型 | 说明 |
|---|---|---|
| 语言 | Java 8 | 字节码 target 1.8，实测 JDK 8 与 17 均可构建运行 |
| 框架 | Spring Boot 2.7.18 | 对应 Spring 5.3 + `javax` 命名空间 |
| 视图 | Thymeleaf 3.0.15 | 纯 HTML 模板，可静态预览、可打进 jar |
| 持久层 | MyBatis-Plus 3.5.3.2 | `BaseMapper` + `LambdaQueryWrapper` + 分页插件 |
| 数据库 | MySQL 8.0 | `t_task` / `t_user` |
| 连接池 | HikariCP | Spring Boot 默认 |
| 校验 | Hibernate Validator | `@Valid` + `@NotBlank/@Size/@NotNull/@Min/@Max` |
| 文档 | springdoc-openapi 1.6.15 | 运行时生成 `/v3/api-docs` + Swagger UI |
| 测试 | JUnit 5 + Mockito + AssertJ | 71 个单元测试，不依赖数据库 |
| 覆盖率 | JaCoCo 0.8.11 | Service 层行/分支 100% |
| 构建 | Maven | 可执行 jar |

---

## 四、环境依赖

| 依赖 | 版本要求 | 必装 |
|---|---|---|
| JDK | **8**（或 11 / 17） | 是 |
| MySQL | 8.0+ | 是 |
| Maven | 3.6+ | 仅构建时需要 |
| Tomcat | **不需要** | 内嵌 |

---

## 五、一键启动

```bash
# 1) 建库建表（会创建 dev / test / prod 三套库，dev 库带 12 条演示数据）
mysql -uroot -p --default-character-set=utf8mb4 < sql/init_profiles.sql

# 2) 改一下数据库账号密码
#    src/main/resources/application-dev.yml 里的 spring.datasource.username / password

# 3) 构建
mvn clean package

# 4) 启动（默认 dev 环境，端口 8084）
java -jar target/taskmaster-plus.jar
```

打开 <http://localhost:8084/login>，用 **admin / 123456** 登录。

### 换环境 / 换端口

```bash
# 测试环境
java -jar target/taskmaster-plus.jar --spring.profiles.active=test

# 生产环境 + 自定义端口
java -jar target/taskmaster-plus.jar --spring.profiles.active=prod --server.port=9090

# 临时覆盖数据库地址（不重新打包）
java -jar target/taskmaster-plus.jar \
  --spring.datasource.url='jdbc:mysql://192.168.1.10:3306/taskmaster?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
```

### 只跑测试（不需要数据库）

```bash
mvn clean test
# 覆盖率报告：target/site/jacoco/index.html
```

### 看接口文档

启动后访问：

- Swagger UI：<http://localhost:8084/swagger-ui/index.html>
- OpenAPI JSON：<http://localhost:8084/v3/api-docs>
- 离线 Markdown 手册：[`docs/API.md`](docs/API.md)

---

## 六、核心特性

### 1. 登录与访问控制

- Session 保存登录用户，`LoginInterceptor` 拦截 `/task/**` 与 `/api/**`
- 未登录访问任意受保护地址 → 302 跳登录页，登录后**跳回原地址**
- API 文档路径（`/v3/api-docs`、`/swagger-ui/**`）显式放行

### 2. 条件查询 + 分页

- 任务名称**模糊**匹配、状态**精确**匹配，条件为空时自动不参与 SQL
- 每页 5 条，页码窗口显示当前页前后各 2 页
- **翻页保留筛选条件**（链接自动带上 `title` / `status`）
- 页码越界（比如删光了最后一页）自动回退到最后一页，不会出现空白页

### 3. 状态流转（状态机）

| 操作 | 允许的当前状态 | 结果 |
|---|---|---|
| `start` | 待办(0) | 进行中(1) |
| `complete` | 进行中(1) | 已完成(2) |
| `reopen` | 已完成(2) | 进行中(1) |

页面上只显示当前合法的那一个按钮；Service 层用 `@Transactional` 再校验一次
（「读状态 → 判合法 → 改状态」三步必须原子）。

### 4. 服务端校验

- 表单对象 `TaskForm` 上打 `@NotBlank/@Size/@NotNull/@Min/@Max`
- 模板用 `th:errors` **逐字段**回显，错误提示就在对应输入框下方
- 客户端 `required`/`maxlength` 只是便利，**能绕过**；真正生效的是服务端这道

### 5. 统一异常处理

| 异常 | HTTP | 输出 |
|---|---|---|
| `ServiceException` | 400 | 页面：`error.html`；API：`ApiResponse{code:400}` |
| `HttpRequestMethodNotSupportedException` | 405 | 页面：`error.html` |
| 其他 `Exception` | 500 | 堆栈只进日志，页面只显示通用提示 |

页面与 API **各有一套 advice**，否则 API 调用方会收到一段 HTML。

### 6. 可执行 JAR

- `java -jar` 直接跑，目标机器只需 JDK 8，不需要 Tomcat、不需要源码
- 内含内嵌 Tomcat 9 与全部依赖（约 32 MB）
- 环境差异全部由启动参数注入，同一份 jar 适配 dev/test/prod

### 7. 质量保障

- **71 个单元测试**，纯 Mockito（`@Mock`/`@InjectMocks`），不连数据库
- **Service 层行与分支覆盖率 100%**（JaCoCo）
- **GitHub Actions CI**：push / PR 触发，JDK **8 与 17 双矩阵**跑 `mvn clean test`，
  通过后才构建 jar 并归档 Artifact

---

## 七、目录结构

```
taskmaster-boot/
├── .github/workflows/ci.yml         CI 流水线
├── docs/
│   ├── API.md                       接口手册（由 springdoc 生成）
│   ├── CODE-REVIEW.md               代码评审记录
│   └── DEMO.md                      演示脚本
├── sql/init_profiles.sql            三套环境库建库脚本
├── src/main/java/cn/edu/mju/task/
│   ├── TaskMasterApplication.java   主启动类
│   ├── api/                         JSON 接口层（TaskApiController / DTO / 统一响应）
│   ├── config/                      WebConfig / MybatisPlusConfig / OpenApiConfig / TaskProperties
│   ├── controller/                  页面接口（TaskController / AuthController）+ 统一异常处理
│   ├── entity/  mapper/  service/   三层主体
│   ├── form/                        TaskForm（@Valid 表单对象）
│   ├── interceptor/                 LoginInterceptor
│   └── util/                        WebUtil
├── src/main/resources/
│   ├── application.yml              公共配置
│   ├── application-{dev,test,prod}.yml
│   └── templates/                   Thymeleaf 模板（含 fragments 片段复用）
└── src/test/java/...                71 个单元测试
```

---

## 八、常见问题

| 现象 | 原因与解决 |
|---|---|
| 启动报 `Table 'taskmaster.user' doesn't exist` | 表名映射错误，确认 `mybatis-plus.global-config.db-config.table-prefix: t_` |
| 启动报 `Could not resolve type alias 'Task'` | `target` 里有上一次构建残留的 mapper XML，执行 `mvn clean package` |
| 分页不生效、total 恒为 0 | 忘了注册 `PaginationInnerInterceptor`（不报错，只是静默退化） |
| `/v3/api-docs` 返回 200 但 `paths` 为空 | `springdoc.packages-to-scan` 没覆盖到控制器所在包 |
| 访问 swagger-ui 被跳到登录页 | `LoginInterceptor` 没放行 `/swagger-ui/**` |
| 页面 500 且响应是 JSON | Thymeleaf 渲染阶段抛异常，看日志里 `TemplateInputException` 的表达式 |

---

## 九、许可

教学演示项目，仅用于课程学习与答辩演示。

---

**仓库**：<https://github.com/zhanzhan702/taskmaster-boot>
**CI**：<https://github.com/zhanzhan702/taskmaster-boot/actions/workflows/ci.yml>
**Tag**：`stage5-class01`（实验六交付版）
