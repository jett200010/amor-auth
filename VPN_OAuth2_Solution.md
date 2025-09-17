# VPN环境下Google OAuth2连接超时解决方案

## 问题分析

根据日志分析，你的OAuth2流程实际上是正常工作的：

1. ✅ **授权阶段正常**：用户成功重定向到Google，获得授权码
2. ✅ **参数传递正确**：Spring Security发送的请求格式完全正确
3. ❌ **网络连接超时**：在调用`https://oauth2.googleapis.com/token`时连接超时

这是VPN环境下Java应用特有的网络问题，不是代码问题。

## 解决方案

我已经更新了`OAuth2HttpClientConfig`配置，新增以下功能：

### 1. 自动代理检测
- 自动检测系统代理设置
- 扫描常见的本地代理端口（7890, 1080, 8080, 10809）
- 优先使用HTTPS代理，回退到HTTP代理

### 2. 增加超时时间
- 连接超时：60秒（原30秒）
- 读取超时：60秒（原30秒）

### 3. 详细日志记录
- 记录代理使用情况
- 记录所有HTTP请求/响应详情

## 使用方法

### 方法1：启动时指定代理（推荐）
如果你知道VPN的代理端口，启动时指定：
```bash
java -jar amor-auth.jar \
  -Dhttps.proxyHost=127.0.0.1 \
  -Dhttps.proxyPort=7890 \
  -Dhttp.proxyHost=127.0.0.1 \
  -Dhttp.proxyPort=7890
```

### 方法2：使用Maven启动
```bash
mvn spring-boot:run -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890
```

### 方法3：自动检测（已配置）
应用会自动尝试连接常见的代理端口，无需手动配置。

## 常见VPN软件代理端口

- **Clash**: 7890
- **V2Ray**: 10809
- **Shadowsocks**: 1080
- **其他**: 8080

## 测试步骤

1. **重新启动应用**
2. **查看启动日志**，确认代理配置
3. **测试OAuth2流程**：访问 `http://localhost:8080/oauth2/authorization/google`
4. **查看详细日志**，确认网络请求成功

## 如果仍然超时

1. **检查VPN状态**：确保VPN正常工作
2. **确认代理端口**：查看VPN软件的代理设置
3. **手动指定代理**：使用方法1手动指定正确的代理端口
4. **检查防火墙**：确保Java进程可以访问网络

## 预期结果

配置成功后，你应该看到类似的日志：
```
Auto-detected and using local proxy: 127.0.0.1:7890
OAuth2 HTTP Request: POST https://oauth2.googleapis.com/token
OAuth2 HTTP Response: 200 OK
Successfully loaded OAuth2 user: user@example.com
```
