# 代码评审记录（Code Review）

> 实验六环节三：在 Git 仓库中模拟团队协作提交 PR，至少记录 2 次针对 AI 生成代码的审查意见。
>
> 本文件记录 **6 条**评审意见，其中 **4 条已修正并回归验证**，2 条经评估后**接受并记录理由**。
> 所有「位置」都指向真实文件与代码，可逐条核对。

## 总览

| # | 问题 | 类型 | 严重度 | 处置 |
|---|---|---|---|---|
| 1 | 查询关键字不限长，直接拼进 `LIKE` | 性能 / 健壮性 | 中 | **已修** |
| 2 | `pageSize` 越界回退成「魔法值」，与控制器不一致 | 逻辑一致性 | 中 | **已修** |
| 3 | `NoHandlerFoundException` 处理器是死代码 | 过度设计 | 低 | **已修** |
| 4 | `springdoc` 扫描包漏掉新增的 `api` 包 | 配置 | 中 | **已修** |
| 5 | 密码明文存储 + 明文比较 | 安全 | **高** | 接受，记录方案 |
| 6 | API 用 Session 鉴权，不适合对外集成 | 架构 | 中 | 接受，记录方案 |

---

## 评审 1 · 查询关键字不限长，直接拼进 LIKE

**位置**：`src/main/java/cn/edu/mju/task/controller/TaskController.java` — `list()` 方法

**AI 生成的原始代码**：

```java
String keyword = (title == null) ? "" : title.trim();
Byte statusValue = parseStatus(status);
```

**问题**：
`keyword` 最终会变成 `LIKE '%关键字%'`。不加长度限制有两个后果：
① 前置通配符本就用不上索引，关键字越长扫描成本越高；
② 超长参数会同时进入 SQL、日志与 HikariCP 的语句缓存，属于「用户可控的放大攻击面」。
用 `?title=` + 10 万个字符就能让服务端白做功。

**审查意见**：
> 筛选条件也是用户输入，不能因为「它不落库」就免于约束。建议在前置校验里截断，
> 并且**不要抛 400** —— 它是筛选条件不是业务数据，静默截断对用户更友好。

**修正后**：

```java
private static final int MAX_KEYWORD_LENGTH = 100;

String keyword = (title == null) ? "" : title.trim();
if (keyword.length() > MAX_KEYWORD_LENGTH) {
    keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
}
```

**回归验证**：`GET /task/list?title=<120 字>` 返回 200 且正常查询（不再把整串塞进 SQL）。
**风险等级**：中（不修不会崩，但可被放大）

---

## 评审 2 · `pageSize` 越界回退成「魔法值」

**位置**：`src/main/java/cn/edu/mju/task/service/impl/TaskServiceImpl.java` — `search()`

**AI 生成的原始代码**：

```java
if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
    pageSize = 10;          // ← 魔法值
}
```

**问题**：
控制器传的每页条数是 **5**，而这里越界时回退成 **10**。
行为因此不连续：正常时每页 5 条，一旦有人传了 101，就会突然变成每页 10 条。
更麻烦的是，这种「静默改变量级」的行为在测试里也容易被忽略 ——
我最初的用例只断言了「被修正」，没断言「修正成多少」，等于放过了这个不一致。

**审查意见**：
> 越界应该「夹紧到边界」，而不是「回退到另一个值」。夹紧是可预测的（101 → 100），
> 回退成 10 则需要解释「为什么是 10」。另外常量要给它一个名字。

**修正后**：

```java
private static final int DEFAULT_PAGE_SIZE = 10;   // 仅在明显非法（<1）时兜底

if (pageSize < 1) {
    pageSize = DEFAULT_PAGE_SIZE;
}
if (pageSize > MAX_PAGE_SIZE) {
    pageSize = MAX_PAGE_SIZE;      // 夹紧到上限，而不是换成 10
}
```

**回归验证**：同步把测试 `search_shouldClampPageSize` 的断言从 `10` 改成 `100`，
并重命名为「应夹紧到上限，而不是回退成别的值」——**代码与测试一起改，才算修完**。
**风险等级**：中

---

## 评审 3 · `NoHandlerFoundException` 处理器是死代码

**位置**：`src/main/java/cn/edu/mju/task/controller/GlobalExceptionHandler.java`

**AI 生成的原始代码**：

```java
@ExceptionHandler(NoHandlerFoundException.class)
public String handleNotFound(NoHandlerFoundException e, Model model, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    ...
}
```

**问题**：
这段代码**永远不会被执行**。Spring Boot 默认
`spring.mvc.throw-exception-if-no-handler-found=false`，找不到路由时不会抛这个异常，
而是转发到 `/error` 由 `BasicErrorController` 处理。
要让它生效，还必须同时把 `spring.web.resources.add-mappings` 设为 `false` ——
而那会连 swagger-ui 的静态资源一起关掉。

**审查意见**：
> 典型的「AI 过度设计」：为了覆盖一个错误页，引入了一个不会触发的分支。
> 能编译、能启动 ≠ 有用。留着它只会让后来的人以为 404 已经被接管了。

**修正**：删除该处理器与对应 import，在原位置留下注释说明为什么删。

**验证**：`GET /some-random-path` 仍返回 404（由 Boot 默认处理），
且 `/swagger-ui/index.html` 依旧可用（证明没有误关静态资源）。
**风险等级**：低（但会误导维护者）

---

## 评审 4 · `springdoc` 扫描包漏掉新增的 `api` 包

**位置**：`src/main/resources/application.yml`

**原始配置**：

```yaml
springdoc:
  packages-to-scan: cn.edu.mju.task.controller
```

**问题**：
为了「避免把 springdoc 自身端点列进去」而限定了扫描包。后来新增了
`cn.edu.mju.task.api`（对外 JSON 接口），它不在 `controller` 包下，
于是 `/v3/api-docs` 返回 **200 但 paths 为空**。
最坑的是：**不报错、不警告**，页面能开、Swagger UI 能打开，就是一条接口都没有。

**审查意见**：
> 「收紧配置」看起来是好事，但它把「新增代码自动被收录」这个便利也一起收掉了。
> 要么放开到根包，要么在 CI 里加一条「paths 数量 > 0」的断言。

**修正后**：

```yaml
springdoc:
  packages-to-scan: cn.edu.mju.task     # 覆盖 controller 与 api
```

**回归验证**：`/v3/api-docs` 长度由 403 字节变为 3137 字节，`paths` 含
`/api/tasks`、`/api/tasks/{id}`。`docs/API.md` 由该 JSON 生成。
**风险等级**：中（文档静默失效）

---

## 评审 5 · 密码明文存储 + 明文比较（接受，记录方案）

**位置**：
- `src/main/java/cn/edu/mju/task/service/impl/UserServiceImpl.java` — `login()`
- `sql/init_profiles.sql` — `t_user.login_pwd`

**AI 生成的代码**：

```java
return loginPwd.equals(user.getLoginPwd()) ? user : null;
```

数据库里存的是 `123456` 明文。

**问题**：数据库一旦泄漏，所有账号直接失守；日志/备份/测试数据里都会出现明文口令。

**审查意见**：
> 这是本次评审里唯一的**高危**项。但修正它会连带影响：初始化脚本、演示账号、
> 以及所有既有数据。作为教学演示项目，本次**接受该风险**，但必须在文档中显式声明，
> 并给出可执行的改造路径，而不是默默留着。

**为什么本次不改**：
① 改造涉及数据迁移（旧密码需要重置），会破坏「开箱即用」的演示体验；
② 本项目是本地单机演示，不对外网暴露；
③ 引入 BCrypt 需要新增依赖，属于实验范围外的变更。

**记录方案（后续迭代直接照做）**：

1. 依赖加 `spring-security-crypto`（只取 `BCryptPasswordEncoder`，不启用整套 Security）
2. 注册时 `passwordEncoder.encode(raw)` 存哈希；登录时 `matches(raw, hash)`
3. 数据迁移：`sql/init_profiles.sql` 里改成插入 BCrypt 哈希串
4. 附带补一条测试：同一明文两次 `encode` 结果不同，但 `matches` 都为 true

**风险等级**：高（已记录，不隐藏）

---

## 评审 6 · API 用 Session 鉴权，不适合对外集成（接受，记录方案）

**位置**：`src/main/java/cn/edu/mju/task/config/WebConfig.java`

**当前实现**：`/api/**` 与 `/task/**` 共用同一个 `LoginInterceptor`，
API 调用方必须先 `POST /login` 拿到 Session Cookie 才能调接口。

**审查意见**：
> 让 API 与页面共用 Session 是「能跑但不对」的典型：调用方要模拟浏览器行为，
> 而且没有过期控制、不能按调用方撤销。不过反过来，如果直接把 `/api/**` 放行，
> 任务标题就会匿名可读 —— 那更糟。
>
> 两害相权，本次选择「**要求登录**」这个更安全的一侧，
> 并在 `docs/API.md` 里写明「这是设计局限，正式项目应改 Token」。

**记录方案**：

1. 新增 `Authorization: Bearer <token>` 的鉴权过滤器（或在现有拦截器里加分支）
2. 登录接口改为返回 JWT，不再依赖 Session
3. 为写接口补幂等键与审计日志（当前 API 只读，暂无此需求）

**风险等级**：中

---

## 小结：人工评审到底拦住了什么

这 6 条里，有 **4 条是「代码能跑、测试也绿」但依然有问题**的：

- 关键字不限长 → 能跑，但可被放大
- `pageSize` 回退魔法值 → 能跑，行为不连续
- 死代码异常处理器 → 能跑，因为从不执行
- 扫描包漏配 → 能跑，只是文档空着

这恰好说明一件事：**测试验证的是「我想到的场景」，评审验证的是「我没想到的场景」**。
两者不是替代关系。第 5、6 条更极端 —— 它们连「有测试」都没用，
因为测试写的是「密码对了就登录成功」，天然不会去质疑「密码该不该明文存」。

所以「AI 提效比」再高，也需要一个环节是**人主动去怀疑**。
本项目的做法是：把怀疑的结果落成文件（本文件），而不是留在口头。
