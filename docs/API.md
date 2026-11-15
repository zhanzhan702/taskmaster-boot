# TaskMaster API 技术手册

> 本文档由 `springdoc-openapi` 生成：启动应用后读取 `/v3/api-docs` 的 OpenAPI 3.0 描述，
> 再由脚本渲染为 Markdown。**不是手写的**——接口的入参、出参、示例均来自代码注解，
> 因此代码改动后重新生成即可保持同步。

| 项 | 值 |
| --- | --- |
| 标题 | TaskMaster 任务管理系统 API |
| 版本 | 1.1.0 |
| 联系人 | 傅展鹏 2837420590@qq.com |
| OpenAPI 版本 | 3.0.1 |
| 在线调试 | `/swagger-ui/index.html` |
| 原始描述 | `/v3/api-docs` |

## 一、认证方式

所有 `/api/**` 与 `/task/**` 接口都要求登录（`LoginInterceptor` 拦截）。

```bash
# 1) 先登录拿到会话 Cookie
curl -c cookie.txt -X POST http://localhost:8084/login \
     -d 'loginName=admin&loginPwd=123456'

# 2) 携带 Cookie 调用 API
curl -b cookie.txt 'http://localhost:8084/api/tasks?page=1&size=5'
```

> 未登录直接访问会得到 `302` 跳转到 `/login`。
> 这也暴露了一个设计局限：**基于 Session 的认证不适合做对外 API**（调用方得先学会抓 Cookie），
> 正式项目应改为 Token（JWT / OAuth2）认证。

## 二、通用错误码

JSON 接口统一返回 `ApiResponse`，`code` 为业务错误码：

| code | 含义 | 对应 HTTP | 触发场景 |
| --- | --- | --- | --- |
| 0 | 成功 | 200 | 正常返回 |
| 400 | 参数不合法 | 400 | 业务校验失败（`ServiceException`） |
| 404 | 资源不存在 | 404 | 按 id 查询查不到 |
| 500 | 服务器内部错误 | 500 | 未预期异常（堆栈只进日志） |

页面接口（非 `/api/**`）失败时返回的是 HTML 错误页，不是本表结构。

## 三、JSON 接口

### GET `/api/tasks`

- **分组**：任务查询 API
- **说明**：分页查询任务
- **详细**：支持按任务名称模糊、按状态精确筛选。页码越界会自动回退到最后一页；size 超过 100 会被夹紧到 100。

**入参**

| 参数 | 位置 | 必填 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `title` | query | 否 | string | 任务名称关键字，模糊匹配；超过 100 字符会被截断 | - |
| `status` | query | 否 | string | 任务状态：0 待办 / 1 进行中 / 2 已完成；不传表示全部 | - |
| `page` | query | 否 | string | 页码，从 1 开始；非法值按 1 处理 | - |
| `size` | query | 否 | string | 每页条数，默认 5，上限 100 | - |

**出参**

| HTTP | 说明 | 响应体 |
| --- | --- | --- |
| 200 | 查询成功 | `ApiResponse` |
| 400 | 参数不合法（page 非数字、status 越界等） | `ApiResponse` |

### GET `/api/tasks/{id}`

- **分组**：任务查询 API
- **说明**：按编号查询任务
- **详细**：任务不存在时返回 HTTP 404 + code=404

**入参**

| 参数 | 位置 | 必填 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `id` | path | 是 | integer(int32) | 任务编号 | - |

**出参**

| HTTP | 说明 | 响应体 |
| --- | --- | --- |
| 200 | 查询成功 | `ApiResponseTaskDto` |
| 404 | 任务不存在 | `ApiResponse` |

## 四、数据模型

### ApiResponse

统一响应体

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `code` | integer(int32) | 错误码：0 表示成功 | 0 |
| `message` | string | 提示信息 | ok |
| `data` | object | 业务数据，失败时为 null | - |

### ApiResponseTaskDto

统一响应体

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `code` | integer(int32) | 错误码：0 表示成功 | 0 |
| `message` | string | 提示信息 | ok |
| `data` | TaskDto | - | - |

### TaskDto

任务

| 字段 | 类型 | 说明 | 示例 |
| --- | --- | --- | --- |
| `id` | integer(int32) | 任务编号 | 1 |
| `title` | string | 任务名称 | 完成实验报告 |
| `status` | integer(int32) | 状态码：0 待办 / 1 进行中 / 2 已完成 | 0 |
| `statusLabel` | string | 状态中文名 | 待办 |
| `priority` | integer(int32) | 优先级：1 低 / 2 中 / 3 高 | 3 |
| `priorityLabel` | string | 优先级中文名 | 高 |
| `dueDate` | string | 截止日期 yyyy-MM-dd | 2026-12-01 |
| `createDate` | string | 创建日期 yyyy-MM-dd | 2026-11-09 |

## 五、页面路由（服务端渲染）

以下路由返回 HTML 视图，不属于 JSON API，故不被 OpenAPI 收录；
此表由脚本扫描控制器源码的 `@RequestMapping` / `@GetMapping` 生成。

| 路由 | 方法 | 控制器 |
| --- | --- | --- |
| `/login` | GET | AuthController |
| `/login` | POST | AuthController |
| `/logout` | GET | AuthController |
| `/task/add` | GET | TaskController |
| `/task/add` | POST | TaskController |
| `/task/delete/{id}` | POST | TaskController |
| `/task/edit/{id}` | GET | TaskController |
| `/task/edit/{id}` | POST | TaskController |
| `/task/list` | GET | TaskController |
| `/task/status/{id}` | POST | TaskController |

> 说明：`springdoc` 只收录**有响应体**的操作。`@Controller` 返回视图名时没有响应体，
> 因而不会出现在 `/v3/api-docs` 里 —— 这是本次接入 springdoc 时踩到的第一个坑。
