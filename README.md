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

### 完整的OAuth2认证流程

#### 1. 用户发起登录请求

```
用户访问: http://localhost:8080
↓
系统检测未登录状态
↓
重定向到登录页面: http://localhost:8080/login
```

#### 2. 选择Google登录

```
用户点击 "使用Google登录" 按钮
↓
系统构造Google OAuth2授权URL
↓
重定向到Google授权服务器
```

**授权URL示例：**
```
https://accounts.google.com/oauth/authorize?
response_type=code&
client_id=your_client_id&
scope=openid%20profile%20email&
redirect_uri=http://localhost:8080/login/oauth2/code/google&
state=random_state_value
```

#### 3. Google授权处理

```
用户在Google页面确认授权
↓
Google验证用户身份
↓
Google重定向回应用回调地址，携带授权码
```

**回调URL示例：**
```
http://localhost:8080/login/oauth2/code/google?
code=authorization_code&
state=random_state_value
```

#### 4. 应用处理授权码

```
Spring Security OAuth2拦截回调请求
↓
使用授权码向Google请求访问令牌
↓
获取用户信息（姓名、邮箱、头像等）
```

**令牌请求：**
```http
POST https://oauth2.googleapis.com/token
Content-Type: application/x-www-form-urlencoded

client_id=your_client_id&
client_secret=your_client_secret&
code=authorization_code&
grant_type=authorization_code&
redirect_uri=http://localhost:8080/login/oauth2/code/google
```

#### 5. 用户信息处理

```
调用UserService.processOAuth2Login()
↓
检查用户是否已存在数据库
↓
如果是新用户，创建用户记录
↓
更新用户最后登录时间
↓
记录登录日志到数据库和Redis
↓
创建Spring Security认证对象
```

#### 6. 登录成功处理

```
设置用户认证状态
↓
重定向到仪表板页面
↓
用户登录完成
```

### 关键代码调用链

1. **SecurityConfig.java** - 配置OAuth2登录端点
2. **AuthController.java** - 处理登录成功后的重定向
3. **UserService.java** - 处理用户信息同步
4. **LoginLogService.java** - 记录登录日志

### API调用示例

#### 手动调用Google登录流程

```bash
# 1. 获取授权URL
curl -X GET "http://localhost:8080/oauth2/authorization/google"

# 2. 用户完成授权后，系统会自动处理回调

# 3. 检查登录状态
curl -X GET "http://localhost:8080/api/user/profile" \
  -H "Cookie: JSESSIONID=your_session_id"
```

### 错误处理

#### 常见错误及解决方案

1. **client_id无效**
   ```
   错误：invalid_client
   解决：检查Google Cloud Console中的客户端ID配置
   ```

2. **重定向URI不匹配**
   ```
   错误：redirect_uri_mismatch
   解决：确保回调URL与Google Console中配置的完全一致
   ```

3. **访问令牌过期**
   ```
   错误：invalid_token
   解决：系统会自动处理令牌刷新
   ```

### 调试技巧

#### 启用详细日志

在 `application.properties` 中添加：

```properties
# OAuth2调试日志
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.web.client.RestTemplate=DEBUG

# 请求响应日志
logging.level.org.apache.http=DEBUG
```

#### 查看用户认证信息

```java
// 在Controller中获取当前用户信息
@GetMapping("/debug/user")
public ResponseEntity<?> getCurrentUser(Authentication authentication) {
    return ResponseEntity.ok(authentication.getPrincipal());
}
```

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
│   │   │   │   ├── WelcomeController.java        # 欢迎页控制器
│   │   │   │   └── AdminController.java          # 管理员控制器
│   │   │   ├── service/
│   │   │   │   ├── UserService.java              # 用户服务
│   │   │   │   └── LoginLogService.java          # 登录日志服务
│   │   │   ├── entity/
│   │   │   │   ├── User.java                     # 用户实体
│   │   │   │   └── LoginLog.java                 # 登录日志实体
│   │   │   └── mapper/
│   │   │       ├── UserMapper.java               # 用户数据访问
│   │   │       └── LoginLogMapper.java           # 日志数据访问
│   │   └── resources/
│   │       ├── application.properties             # 应用配置
│   │       ├── templates/
│   │       │   └── login.html                    # 登录页面
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
