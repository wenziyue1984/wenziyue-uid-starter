# **wenziyue-uid-starter**





基于美团开源项目 [Leaf Segment](https://github.com/Meituan-Dianping/Leaf) 封装的分布式唯一 ID 生成器组件，支持高并发、高可用、趋势递增的ID。



> 当前实现完全参考 Leaf 的 Segment 模式，未引入雪花算法（Snowflake）。支持后续扩展。



------





## **✨ 功能特性**



- 基于数据库的 **Segment 模式** 实现（支持双段缓存、懒加载、异步预加载等机制）
- 支持 **高性能、本地发号、线程安全**
- 自动建表（配合 Flyway 使用）
- 支持 **自定义初始值、步长、业务隔离**
- 支持 **tag 缓存热更新**（定时刷新数据库感知新 tag）
- 内置异常码机制，明确区分各种失败场景



------



## **📦引入依赖**

首先在 `pom.xml` 中添加 GitHub 仓库地址：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/wenziyue1984/wenziyue-uid-starter</url>
    </repository>
</repositories>
```

然后引入依赖：

```xml
<dependency>
    <groupId>com.wenziyue</groupId>
    <artifactId>wenziyue-uid-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```



------



## **⚙️ 配置方式**



### **✅ 推荐使用 Flyway 自动建表**

#### **Maven 中添加依赖：**

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!--  如果使用mysql的话还需要添加  -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>   <!-- 专门支持 MySQL / MariaDB -->
</dependency>
```



#### **application.yml 配置示例：**

```yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    create-schemas: true

wenziyue:
  uid:
    biz-tag: blog
    step: 1000  # 每次获取号段长度
    init-id: 10000000000000000  #  ID从10000000000000000开始
    prepare-next-percent: 80  # 使用当前号段多少百分比时预加载下一个号段，默认 80，数值范围 1-100
    auto-create-table: true
```

> ✅ 启动后会自动执行 classpath:db/migration/R__create_leaf_tables.sql 创建 leaf_alloc 表并初始化对应 tag 记录。



------



### **❎ 手动建表方式（不使用 Flyway）**



```yaml
wenziyue:
  uid:
    biz-tag: blog
    step: 1000  # 每次获取号段长度
    init-id: 10000000000000000  #  ID从10000000000000000开始
    prepare-next-percent: 80  # 使用当前号段多少百分比时预加载下一个号段，默认 80，数值范围 1-100
    auto-create-table: false
```

对应建表 SQL：

```sql
CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag     VARCHAR(128) NOT NULL COMMENT '业务标识',
    max_id      BIGINT       NOT NULL COMMENT '当前号段最大值',
    step        INT          NOT NULL DEFAULT 1000 COMMENT '号段步长',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式 ID 号段管理表';

INSERT INTO leaf_alloc (biz_tag, max_id, step, update_time) 
VALUES ('blog', 10000000000000000, 1000, NOW());
```



------



## **🚀 使用方式**

```java
import com.wenziyue.uid.core.IdGen;
import com.wenziyue.uid.common.Result;
import com.wenziyue.uid.common.Status;

@Autowired
private IdGen idGen;

public void create() {
  Result id = idGen.nextId(); // 返回 Result 封装对象
  if (id.getStatus().equals(Status.SUCCESS)) {
    System.out.println("生成 ID：" + id);
  }
}
```



------



## **🔁 核心发号机制**

- 初始化时预加载所有业务 tag，创建空缓存（SegmentBuffer），**懒加载首段**

- 当当前段剩余不到 20% 时，异步后台线程预加载下一段

- 当前段耗尽时：

  - 若下一段准备好：立即切换
  - 若未准备好：等待最多 50ms，若仍未就绪返回异常码

  

所有逻辑均采用原子类 + 读写锁组合，保证高并发下的一致性与可用性。



------



## **❗ 异常码说明**



| **异常码值** | **含义**                              |
| ------------ | ------------------------------------- |
| -1           | Segment ID 缓存未初始化成功           |
| -2           | 配置的 bizTag 不存在于数据库          |
| -3           | 当前段与下一段均未准备好，ID 无法生成 |



------



## **🔄 缓存动态刷新机制**

- 使用定时任务 ScheduledExecutorService 每 60 秒从数据库刷新一次所有 bizTag
- 自动感知新增或删除 tag，支持缓存热更新（无需重启）



------

## **✂️ 不能保证id连续性**

当服务重启时，重启前已获取的号段会丢失。因此不能保证id连续性，**如果要求id必须连续那么请不要使用**。

------


## **🔍 与 Redis INCR 模式的对比**



| **对比项** | **Segment 模式（本组件）**              | **Redis INCR 模式**                |
| ---------- | --------------------------------------- | ---------------------------------- |
| 性能       | 极高（内存发号）                        | 高（依赖网络与 Redis 性能）        |
| 分布式支持 | 需配置 DB，服务间共享 tag               | 天然支持                           |
| 跨语言支持 | 无，限 Java 项目                        | 高，可用于多端系统                 |
| 可控性     | 强（本地缓存、懒加载、步长可调）        | 弱（仅通过 key 控制）              |
| 持久性     | 高（DB 存储）                           | 依赖 Redis 持久化策略              |
| 容错性     | 高（双段缓存，线程隔离）                | Redis 异常时全链路中断             |
| 部署成本   | 中（需建表）                            | 低（仅 Redis 即可）                |
| 适用场景   | 核心业务高并发 ID（订单、用户、评论等） | 原型系统、分布式微服务、跨语言接口 |



------



## **✅ 推荐使用场景**

- 高并发业务系统中的 ID 生成
- 需要保证 **趋势递增**、**高可用**、**低延迟** 的场景
- 对 Redis 依赖较少或希望脱离中间件的系统
- 希望将 UID 逻辑内聚在 Java 项目内部，方便维护与扩展



------



## **🔚 总结**

这个 starter 的目标是提供一套高度工程化、灵活可控的 UID 生成方案，它在保证稳定性和性能的同时也兼顾了易用性和扩展性。

你可以将它集成在任何 Spring Boot 项目中，作为正式环境下的主力发号组件。未来也支持拓展为 UID 微服务。

------

