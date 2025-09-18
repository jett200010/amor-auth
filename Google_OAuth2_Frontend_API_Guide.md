# Google OAuth2 登录 - 前端调用指南

## API 接口概览

基础URL: `http://localhost:8080`

### 1. 获取Google登录URL
**接口**: `GET /api/google/login-url`
**描述**: 获取Google OAuth2登录链接
**返回**:
```json
{
  "success": true,
  "loginUrl": "/oauth2/authorization/google",
  "message": "请使用此URL进行Google登录",
  "clientId": "your-google-client-id"
}
```

### 2. 直接跳转到Google登录
**接口**: `GET /api/google/login`
**描述**: 直接重定向到Google登录页面

### 3. Google登录回调处理
**接口**: `GET /api/google/callback`
**描述**: Google OAuth2回调地址（配置在Google Console中）
**返回**:
```json
{
  "success": true,
  "message": "Google登录成功",
  "user": {
    "id": 1,
    "email": "user@gmail.com",
    "name": "用户名",
    "picture": "头像URL",
    "googleId": "google-user-id"
  },
  "token": "jwt_token_for_user_1"
}
```

### 4. 获取当前用户信息
**接口**: `GET /api/google/user-info`
**描述**: 获取当前登录用户的详细信息
**返回**:
```json
{
  "success": true,
  "user": {
    "id": 1,
    "email": "user@gmail.com",
    "name": "用户名",
    "picture": "头像URL",
    "googleId": "google-user-id",
    "createdAt": "2023-01-01T00:00:00",
    "lastLoginAt": "2023-01-01T00:00:00"
  }
}
```

### 5. 检查登录状态
**接口**: `GET /api/google/status`
**描述**: 检查用户是否已登录
**返回**:
```json
{
  "isLoggedIn": true,
  "email": "user@gmail.com",
  "name": "用户名"
}
```

### 6. 登出
**接口**: `POST /api/google/logout`
**描述**: 用户登出
**返回**:
```json
{
  "success": true,
  "message": "登出成功"
}
```

## 前端实现示例

### JavaScript/TypeScript 实现

```javascript
class GoogleAuthService {
  constructor() {
    this.baseURL = 'http://localhost:8080';
  }

  // 方法1: 直接跳转到Google登录
  async redirectToGoogleLogin() {
    window.location.href = `${this.baseURL}/api/google/login`;
  }

  // 方法2: 获取登录URL后自定义处理
  async getGoogleLoginUrl() {
    try {
      const response = await fetch(`${this.baseURL}/api/google/login-url`);
      const data = await response.json();
      
      if (data.success) {
        // 可以在新窗口打开或当前页面跳转
        window.location.href = `${this.baseURL}${data.loginUrl}`;
        // 或者在新窗口打开: window.open(`${this.baseURL}${data.loginUrl}`, '_blank');
      }
    } catch (error) {
      console.error('获取Google登录URL失败:', error);
    }
  }

  // 检查登录状态
  async checkLoginStatus() {
    try {
      const response = await fetch(`${this.baseURL}/api/google/status`, {
        credentials: 'include' // 包含cookies
      });
      const data = await response.json();
      return data.isLoggedIn;
    } catch (error) {
      console.error('检查登录状态失败:', error);
      return false;
    }
  }

  // 获取用户信息
  async getUserInfo() {
    try {
      const response = await fetch(`${this.baseURL}/api/google/user-info`, {
        credentials: 'include'
      });
      const data = await response.json();
      
      if (data.success) {
        return data.user;
      } else {
        throw new Error(data.message);
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
      return null;
    }
  }

  // 登出
  async logout() {
    try {
      const response = await fetch(`${this.baseURL}/api/google/logout`, {
        method: 'POST',
        credentials: 'include'
      });
      const data = await response.json();
      
      if (data.success) {
        // 清除本地存储的用户信息
        localStorage.removeItem('user');
        // 刷新页面或重定向到登录页
        window.location.reload();
      }
    } catch (error) {
      console.error('登出失败:', error);
    }
  }
}

// 使用示例
const authService = new GoogleAuthService();

// 登录按钮点击事件
document.getElementById('googleLoginBtn').addEventListener('click', () => {
  authService.redirectToGoogleLogin();
});

// 页面加载时检查登录状态
window.addEventListener('load', async () => {
  const isLoggedIn = await authService.checkLoginStatus();
  
  if (isLoggedIn) {
    const user = await authService.getUserInfo();
    if (user) {
      // 显示用户信息
      console.log('当前用户:', user);
      // 更新UI显示用户已登录状态
    }
  } else {
    // 显示登录按钮
    console.log('用户未登录');
  }
});
```

### React 组件示例

```jsx
import React, { useState, useEffect } from 'react';

const GoogleLogin = () => {
  const [user, setUser] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const handleGoogleLogin = () => {
    // 直接跳转到Google登录
    window.location.href = 'http://localhost:8080/api/google/login';
  };

  const handleLogout = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/google/logout', {
        method: 'POST',
        credentials: 'include'
      });
      const data = await response.json();
      
      if (data.success) {
        setUser(null);
        setIsLoggedIn(false);
      }
    } catch (error) {
      console.error('登出失败:', error);
    }
  };

  const checkLoginStatus = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/google/status', {
        credentials: 'include'
      });
      const data = await response.json();
      
      if (data.isLoggedIn) {
        setIsLoggedIn(true);
        // 获取详细用户信息
        const userResponse = await fetch('http://localhost:8080/api/google/user-info', {
          credentials: 'include'
        });
        const userData = await userResponse.json();
        if (userData.success) {
          setUser(userData.user);
        }
      }
    } catch (error) {
      console.error('检查登录状态失败:', error);
    }
  };

  useEffect(() => {
    checkLoginStatus();
  }, []);

  return (
    <div>
      {isLoggedIn ? (
        <div>
          <h3>欢迎, {user?.name}!</h3>
          <img src={user?.picture} alt="头像" width="50" height="50" />
          <p>邮箱: {user?.email}</p>
          <button onClick={handleLogout}>登出</button>
        </div>
      ) : (
        <div>
          <h3>请登录</h3>
          <button onClick={handleGoogleLogin}>
            使用Google账户登录
          </button>
        </div>
      )}
    </div>
  );
};

export default GoogleLogin;
```

### Vue.js 组件示例

```vue
<template>
  <div>
    <div v-if="isLoggedIn">
      <h3>欢迎, {{ user.name }}!</h3>
      <img :src="user.picture" alt="头像" width="50" height="50" />
      <p>邮箱: {{ user.email }}</p>
      <button @click="logout">登出</button>
    </div>
    <div v-else>
      <h3>请登录</h3>
      <button @click="loginWithGoogle">使用Google账户登录</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GoogleLogin',
  data() {
    return {
      user: null,
      isLoggedIn: false
    }
  },
  async mounted() {
    await this.checkLoginStatus();
  },
  methods: {
    loginWithGoogle() {
      window.location.href = 'http://localhost:8080/api/google/login';
    },
    
    async logout() {
      try {
        const response = await fetch('http://localhost:8080/api/google/logout', {
          method: 'POST',
          credentials: 'include'
        });
        const data = await response.json();
        
        if (data.success) {
          this.user = null;
          this.isLoggedIn = false;
        }
      } catch (error) {
        console.error('登出失败:', error);
      }
    },
    
    async checkLoginStatus() {
      try {
        const response = await fetch('http://localhost:8080/api/google/status', {
          credentials: 'include'
        });
        const data = await response.json();
        
        if (data.isLoggedIn) {
          this.isLoggedIn = true;
          const userResponse = await fetch('http://localhost:8080/api/google/user-info', {
            credentials: 'include'
          });
          const userData = await userResponse.json();
          if (userData.success) {
            this.user = userData.user;
          }
        }
      } catch (error) {
        console.error('检查登录状态失败:', error);
      }
    }
  }
}
</script>
```

## 重要配置说明

### 1. Google OAuth2 配置
确保在Google Cloud Console中配置了正确的重定向URI：
```
http://localhost:8080/api/auth/google/callback
```

### 2. CORS 配置
如果前端和后端在不同端口，确保处理跨域问题。后端已添加 `@CrossOrigin(origins = "*")`。

### 3. Cookie/Session 配置
前端请求时需要包含 `credentials: 'include'` 以传递session cookies。

### 4. 错误处理
所有API都返回统一格式的错误响应：
```json
{
  "success": false,
  "message": "错误描述"
}
```

## 部署注意事项

1. **生产环境配置**: 修改 `application.properties` 中的 `redirect-uri` 为生产环境域名
2. **HTTPS**: 生产环境建议使用HTTPS
3. **跨域配置**: 根据实际前端域名配置CORS
4. **安全性**: 可以集成JWT token进行更安全的认证

这套API提供了完整的Google OAuth2登录流程，前端可以根据需要选择合适的调用方式。
