# Google OAuth2 登录登出 API 接口文档

## 概述
本文档描述了Google OAuth2登录系统的前端调用接口，包括登录、登出、Session管理等功能。

## 基础信息
- 服务器地址: `http://localhost:8080`
- 所有接口返回JSON格式数据
- 错误时HTTP状态码为4xx或5xx

## 1. 登录相关接口

### 1.1 获取登录信息
```
GET /api/auth/login
```
**响应示例:**
```json
{
  "message": "请通过Google账户登录",
  "googleLoginUrl": "/oauth2/authorization/google"
}
```

### 1.2 开始Google OAuth2登录
```
GET /oauth2/authorization/google
```
**说明:** 
- 前端重定向到此URL开始Google登录流程
- 用户完成Google授权后会自动回调到callback接口

### 1.3 Google登录回调处理
```
GET /api/auth/google/callback
```
**响应示例:**
```json
{
  "success": true,
  "message": "登录成功",
  "user": {
    "id": 1,
    "email": "user@gmail.com",
    "name": "User Name",
    "googleId": "12345678",
    "picture": "https://lh3.googleusercontent.com/...",
    "createdAt": "2025-01-01T00:00:00"
  },
  "redirectUrl": "/dashboard"
}
```

### 1.4 获取当前用户信息
```
GET /api/auth/user
```
**响应示例:**
```json
{
  "user": {
    "id": 1,
    "email": "user@gmail.com",
    "name": "User Name",
    "googleId": "12345678",
    "picture": "https://lh3.googleusercontent.com/..."
  },
  "authenticated": true
}
```

### 1.5 获取登录状态
```
GET /api/auth/status
```
**响应示例:**
```json
{
  "authenticated": true,
  "user": {
    "email": "user@gmail.com",
    "name": "User Name",
    "sub": "12345678"
  },
  "hasSession": true,
  "sessionId": "ABC123..."
}
```

## 2. 登出相关接口

### 2.1 标准登出 ⭐ 推荐
```
POST /api/auth/logout
```
**说明:** 清理用户Session和安全上下文
**响应示例:**
```json
{
  "success": true,
  "message": "登出成功",
  "user": "user@gmail.com",
  "timestamp": 1642857600000
}
```

### 2.2 强制登出（清理所有Session）
```
POST /api/auth/force-logout
```
**说明:** 清理所有Redis中的Session数据，用于解决Session异常
**响应示例:**
```json
{
  "success": true,
  "message": "强制登出成功，所有Session已清理",
  "timestamp": 1642857600000
}
```

## 3. Session管理接口

### 3.1 清理当前Session
```
POST /api/session/logout
```
**响应示例:**
```json
{
  "success": true,
  "message": "Session已清理",
  "sessionId": "ABC123..."
}
```

### 3.2 清理Redis中的所有Session
```
POST /api/session/redis/clear/sessions
```
**说明:** 用于解决"authorization_request_not_found"错误
**响应示例:**
```json
{
  "success": true,
  "message": "Redis Session数据清理完成",
  "clearedCount": 15,
  "timestamp": 1642857600000
}
```

### 3.3 清理OAuth2缓存
```
POST /api/session/redis/clear/oauth2
```
**响应示例:**
```json
{
  "success": true,
  "message": "OAuth2缓存清理完成",
  "clearedCount": 8,
  "timestamp": 1642857600000
}
```

### 3.4 获取Redis缓存统计
```
GET /api/session/redis/stats
```
**响应示例:**
```json
{
  "success": true,
  "patternStats": {
    "spring:session:*": 5,
    "oauth2:*": 3,
    "auth:*": 2,
    "user:*": 1,
    "google:*": 0
  },
  "totalKeys": 11,
  "categorizedKeys": 11,
  "otherKeys": 0,
  "timestamp": 1642857600000
}
```

## 4. 调试和网络测试接口

### 4.1 网络连接测试
```
GET /api/auth/network/test
```
**说明:** 测试与Google API的网络连接
**响应示例:**
```json
{
  "success": true,
  "dns": {
    "oauth2.googleapis.com": "142.250.191.106",
    "accounts.google.com": "142.250.191.84",
    "status": "success"
  },
  "googleApi": {
    "oauth2.googleapis.com": {
      "responseCode": 405,
      "responseTime": "234ms",
      "status": "success"
    }
  },
  "system": {
    "httpProxy": "not set",
    "httpsProxy": "not set",
    "activeNetworkInterfaces": ["eth0 (以太网)"]
  },
  "timestamp": 1642857600000
}
```

### 4.2 OAuth2配置调试
```
GET /api/auth/oauth2/debug
```
**响应示例:**
```json
{
  "success": true,
  "oauth2Config": {
    "clientId": "13566225304-0dol5ppkktl1vpehsenii5cqp2avuktt.apps.googleusercontent.com",
    "redirectUri": "http://localhost:8080/api/auth/google/callback",
    "scope": "profile,email",
    "authorizationUri": "https://accounts.google.com/o/oauth2/auth",
    "tokenUri": "https://oauth2.googleapis.com/token"
  },
  "manualAuthUrl": "https://accounts.google.com/o/oauth2/auth?...",
  "instructions": {
    "step1": "访问上面的manualAuthUrl进行授权",
    "step2": "授权后会重定向到callback，从URL中获取code参数",
    "step3": "使用code调用/api/auth/manual-token-exchange端点来手动交换token"
  }
}
```

## 5. 前端调用示例

### 5.1 JavaScript/React 登录示例
```javascript
// 开始登录
const handleLogin = () => {
  window.location.href = 'http://localhost:8080/oauth2/authorization/google';
};

// 检查登录状态
const checkAuthStatus = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/status', {
      credentials: 'include' // 重要：包含Session Cookie
    });
    const data = await response.json();
    return data.authenticated;
  } catch (error) {
    console.error('检查登录状态失败:', error);
    return false;
  }
};

// 登出
const handleLogout = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      credentials: 'include' // 重要：包含Session Cookie
    });
    const data = await response.json();
    if (data.success) {
      console.log('登出成功');
      // 重定向到登录页面
      window.location.href = '/login';
    }
  } catch (error) {
    console.error('登出失败:', error);
  }
};
```

### 5.2 解决Session问题的示例
```javascript
// 当遇到 "authorization_request_not_found" 错误时
const clearSessionAndRetry = async () => {
  try {
    // 清理Redis Session
    await fetch('http://localhost:8080/api/session/redis/clear/sessions', {
      method: 'POST'
    });
    
    // 强制登出
    await fetch('http://localhost:8080/api/auth/force-logout', {
      method: 'POST'
    });
    
    // 重新开始登录流程
    setTimeout(() => {
      window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    }, 1000);
    
  } catch (error) {
    console.error('清理Session失败:', error);
  }
};
```

## 6. 常见错误处理

### 6.1 authorization_request_not_found
**原因:** Session在OAuth2流程中丢失
**解决方案:** 
1. 调用 `POST /api/session/redis/clear/sessions` 清理Session
2. 调用 `POST /api/auth/force-logout` 强制登出
3. 重新开始登录流程

### 6.2 Connection timed out
**原因:** 网络连接问题，可能需要VPN
**解决方案:**
1. 调用 `GET /api/auth/network/test` 测试网络
2. 检查VPN连接
3. 使用手动token交换接口

### 6.3 invalid_id_token
**原因:** JWT token验证失败
**解决方案:**
1. 调用 `POST /api/session/redis/clear/oauth2` 清理OAuth2缓存
2. 重新登录

## 7. 重要注意事项

1. **Cookie和Session:** 所有需要认证的接口都必须在请求中包含 `credentials: 'include'`
2. **CORS配置:** 确保前端域名在CORS白名单中
3. **HTTPS:** 生产环境必须使用HTTPS
4. **错误重试:** 遇到Session相关错误时，优先尝试清理缓存再重新登录
5. **网络环境:** 如果在中国大陆，可能需要VPN访问Google API

## 8. 测试流程

1. 访问 `GET /api/auth/network/test` 确认网络连接
2. 访问 `GET /api/session/redis/stats` 查看缓存状态
3. 调用 `POST /api/session/redis/clear/sessions` 清理旧Session
4. 开始登录流程 `GET /oauth2/authorization/google`
5. 登录成功后测试 `GET /api/auth/user`
6. 测试登出 `POST /api/auth/logout`
