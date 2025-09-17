# Google OAuth2 网络连接问题排查指南

## 问题描述
在VPN环境下，Google OAuth2回调时出现连接超时错误：
```
[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: I/O error on POST request for "https://oauth2.googleapis.com/token": Connection timed out: connect
```

## 问题分析
1. **DNS解析问题**: VPN可能影响对Google服务器的DNS解析
2. **网络超时**: 默认的连接超时设置过短
3. **代理配置**: 可能需要配置HTTP代理
4. **防火墙/安全软件**: 可能阻止了对Google API的访问

## 解决方案

### 1. 网络连接测试
```bash
# 测试DNS解析
nslookup oauth2.googleapis.com
nslookup accounts.google.com

# 测试连接
curl -I https://oauth2.googleapis.com/token
curl -I https://accounts.google.com
```

### 2. JVM网络参数配置
在启动应用时添加以下JVM参数：
```bash
-Djava.net.preferIPv4Stack=true
-Dnetworking.timeout.connection=30000
-Dnetworking.timeout.read=30000
-Dhttps.protocols=TLSv1.2,TLSv1.3
```

### 3. 系统代理配置（如果使用HTTP代理）
```bash
-Dhttp.proxyHost=your-proxy-host
-Dhttp.proxyPort=your-proxy-port
-Dhttps.proxyHost=your-proxy-host
-Dhttps.proxyPort=your-proxy-port
```

### 4. DNS配置
如果DNS解析有问题，可以在hosts文件中添加：
```
172.217.24.74 oauth2.googleapis.com
142.250.185.84 accounts.google.com
```

### 5. 应用配置优化
- 增加HTTP客户端超时时间
- 添加重试机制
- 增加详细的网络日志

## 测试步骤
1. 检查网络连接
2. 验证DNS解析
3. 测试Google API端点
4. 查看应用日志
5. 逐步调整超时参数

## 常见错误及解决方法
- **Connection timed out**: 增加超时时间
- **UnknownHostException**: 检查DNS配置
- **SSLHandshakeException**: 检查SSL/TLS配置
- **ConnectException**: 检查代理和防火墙设置
