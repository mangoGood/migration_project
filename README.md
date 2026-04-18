# Migration Project - 数据迁移项目

一个基于 MySQL binlog 和 MongoDB oplog 的数据迁移系统，支持实时数据捕获、转换和应用。

## 项目结构

```
migration_project/
├── common/          # 通用模块 - 基础接口和工具类
├── thl/             # THL 模块 - Transaction History Log 文件格式定义
├── capture/         # 捕获模块 - 从源数据库捕获变更
├── extract/         # 提取模块 - 解析变更数据并生成 THL 文件
└── increment/       # 增量应用模块 - 将 THL 转换为 SQL 并在目标库执行
```

## 快速开始

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 运行 Capture 模块

Capture 模块负责从 MySQL 源数据库捕获 binlog 事件。

**配置文件：** `capture/src/main/resources/capture.properties`

```properties
# MySQL connection properties
mysql.host=192.168.107.6
mysql.port=3306
mysql.user=root
mysql.password=rootpassword

# Binlog properties (optional - will start from current position if not specified)
binlog.file=
binlog.position=4

# Output directory
output.dir=./output
```

**运行命令：**

```bash
# 方式1：使用 Maven
cd capture
mvn exec:java -Dexec.mainClass="com.example.capture.BinlogCaptureMain"

# 方式2：直接运行 jar
java -cp target/classes:target/dependency/* com.example.capture.BinlogCaptureMain

# 方式3：指定配置文件
java -cp target/classes:target/dependency/* com.example.capture.BinlogCaptureMain /path/to/capture.properties
```

### 3. 运行 Extract 模块

Extract 模块负责解析 binlog 文件并生成 THL 文件。

**配置文件：** `extract/src/main/resources/extract.properties`

```properties
# Input and output directories
input.dir=./output
output.dir=./thl
```

**运行命令：**

```bash
# 方式1：使用 Maven
cd extract
mvn exec:java -Dexec.mainClass="com.example.extract.ExtractMain"

# 方式2：直接运行 jar
java -cp target/classes:target/dependency/* com.example.extract.ExtractMain
```

### 4. 运行 Increment 模块

Increment 模块负责将 THL 文件转换为 SQL 语句并在目标数据库执行。

**配置文件：** `increment/src/main/resources/increment.properties`

```properties
# Input directory for THL files
input.dir=./thl

# Target MySQL connection properties
target.mysql.url=jdbc:mysql://192.168.107.7:3306/
target.mysql.user=root
target.mysql.password=rootpassword
```

**运行命令：**

```bash
# 方式1：使用 Maven
cd increment
mvn exec:java -Dexec.mainClass="com.example.increment.IncrementMain"

# 方式2：直接运行 jar
java -cp target/classes:target/dependency/* com.example.increment.IncrementMain
```

## 完整流程

### 方式1：分步执行

```bash
# 1. 编译项目
mvn clean compile

# 2. 运行 Capture 模块（捕获 binlog）
cd capture
mvn exec:java -Dexec.mainClass="com.example.capture.BinlogCaptureMain"

# 3. 运行 Extract 模块（解析 binlog 并生成 THL）
cd ../extract
mvn exec:java -Dexec.mainClass="com.example.extract.ExtractMain"

# 4. 运行 Increment 模块（应用变更到目标库）
cd ../increment
mvn exec:java -Dexec.mainClass="com.example.increment.IncrementMain"
```

### 方式2：打包后运行

```bash
# 1. 打包项目
mvn clean package

# 2. 运行各个模块
java -jar capture/target/capture-1.0-SNAPSHOT.jar
java -jar extract/target/extract-1.0-SNAPSHOT.jar
java -jar increment/target/increment-1.0-SNAPSHOT.jar
```

## 配置说明

### Capture 模块配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| mysql.host | MySQL 主机地址 | localhost |
| mysql.port | MySQL 端口 | 3306 |
| mysql.user | MySQL 用户名 | root |
| mysql.password | MySQL 密码 | |
| binlog.file | 起始 binlog 文件 | 当前位置 |
| binlog.position | 起始 binlog 位置 | 4 |
| output.dir | 输出目录 | ./output |

### Extract 模块配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| input.dir | 输入目录（binlog 文件） | ../output |
| output.dir | 输出目录（THL 文件） | ../thl |

**注意**：路径是相对于 extract 模块目录的，使用 `../` 表示项目根目录。

### Increment 模块配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| input.dir | 输入目录（THL 文件） | ../thl |
| target.mysql.url | 目标 MySQL URL | |
| target.mysql.user | 目标 MySQL 用户名 | |
| target.mysql.password | 目标 MySQL 密码 | |

**注意**：路径是相对于 increment 模块目录的，使用 `../` 表示项目根目录。

## 测试

### 运行所有测试

```bash
mvn test
```

### 运行特定模块的测试

```bash
mvn test -pl common
mvn test -pl thl
```

### 运行特定的测试类

```bash
mvn test -Dtest=RetryUtilTest
mvn test -Dtest=AsyncBatchProcessorTest
```

## 架构设计

### 模块依赖关系

```
common (基础模块)
    ↓
thl (THL 文件格式定义)
    ↓
capture → extract → increment (功能模块)
```

### 核心接口

- **Capture**: 数据捕获接口
- **Extractor**: 数据提取接口
- **AbstractCapture**: 捕获抽象基类
- **AbstractExtractor**: 提取抽象基类

### 支持的数据库

- **MySQL**: 通过 binlog 捕获变更
- **MongoDB**: 通过 oplog 捕获变更（预留接口）

## 监控和日志

### 日志配置

所有模块使用 SLF4J + Log4j 进行日志记录。

### 监控指标

- 事件捕获数
- 事件处理数
- 成功/失败数
- 吞吐量（events/second）
- 处理持续时间

## 性能优化

- **异步批处理**: 使用 `AsyncBatchProcessor` 进行高吞吐量处理
- **重试机制**: 使用 `RetryUtil` 进行自动重试
- **连接池**: 支持数据库连接池
- **并发处理**: 支持多线程处理

## 故障排查

### 常见问题

1. **连接 MySQL 失败**
   - 检查 MySQL 服务器是否运行
   - 检查网络连接和防火墙设置
   - 检查用户名和密码是否正确
   - 检查 MySQL binlog 是否启用

2. **找不到配置文件**
   - 确保配置文件在 `src/main/resources` 目录下
   - 检查配置文件名是否正确

3. **权限不足**
   - 确保 MySQL 用户有 REPLICATION SLAVE 权限
   - 确保有文件读写权限

### 启用调试日志

修改 `log4j.properties` 文件：

```properties
log4j.logger.com.example=DEBUG
```

## 开发指南

### 添加新的数据库支持

1. 实现 `Capture` 接口
2. 继承 `AbstractCapture` 类
3. 实现数据库特定的捕获逻辑

### 添加新的事件类型

1. 在 `THLEvent` 中添加新的事件类型常量
2. 在 `MySQLBinlogExtractor` 中添加解析逻辑
3. 在 `THLToSqlConverter` 中添加 SQL 生成逻辑

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
