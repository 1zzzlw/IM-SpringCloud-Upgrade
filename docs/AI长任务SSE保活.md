# AI 长任务 SSE 保活与代理配置

## 修复依据

Electron 开发模式的 `/api` 由 Vite 转发至 `127.0.0.1:81`，打包后也通过 81 端口访问 HTTP 接口。工作区 Nginx 的普通 HTTP 代理使用 `proxy_read_timeout 30s`。

imAgent 在规划、模型思考、等待 Electron 工具结果时，可能连续超过 30 秒没有发送业务事件。代理会因读取空闲关闭连接，Starlette 随后取消响应任务，LangGraph 中表现为 `CancelledError`。该异常只能证明运行被取消，单独的堆栈不能证明实际部署环境中一定由哪一层先断开；此次修复的是源码已确认的空闲超时隐患。

工作区 `nginx-1.28.2/logs/spring_error.log` 也存在对应证据：2026-11-02 14:59:08、15:00:02 的 `POST /ai-message/sendMessage` 记录为 `upstream timed out ... while reading upstream`。说明本机请求链路确实发生过代理读取超时，而不只是配置层面的推测。尚未把这些记录与用户提供的每条 Python 取消堆栈逐一关联。

## 已修改内容

- `imAgent/app/api/sse.py`：业务事件由一个持续任务消费，通过最多 32 条的队列交给响应流；连接建立时立即发送注释，空闲每 10 秒发送 `: keep-alive\n\n`。
- 心跳超时只结束本次等待，不取消模型、不重启任务。业务事件按顺序交付，末尾 completed/done 不丢失。
- `imAgent/app/api/ai_message.py`：使用保活流，由 `StreamingResponse` 统一监听客户端断开，取消时记录会话及经过的秒数。
- `imAgent/app/services/ai_message_service.py`：显式关闭嵌套异步生成器；响应被取消时取消并等待业务任务清理，避免后台继续运行。
- `nginx-1.28.2/conf/nginx.conf`：只给 `/ai-message/sendMessage` 增加精确匹配的 SSE location，读取和发送超时 300 秒，关闭缓冲、缓存和压缩，不自动重试有文件修改副作用的任务；客户端断开时通知后端清理。

这里的 300 秒是两次读取之间允许的最大空闲时间，不是 Agent 总执行时长。常规心跳每 10 秒刷新数据。普通 HTTP 接口、WS 心跳、模型配额以及单个命令/工具的执行时限不在这次修改范围内。

SSE 心跳是协议注释，Electron 现有解析器已经会忽略，不需要修改前端事件类型。参考：[SSE 注释保活](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format)、[Nginx 读取超时](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_read_timeout)。

## 生效步骤

1. 更新并重启 imAgent 的 9090 业务服务。
2. 将工作区 Nginx 配置中的 AI location 同步到客户端实际访问的部署机器。如果该机器已有其他专用 AI location，应在原位置应用这些指令，避免添加重复 location。
3. 在那台机器上检查 Nginx 配置，再重新加载。Windows 工作区示例：

```powershell
Set-Location E:\JavaProject\IM-SpringCloud\nginx-1.28.2
.\nginx.exe -t
.\nginx.exe -s reload
```

本次没有修改运行中的远程配置，也没有重新加载任何 Nginx 服务。Spring Cloud Gateway 当前没有额外配置该路由的响应超时，未新增无关网关配置。如果部署环境另设固定总时长限制，仍需在相应层检查。

## 验证范围

按用户此前安排，没有运行功能测试、构建、服务或模型请求。4 个相关 Python 文件的 AST 语法检查和 Git 差异检查通过。已添加 `imAgent/tests/test_sse_keepalive.py`，覆盖空闲心跳不取消模型、队列顺序及 done 事件、AnyIO 断开取消时同任务清理、业务异常传播；测试留给用户执行：

```powershell
Set-Location E:\AgentProject\imAgent
uv run python -m unittest discover -s tests -p test_sse_keepalive.py -v
```

实际联调时发起会长时间思考或执行工具的 Agent 请求，在开发者工具 Network 中查看 SSE：空闲仍应每 10 秒有注释数据，超过原来的 30 秒后继续执行；点击停止则仍会取消并清理。可结合 imAgent 的取消耗时日志，以及 Nginx 的 upstream timed out 日志判断是否还有其他断开来源。
