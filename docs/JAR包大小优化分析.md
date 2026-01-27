# JAR 包大小优化分析

## 📊 当前状态

- **JAR 文件大小**: 73.69 MB (77,272,261 bytes)
- **依赖库数量**: 124 个
- **打包时间**: ~17 秒（首次） / ~3.5 秒（增量）

## 🔍 占用空间最大的依赖（Top 20）

| 依赖库 | 大小 | 占比 | 说明 |
|--------|------|------|------|
| hibernate-core-5.6.15.Final.jar | 7.33 MB | 9.71% | Hibernate ORM 核心库 |
| byte-buddy-1.12.23.jar | 3.86 MB | 5.11% | 字节码生成库（Hibernate 依赖） |
| druid-1.2.20.jar | 3.82 MB | 5.06% | 数据库连接池 |
| tomcat-embed-core-9.0.83.jar | 3.46 MB | 4.59% | 内嵌 Tomcat 服务器 |
| guava-31.1-jre.jar | 2.89 MB | 3.83% | Google Guava 工具库 |
| mysql-connector-j-8.0.33.jar | 2.42 MB | 3.21% | MySQL JDBC 驱动 |
| spring-data-redis-2.7.18.jar | 2.09 MB | 2.77% | Spring Data Redis |
| aspectjweaver-1.9.7.jar | 2.04 MB | 2.70% | AOP 切面编织器 |
| lombok-1.18.30.jar | 1.96 MB | 2.60% | 代码生成工具（编译时） |
| lettuce-core-6.1.10.RELEASE.jar | 1.81 MB | 2.40% | Redis 客户端 |
| mybatis-3.5.15.jar | 1.77 MB | 2.34% | MyBatis ORM 框架 |
| reactor-core-3.4.34.jar | 1.70 MB | 2.26% | 响应式编程库 |
| spring-boot-autoconfigure-2.7.18.jar | 1.65 MB | 2.19% | Spring Boot 自动配置 |
| spring-web-5.3.31.jar | 1.60 MB | 2.13% | Spring Web MVC |
| jackson-databind-2.13.5.jar | 1.50 MB | 1.99% | JSON 序列化库 |
| spring-security-config-5.7.11.jar | 1.48 MB | 1.96% | Spring Security 配置 |
| kotlin-stdlib-1.6.21.jar | 1.47 MB | 1.95% | Kotlin 标准库（Spring 依赖） |
| spring-core-5.3.31.jar | 1.45 MB | 1.93% | Spring 核心库 |
| spring-boot-2.7.18.jar | 1.43 MB | 1.90% | Spring Boot 核心 |
| hutool-core-5.8.25.jar | 1.39 MB | 1.85% | Hutool 工具库 |

**前 20 个依赖合计**: ~45 MB (约 61%)

## ⚠️ 问题分析

### 1. **重复的 ORM 框架**
- **Hibernate** (7.33 MB) + **MyBatis** (1.77 MB) = 9.1 MB
- 项目同时引入了 `spring-boot-starter-data-jpa` (包含 Hibernate) 和 `mybatis-plus-boot-starter`
- **建议**: 只使用 MyBatis Plus，移除 JPA/Hibernate

### 2. **开发时依赖被打包**
- **Lombok** (1.96 MB) - 编译时工具，运行时不需要
- **mybatis-plus-generator** (代码生成器) - 开发工具，生产环境不需要
- **velocity-engine-core** (代码生成模板引擎) - 仅用于代码生成

### 3. **可能未使用的依赖**
- **spring-boot-starter-cache** - 如果未使用 Spring Cache，可以移除
- **spring-boot-starter-mail** - 如果未使用邮件功能，可以移除
- **hutool-crypto** - 如果只使用了部分功能，可以考虑只引入需要的模块

### 4. **大型工具库**
- **Guava** (2.89 MB) - 功能强大但体积大，如果只用了少量功能可以考虑替换
- **Hutool** (1.39 MB) - 类似问题

## 🎯 优化建议

### 优先级 1: 立即优化（可减少 ~10-15 MB）

#### 1. 移除 JPA/Hibernate（节省 ~9 MB）
```xml
<!-- 移除这个依赖 -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
-->
```

#### 2. 将开发工具设为 provided 或移除（节省 ~2-3 MB）
```xml
<!-- 代码生成器 - 只在开发时需要 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>3.5.5</version>
    <scope>provided</scope>  <!-- 或完全移除，只在需要时临时添加 -->
</dependency>

<dependency>
    <groupId>org.apache.velocity</groupId>
    <artifactId>velocity-engine-core</artifactId>
    <version>2.3</version>
    <scope>provided</scope>  <!-- 或完全移除 -->
</dependency>
```

### 优先级 2: 可选优化（可减少 ~3-5 MB）

#### 3. 检查并移除未使用的依赖
```xml
<!-- 如果未使用 Spring Cache -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
-->

<!-- 如果未使用邮件功能 -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
-->
```

#### 4. 优化 Lombok（已正确配置，但可以确认）
```xml
<!-- Lombok 已正确配置为 provided，运行时不会被打包 -->
<!-- 但编译后的 class 文件可能包含一些元数据 -->
```

### 优先级 3: 高级优化（需要评估）

#### 5. 使用 Spring Boot 的 Thin JAR
- 将依赖外置，只打包应用代码
- 需要配合依赖管理工具

#### 6. 使用 Docker 多阶段构建
- 构建阶段包含所有依赖
- 运行镜像只包含运行时依赖

#### 7. 代码混淆和压缩
- 使用 ProGuard 或 R8 进行代码混淆和优化
- 可以进一步减小体积

## 📝 优化后的 pom.xml 示例

```xml
<dependencies>
    <!-- Web 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- 验证支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- 移除 JPA，只使用 MyBatis Plus -->
    <!--
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    -->
    
    <!-- 邮件支持（如果不需要可以移除） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    
    <!-- AOP 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    
    <!-- 缓存支持（如果不需要可以移除） -->
    <!--
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    -->
    
    <!-- 消息队列 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- 数据库相关 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
        <version>1.2.20</version>
    </dependency>
    
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.5</version>
    </dependency>
    
    <!-- 代码生成器 - 只在开发时需要 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-generator</artifactId>
        <version>3.5.5</version>
        <scope>provided</scope>
    </dependency>
    
    <dependency>
        <groupId>org.apache.velocity</groupId>
        <artifactId>velocity-engine-core</artifactId>
        <version>2.3</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Lombok - 编译时工具 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
        <version>1.18.30</version>
        <optional>true</optional>
    </dependency>
    
    <!-- 其他依赖保持不变 -->
    <!-- ... -->
</dependencies>
```

## 🎯 预期优化效果

| 优化项 | 预计减少 | 累计减少 |
|--------|----------|----------|
| 移除 Hibernate/JPA | ~9 MB | 9 MB |
| 移除代码生成器依赖 | ~2-3 MB | 11-12 MB |
| 移除未使用的 starter | ~1-2 MB | 12-14 MB |
| **总计** | **~12-14 MB** | **最终大小: ~60 MB** |

## ⚡ 快速优化步骤

1. **备份当前 pom.xml**
2. **移除 `spring-boot-starter-data-jpa` 依赖**
3. **将代码生成相关依赖设为 `provided`**
4. **检查并移除未使用的 starter**
5. **重新打包**: `mvn clean package`
6. **验证功能**: 确保所有功能正常
7. **对比大小**: 使用分析脚本查看优化效果

## 📌 注意事项

1. **移除 JPA 前**：确保代码中没有使用 JPA 的注解和接口
2. **代码生成器**：如果经常使用，可以保留但设为 `provided`
3. **功能验证**：每次优化后都要完整测试功能
4. **生产环境**：建议在测试环境充分验证后再部署

## 🔧 分析脚本使用

```powershell
# 运行分析脚本
powershell -ExecutionPolicy Bypass -File analyze-jar-size.ps1
```

脚本会显示：
- JAR 文件总大小
- 依赖库数量
- 前 20 个最大的依赖库及其占比
