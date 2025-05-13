# **wenziyue-uid-starter**





基于美团开源项目 [Leaf Segment](https://github.com/Meituan-Dianping/Leaf) 封装的分布式唯一 ID 生成器，支持高可用、高并发、趋势递增的 17 位长整型 ID。



------



## **✨ 功能特性**

- 基于数据库的 **Segment 模式**
- **自动建表**（配合 Flyway 使用）
- **自定义初始 ID 值**
- 支持 **Cached 模式 / Default 模式**
- 支持可选依赖 Flyway（无需强制使用）



------



## **📦 引入依赖**

```xml
<dependency>
    <groupId>com.wenziyue</groupId>
    <artifactId>wenziyue-uid-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```



------



## **⚙️ 使用方式**

### **1. 使用 Flyway 自动建表（推荐）**

#### **✅ 添加 Flyway 依赖**

你需要在主项目中添加 Flyway：

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



#### **✅ 配置** 

#### **application.yml**



```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    create-schemas: true

wenziyue:
  uid:
    use-cached: false   # 是否使用 CachedUidGenerator（默认 false）
    biz-tag: blog        # 业务标识
    step: 100            # 步长
    init-id: 10000000000000000  # 初始 ID（默认从 1 开始）
    auto-create-table: true     # 是否自动初始化 leaf_alloc 表记录
```

> ✅ 表结构脚本将从 starter 中的 classpath:wzyuid/db/R__create_leaf_tables.sql 自动执行

> ✅ 初始记录也会自动写入数据库，无需手动维护



------



### **2. 不使用 Flyway（手动建表）**

#### **✅ application.yml 配置**

```yaml
wenziyue:
  uid:
    use-cached: true
    biz-tag: blog
    step: 100
    init-id: 10000000000000000
    auto-create-table: false
```



#### **✅ 手动创建数据库表**

```sql
CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag     VARCHAR(128) NOT NULL COMMENT '业务标识',
    max_id      BIGINT       NOT NULL COMMENT '当前号段最大值',
    step        INT          NOT NULL DEFAULT 1000 COMMENT '号段步长',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式 ID 号段管理表';

INSERT INTO leaf_alloc (biz_tag, max_id, step, update_time) VALUES ('你的业务名', '起始id，建议10000000000000000', '每次获取id数量，建议1000', NOW());
```

> 若未使用 Flyway，则必须手动建表（表名为 leaf_alloc，不可更改）



------



## **🚀 使用方式**

```java
@Autowired
private UidUtils uidUtils;

public void test() {
    long id = uidUtils.nextId();
    System.out.println("生成的ID: " + id);
}
```



------



## **⚠️ 注意事项**

1. 本 starter 中 Leaf 表的建表脚本路径为 classpath:wzyuid/db，**请确保项目中没有同名路径，否则可能导致项目启动自动执行其中的sql文件。**
2. 若不启用 Flyway，请务必手动创建表。
3. 推荐配置 init-id 起始为 10000000000000000，避免前期 ID 长度不一致。



------

