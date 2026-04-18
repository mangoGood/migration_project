# 数据迁移项目 - 完整运行指南

## 📋 前提条件

1. **MySQL 源数据库** - 启用 binlog
2. **MySQL 目标数据库** - 用于接收迁移数据
3. **Java 8+**
4. **Maven 3.6+**

## 🚀 快速开始

### 步骤 1: 编译项目

```bash
mvn clean compile
```

### 步骤 2: 配置各模块

#### 2.1 配置 Capture 模块

编辑 `capture/src/main/resources/capture.properties`:

```properties
# MySQL connection properties
mysql.host=192.168.107.6
mysql.port=3306
mysql.user=root
mysql.password=rootpassword

# Binlog properties (optional - will start from current position if not specified)
binlog.file=
binlog.position=4

# Output directory - unified output location
output.dir=../output/binlog
```

#### 2.2 配置 Extract 模块

编辑 `extract/src/main/resources/extract.properties`:

```properties
# Input and output directories - unified output location
input.dir=../output/binlog
output.dir=../output/thl
```

#### 2.3 配置 Increment 模块

编辑 `increment/src/main/resources/increment.properties`:

```properties
# Input directory for THL files - unified output location
input.dir=../output/thl

# Target MySQL connection properties
target.mysql.url=jdbc:mysql://192.168.107.7:3306/
target.mysql.user=root
target.mysql.password=rootpassword
```

### 步骤 3: 运行完整流程

#### 方式 1: 分步运行（推荐用于测试）

```bash
# 1. 运行 Capture 模块（捕获 binlog）
cd capture
mvn exec:java -Dexec.mainClass="com.example.capture.BinlogCaptureMain"
# 按 Ctrl+C 停止捕获

# 2. 运行 Extract 模块（解析 binlog 并生成 THL）
cd ../extract
mvn exec:java -Dexec.mainClass="com.example.extract.ExtractMain"

# 3. 验证 THL 文件内容（可选）
mvn exec:java -Dexec.mainClass="com.example.extract.THLReaderTest" -Dexec.args="../output/thl"

# 4. 运行 Increment 模块（应用变更到目标库）
cd ../increment
mvn exec:java -Dexec.mainClass="com.example.increment.THLToSqlConverterMain"
```

#### 方式 2: 持续监听模式（生产环境推荐）

```bash
# 终端 1: 运行 Capture 模块（持续捕获 binlog）
cd capture
mvn exec:java -Dexec.mainClass="com.example.capture.BinlogCaptureMain"

# 终端 2: 运行 Continuous Extract 模块（持续监听新 binlog）
cd extract
mvn exec:java -Dexec.mainClass="com.example.extract.ContinuousExtractMain"

# 终端 3: 运行 Continuous Increment 模块（持续监听新 THL）
cd increment
mvn exec:java -Dexec.mainClass="com.example.increment.ContinuousIncrementMain"
```

**持续监听模式特性：**
- ✅ 自动检测新生成的 binlog/THL 文件
- ✅ 记录已处理的文件，避免重复处理
- ✅ 支持优雅停机（Ctrl+C）
- ✅ 可配置扫描间隔（默认 5 秒）
- ✅ 断点续传支持
- ✅ SQL 语句通过 DEBUG 日志输出

**配置说明：**

编辑 `extract/src/main/resources/extract.properties`:

```properties
# Input and output directories - unified output location
input.dir=../output/binlog
output.dir=../output/thl

# Scan interval for continuous mode (milliseconds)
scan.interval=5000  # 每 5 秒扫描一次
```

编辑 `increment/src/main/resources/increment.properties`:

```properties
# Input directory for THL files - unified output location
input.dir=../output/thl

# Target MySQL connection properties
target.mysql.url=jdbc:mysql://192.168.107.7:3306/
target.mysql.user=root
target.mysql.password=rootpassword

# Scan interval for continuous mode (milliseconds)
scan.interval=5000  # 每 5 秒扫描一次
```

**处理记录文件：**

- Extract 模块会在 `output/thl/` 目录下创建 `.processed_files` 文件
- Increment 模块会在 `output/thl/` 目录下创建 `.processed_thl_files` 文件

**SQL 日志输出：**

Increment 模块会在 DEBUG 级别输出 SQL 语句，可以在 `log4j.properties` 中配置：

```properties
# Enable DEBUG level to see SQL statements
log4j.logger.com.example.increment=DEBUG
```

## 📊 验证和监控

### 验证 Capture 模块

```bash
# 查看捕获的 binlog 文件
ls -lh output/binlog/

# 查看文件内容
head -n 20 output/binlog/binlog-*.bin
```

**预期输出：**
```
EVENT:ROTATE:0:binlog.000022:binlog.000022:1137
EVENT:FORMAT_DESCRIPTION:1775197267000:binlog.000022:binlog.000022:1137
EVENT:GTID:1775204368000:binlog.000022:binlog.000022:1137
EVENT:QUERY:1775204368000:binlog.000022:binlog.000022:1216:database=test:sql=BEGIN
EVENT:TABLE_MAP:1775204368000:binlog.000022:binlog.000022:1292:database=test:table=users:table_id=123
EVENT:EXT_WRITE_ROWS:1775204368000:binlog.000022:binlog.000022:1292:table_id=123
```

### 验证 Extract 模块

```bash
# 查看 THL 文件
ls -lh output/thl/

# 验证 THL 文件内容
cd extract
mvn exec:java -Dexec.mainClass="com.example.extract.THLReaderTest" -Dexec.args="../output/thl"
```

**预期输出：**
```
Found 6 THL files, using latest: thl-1775205478500.thl
Reading THL file: /path/to/output/thl/thl-1775205478500.thl

Event #1
  Seqno: 1
  EventId: binlog.000022:1137
  SourceId: mysql
  Timestamp: 2026-04-03 16:19:28.0
  Event Type: TABLE_MAP
  Binlog File: binlog.000022
  Binlog Position: 1292
  Database: test
  Table: users
  Table ID: 123
```

### 验证 Increment 模块

```bash
# 运行 Increment 模块
cd increment
mvn exec:java -Dexec.mainClass="com.example.increment.THLToSqlConverterMain"
```

**预期输出：**
```
Starting THL to SQL conversion and execution...
Connected to target MySQL: jdbc:mysql://192.168.107.7:3306/
Found 6 THL files in directory: ../output/thl
Processing THL file: thl-1775205478500.thl
Processing event: TABLE_MAP - database=test, table=users
Processing event: EXT_WRITE_ROWS - table_id=123
Generated SQL: INSERT INTO `test`.`users` VALUES (...);
Statistics - Total: 14, Successful: 14, Failed: 0
```

## 🔍 故障排查

### 问题 1: Capture 模块连接失败

**错误信息：**
```
Could not connect to MySQL server
```

**解决方案：**
1. 检查 MySQL 服务器是否运行
2. 检查网络连接和防火墙
3. 检查用户名和密码
4. 确保 MySQL 用户有 REPLICATION SLAVE 权限

```sql
-- 授予复制权限
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;
```

### 问题 2: Extract 模块找不到 binlog 文件

**错误信息：**
```
No binlog files found in directory: ../output/binlog
```

**解决方案：**
1. 确保 Capture 模块已运行
2. 检查 output/binlog 目录是否存在
3. 检查文件权限

### 问题 3: Increment 模块找不到表信息

**错误信息：**
```
Database or table name not found for INSERT event
```

**解决方案：**
1. 确保 Capture 模块已更新到最新版本
2. 重新运行 Capture 模块捕获新的 binlog
3. 检查 TABLE_MAP 事件是否被正确捕获

## 📈 性能优化

### 调整批处理大小

编辑 `extract/src/main/resources/extract.properties`:

```properties
# Batch processing
batch.size=1000
batch.timeout=5000
```

### 调整并发处理

编辑 `increment/src/main/resources/increment.properties`:

```properties
# Concurrent processing
num.workers=4
queue.capacity=10000
```

## 🎯 最佳实践

1. **定期备份** - 定期备份 output 目录
2. **监控日志** - 监控各模块的日志输出
3. **性能测试** - 在生产环境前进行性能测试
4. **错误处理** - 实现适当的错误处理和重试机制
5. **数据验证** - 定期验证源数据库和目标数据库的数据一致性

## 📝 日志配置

所有模块的日志级别可以在 `src/main/resources/log4j.properties` 中配置：

```properties
# Root logger
log4j.rootLogger=INFO, console

# Package specific loggers
log4j.logger.com.example=DEBUG
```

## 🔗 相关文档

- [README.md](../README.md) - 项目概述
- [架构设计](../README.md#架构设计) - 系统架构
- [API 文档](../README.md#核心接口) - 核心接口说明
