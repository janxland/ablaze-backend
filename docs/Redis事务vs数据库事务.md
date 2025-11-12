# Redis 事务 vs 数据库事务

## 一、Redis 事务

### 1.1 基本概念

Redis 事务是一组命令的集合，这些命令会被序列化并按顺序执行。Redis 事务保证：
- **原子性**：事务中的所有命令要么全部执行，要么全部不执行
- **隔离性**：事务在执行过程中不会被其他客户端发送的命令打断
- **不支持回滚**：即使某个命令执行失败，Redis 也不会回滚已执行的命令

### 1.2 Redis 事务命令

```redis
MULTI          # 开始事务
SET key1 value1
SET key2 value2
EXEC           # 执行事务
DISCARD        # 取消事务
WATCH key      # 监视键，如果键被修改，事务将失败
UNWATCH        # 取消监视
```

### 1.3 Redis 事务特点

**优点：**
- 执行速度快，所有命令在内存中执行
- 支持 WATCH 机制，可以实现乐观锁
- 适合高并发场景

**缺点：**
- 不支持回滚，命令执行失败不会撤销已执行的命令
- 不支持事务嵌套
- 事务中的命令不会立即执行，而是先放入队列

### 1.4 Redis 事务示例

```java
// 使用 RedisTemplate 执行事务
List<Object> results = redisTemplate.execute(new SessionCallback<List<Object>>() {
    @Override
    public List<Object> execute(RedisOperations operations) throws DataAccessException {
        operations.multi();
        operations.opsForValue().set("key1", "value1");
        operations.opsForValue().set("key2", "value2");
        return operations.exec();
    }
});
```

## 二、数据库事务（MySQL）

### 2.1 基本概念

数据库事务是数据库操作的最小执行单元，具有 ACID 特性：
- **原子性（Atomicity）**：事务中的所有操作要么全部成功，要么全部失败
- **一致性（Consistency）**：事务执行前后，数据库状态保持一致
- **隔离性（Isolation）**：并发事务之间相互隔离
- **持久性（Durability）**：事务提交后，数据永久保存

### 2.2 数据库事务隔离级别

1. **READ UNCOMMITTED（读未提交）**：最低隔离级别，可能读取到未提交的数据
2. **READ COMMITTED（读已提交）**：只能读取已提交的数据
3. **REPEATABLE READ（可重复读）**：MySQL 默认级别，保证同一事务中多次读取结果一致
4. **SERIALIZABLE（串行化）**：最高隔离级别，完全隔离

### 2.3 数据库事务特点

**优点：**
- 支持回滚，保证数据一致性
- 支持事务嵌套（保存点）
- 支持复杂的业务逻辑

**缺点：**
- 性能相对较低，涉及磁盘 I/O
- 锁机制可能影响并发性能

## 三、Redis 事务 vs 数据库事务对比

| 特性 | Redis 事务 | 数据库事务 |
|------|-----------|-----------|
| **原子性** | ✅ 支持 | ✅ 支持 |
| **一致性** | ⚠️ 部分支持 | ✅ 完全支持 |
| **隔离性** | ✅ 支持 | ✅ 支持（多级别） |
| **持久性** | ⚠️ 取决于持久化策略 | ✅ 完全支持 |
| **回滚** | ❌ 不支持 | ✅ 支持 |
| **性能** | ⚡ 高（内存操作） | 🐌 相对较低（磁盘 I/O） |
| **适用场景** | 高并发、简单操作 | 复杂业务逻辑、数据一致性要求高 |

## 四、使用场景建议

### 4.1 使用 Redis 事务的场景

1. **缓存更新**：批量更新缓存数据
2. **计数器操作**：多个计数器的原子性更新
3. **分布式锁**：使用 WATCH 实现乐观锁
4. **简单业务逻辑**：不需要回滚的简单操作

### 4.2 使用数据库事务的场景

1. **复杂业务逻辑**：涉及多个表的复杂操作
2. **数据一致性要求高**：需要保证强一致性
3. **需要回滚**：操作失败需要撤销已执行的操作
4. **持久化存储**：需要永久保存的数据

## 五、最佳实践

### 5.1 Redis 事务最佳实践

```java
// 1. 使用 WATCH 实现乐观锁
redisTemplate.execute(new SessionCallback<Object>() {
    @Override
    public Object execute(RedisOperations operations) throws DataAccessException {
        operations.watch("key");
        Object value = operations.opsForValue().get("key");
        // 业务逻辑处理
        operations.multi();
        operations.opsForValue().set("key", newValue);
        List<Object> results = operations.exec();
        if (results.isEmpty()) {
            // 事务执行失败，重试
        }
        return results;
    }
});

// 2. 使用 Lua 脚本实现原子性操作（推荐）
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
               "return redis.call('del', KEYS[1]) " +
               "else " +
               "return 0 " +
               "end";
redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), 
                     Collections.singletonList("key"), "value");
```

### 5.2 数据库事务最佳实践

```java
// 1. 使用 @Transactional 注解
@Transactional(rollbackFor = Exception.class)
public void updateUser(User user) {
    userMapper.updateById(user);
    // 其他操作
}

// 2. 手动管理事务
@Autowired
private TransactionTemplate transactionTemplate;

public void updateUser(User user) {
    transactionTemplate.execute(status -> {
        userMapper.updateById(user);
        // 其他操作
        return null;
    });
}
```

### 5.3 混合使用场景

```java
// 场景：更新数据库并同步更新缓存
@Transactional(rollbackFor = Exception.class)
public void updateArticle(Article article) {
    // 1. 更新数据库
    articleMapper.updateById(article);
    
    // 2. 更新缓存（使用 Redis 事务或 Lua 脚本）
    redisTemplate.execute(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) throws DataAccessException {
            operations.multi();
            operations.opsForValue().set("article:" + article.getId(), article);
            operations.delete("article:list:*"); // 删除相关列表缓存
            return operations.exec();
        }
    });
}
```

## 六、总结

1. **Redis 事务**：适合高并发、简单操作、不需要回滚的场景
2. **数据库事务**：适合复杂业务逻辑、数据一致性要求高、需要回滚的场景
3. **混合使用**：在实际项目中，通常结合使用 Redis 和数据库事务，发挥各自优势
4. **优先使用 Lua 脚本**：对于 Redis 的复杂操作，优先使用 Lua 脚本而不是事务，因为 Lua 脚本更灵活且性能更好

