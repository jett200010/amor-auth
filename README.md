# Amor Auth - Google OAuth2 认证服务

![Amor Auth Logo](https://img.shields.io/badge/Amor-Auth-blue?style=for-the-badge&logo=google&logoColor=white)

一个基于Spring Boot的现代化Google OAuth2.0身份验证服务，提供安全、快速的用户认证解决方案。

## 🚀 项目特性

- 🔐 **安全认证** - 基于Google OAuth2.0的安全认证机制
- ⚡ **快速登录** - 一键登录，无需繁琐的注册流程
- 🔄 **自动同步** - 自动同步用户信息，支持Redis缓存和数据库持久化
- 📊 **登录追踪** - 完整的用户行为日志记录
- 🎨 **现代化UI** - 响应式设计的登录页面
- 🛡️ **安全防护** - 智能IP获取，多层安全防护
- 📱 **移动友好** - 完全响应式设计，支持各种设备

## 🛠️ 技术栈

### 后端技术
- **Spring Boot 3.4.9** - 核心框架
- **Spring Security** - 安全框架
- **Spring OAuth2 Client** - OAuth2客户端
- **MyBatis 3.0.5** - 数据持久化
- **Redis** - 缓存系统
- **MySQL** - 数据库

### 前端技术
- **HTML5** - 页面结构
- **CSS3** - 样式设计
- **Thymeleaf** - 模板引擎（仅login.html页面）

### 工具与依赖
- **Lombok** - 简化Java代码
- **Jackson** - JSON序列化
- **Maven** - 项目管理

## 📋 系统要求

- **Java 17+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Maven 3.6+**

## 🔧 快速开始

### 1. 环境准备

确保以下服务已安装并正在运行：

```bash
# 检查Java版本
java -version

# 启动MySQL服务
net start mysql

# 启动Redis服务
redis-server
```

### 2. 克隆项目

```bash
git clone <your-repository-url>
cd amor-auth
```

### 3. 配置数据库

创建MySQL数据库并执行初始化脚本：

```sql
-- 创建数据库
CREATE DATABASE amor_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行初始化脚本
mysql -u root -p amor_auth < src/main/resources/db/init.sql
```

### 4. 配置应用

编辑 `src/main/resources/application.properties`：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/amor_auth
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis配置
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Google OAuth2配置
spring.security.oauth2.client.registration.google.client-id=your_google_client_id
spring.security.oauth2.client.registration.google.client-secret=your_google_client_secret
```

### 5. 获取Google OAuth2凭据

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建新项目或选择现有项目
3. 启用Google+ API和Google Identity API
4. 创建OAuth2.0客户端ID
5. 设置授权重定向URI：`http://localhost:8080/login/oauth2/code/google`
6. 复制客户端ID和密钥到配置文件

### 6. 编译和启动应用

```bash
# 清理和编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 7. 访问应用

打开浏览器访问：http://localhost:8080

## 🚀 应用启动流程

### 启动步骤说明

1. **应用初始化**
   - Spring Boot启动，加载配置文件
   - 初始化Spring Security配置
   - 配置OAuth2客户端参数
   - 建立数据库连接池
   - 连接Redis缓存服务

2. **安全配置加载**
   - 加载OAuth2登录配置
   - 设置安全过滤器链
   - 配置授权重定向处理

3. **服务就绪**
   - 启动内嵌Tomcat服务器
   - 监听8080端口
   - 服务健康检查通过

### 启动日志关键信息

```
INFO : Started AmorAuthApplication in 3.456 seconds
INFO : OAuth2 client registration loaded: google
INFO : Redis connection established
INFO : Database connection pool initialized
INFO : Application ready on port 8080
```

## 🔐 Google登录调用流程


## 1. 登录流程总览

1. 用户访问系统首页（如 http://localhost:8080/login），系统检测未登录。
2. 用户点击“使用 Google 登录”按钮，前端跳转到 `/api/auth/login`。
3. 后端 `/api/auth/login` 接口重定向到 Google OAuth2 授权页面。
4. 用户在 Google 授权页面同意授权后，Google 回调到 `/api/auth/google/callback`。
5. 后端自动处理授权码，获取用户信息，创建或更新用户，并完成登录。
6. 登录成功后，后端返回 JSON 响应，前端可根据 `redirectUrl` 跳转到仪表盘等页面。

## 2. 关键接口说明

- `GET /api/auth/login`：跳转到 Google OAuth2 登录页面
- `GET /api/auth/google/callback`：Google 登录回调，自动处理授权码和用户信息
- `GET /api/auth/user`：获取当前登录用户信息
- `POST /api/auth/logout`：登出，清理会话

## 3. 主要代码调用链

1. **SecurityConfig.java**
    - 配置 OAuth2 登录端点和回调路径
    - 放行 `/api/auth/login` 和 `/api/auth/google/callback` 等接口
2. **AuthController.java**
    - `/api/auth/login`：重定向到 Google 授权页面
    - `/api/auth/google/callback`：处理 Google 回调，获取用户信息，创建/更新用户
3. **UserService.java**
    - `createOrUpdateUser`：同步 Google 用户信息到数据库
4. **LoginLogService.java**
    - 记录用户登录日志

## 4. API 调用示例

### 1. 跳转到 Google 登录
```http
GET http://localhost:8080/api/auth/login
```

### 2. 用户授权后，Google 回调
- 回调地址：`http://localhost:8080/api/auth/google/callback`
- 后端自动处理，无需手动调用

### 3. 获取当前登录用户信息
```http
GET http://localhost:8080/api/auth/user
Cookie: JSESSIONID=your_session_id
```

### 4. 登出
```http
POST http://localhost:8080/api/auth/logout
Cookie: JSESSIONID=your_session_id
```

## 5. 错误处理与常见问题

1. **client_id无效**
    - 错误：`invalid_client`
    - 解决：检查 Google Cloud Console 中的客户端 ID 配置

2. **重定向URI不匹配**
    - 错误：`redirect_uri_mismatch`
    - 解决：确保回调 URL 与 Google Console 中配置的完全一致

3. **authorization_request_not_found**
    - 解决：检查 session 是否丢失，前端请求需携带 Cookie（`credentials: 'include'`）

4. **访问令牌过期**
    - 错误：`invalid_token`
    - 解决：系统会自动处理令牌刷新，无需手动干预

## 6. 其他说明
- 所有 Google 登录流程均由后端自动处理，前端只需跳转到 `/api/auth/login`。
- 回调和用户信息同步逻辑在后端完成，无需前端传递授权码。
- 用户信息和登录日志自动写入数据库。
- 登出接口无需参数，直接 POST 即可。

## 📁 项目结构

```
amor-auth/
├── src/
│   ├── main/
│   │   ├── java/org/example/amorauth/
│   │   │   ├── AmorAuthApplication.java          # 应用启动类
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java           # 安全配置
│   │   │   │   ├── OAuth2HttpClientConfig.java   # OAuth2客户端配置
│   │   │   │   └── RedisConfig.java              # Redis配置
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java           # 认证控制器
│   │   │   │   ├── SessionController.java        # Session控制器
│   │   │   │   └── AdminController.java          # 管理员控制器
│   │   │   ├── service/
│   │   │   │   ├── UserService.java              # 用户服务
│   │   │   │   └── LoginLogService.java          # 登录日志服务
│   │   │   ├── entity/
│   │   │   │   ├── User.java                     # 用户实体
│   │   │   │   └── LoginLog.java                 # 登录日志实体
│   │   └── resources/
│   │       ├── application.properties             # 应用配置
│   │       └── mapper/
│   │           └── LoginLogMapper.xml                # 日志数据访问
│   │           └── UserMapper.xml                    # 用户数据访问
│   │       └── db/
│   │           └── init.sql                      # 数据库初始化脚本
└── pom.xml                                       # Maven依赖配置
```

## 🔍 故障排除

### 常见问题

1. **MySQL连接失败**
   - 检查MySQL服务是否启动
   - 验证数据库用户名和密码
   - 确认数据库名称是否正确

2. **Redis连接失败**
   - 检查Redis服务是否启动
   - 验证Redis端口配置
   - 检查防火墙设置

3. **Google OAuth2配置错误**
   - 验证客户端ID和密钥
   - 检查重定向URI配置
   - 确认Google API已启用

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交Issue和Pull Request来改进项目！
