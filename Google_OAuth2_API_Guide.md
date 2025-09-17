# Google OAuth2 API 调用指南

## 项目概述
这是一个Spring Boot应用，集成了Google OAuth2登录功能。用户可以通过Google账户登录系统。

## API端点说明

### 1. 登录相关端点

#### 获取登录信息
```
GET /api/auth/login
```
返回登录页面信息和Google登录URL。

#### Google OAuth2 登录
```
GET /oauth2/authorization/google
```
重定向到Google登录页面，用户完成认证后会自动回调到系统。

#### Google 登录回调
```
GET /api/auth/google/callback
```
Google认证成功后的回调端点，处理用户信息并返回登录结果。

#### 获取当前用户信息
```
GET /api/auth/user
```
获取已登录用户的详细信息。

#### 登出
```
POST /api/auth/logout
```
用户登出系统。

### 2. 网络诊断端点

#### 网络连接测试
```
GET /api/auth/network/test
```
测试网络连接状态，包括DNS解析和Google API连接测试。

## VPN环境下的问题解决方案

### 问题现象
在VPN环境下，可能出现以下错误：
```
[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: I/O error on POST request for "https://oauth2.googleapis.com/token": Connection timed out: connect
```

### 解决步骤

#### 1. 网络连接诊断
首先访问网络测试端点来诊断问题：
```bash
curl http://localhost:8080/api/auth/network/test
```

#### 2. 启动参数优化
在启动应用时添加以下JVM参数：
```bash
java -jar amor-auth.jar \
  -Djava.net.preferIPv4Stack=true \
  -Dnetworking.timeout.connection=30000 \
  -Dnetworking.timeout.read=30000 \
  -Dhttps.protocols=TLSv1.2,TLSv1.3
```

#### 3. 代理配置（如果需要）
如果你的VPN需要HTTP代理，添加代理参数：
```bash
java -jar amor-auth.jar \
  -Dhttp.proxyHost=your-proxy-host \
  -Dhttp.proxyPort=your-proxy-port \
  -Dhttps.proxyHost=your-proxy-host \
  -Dhttps.proxyPort=your-proxy-port
```

#### 4. DNS配置优化
如果DNS解析有问题，可以：
1. 修改系统DNS服务器（如使用8.8.8.8, 1.1.1.1）
2. 在hosts文件中添加Google API的IP映射

#### 5. 防火墙和安全软件
确保防火墙允许访问以下域名：
- oauth2.googleapis.com
- accounts.google.com
- www.googleapis.com

## 使用流程

### 1. 启动登录流程
```bash
# 1. 获取登录信息
curl http://localhost:8080/api/auth/login

# 2. 在浏览器中访问Google登录URL
# 用户在浏览器中访问: http://localhost:8080/oauth2/authorization/google
```

### 2. 用户认证流程
1. 用户点击Google登录链接
2. 重定向到Google授权页面
3. 用户授权后，Google重定向回系统回调地址
4. 系统处理用户信息并返回登录结果

### 3. 获取用户信息
```bash
# 登录成功后，可以获取用户信息
curl http://localhost:8080/api/auth/user \
  -H "Cookie: JSESSIONID=your-session-id"
```

## 配置说明

### application.properties 关键配置
```properties
# Google OAuth2配置
spring.security.oauth2.client.registration.google.client-id=your-client-id
spring.security.oauth2.client.registration.google.client-secret=your-client-secret
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/api/auth/google/callback

# 网络超时配置
spring.security.oauth2.client.http-client.connect-timeout=30000
spring.security.oauth2.client.http-client.read-timeout=30000

# 详细日志配置
logging.level.org.springframework.security.oauth2=DEBUG
```

## 故障排除

### 常见错误及解决方法

1. **连接超时**
   - 检查网络连接
   - 增加超时时间
   - 检查DNS解析

2. **循环重定向**
   - 已在SecurityConfig中修复
   - 避免在successHandler中重定向到回调URL

3. **OAuth2认证失败**
   - 检查client-id和client-secret
   - 确认回调URL配置正确
   - 查看详细错误日志

### 日志分析
应用会输出详细的OAuth2日志，包括：
- DNS解析结果
- 网络连接状态
- OAuth2认证过程
- 用户信息处理结果

## 测试命令

```bash
# 1. 测试应用启动
curl http://localhost:8080/api/auth/login

# 2. 测试网络连接
curl http://localhost:8080/api/auth/network/test

# 3. 测试Google登录（在浏览器中）
http://localhost:8080/oauth2/authorization/google

# 4. 查看日志
tail -f logs/application.log
```

## 安全注意事项

1. **客户端密钥安全**：不要在公开代码中暴露client-secret
2. **HTTPS使用**：生产环境应使用HTTPS
3. **会话管理**：合理配置会话超时时间
4. **日志安全**：生产环境应减少敏感信息的日志输出
