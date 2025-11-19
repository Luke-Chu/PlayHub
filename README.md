# PlayHub 趣桌玩汇

单独做一些关键技术难点，以及测试。

## 超卖问题 - 不解决

Apifox自动化测试请求示例：

```
POST /voucher-order/seckill/withOversold/{voucherId}
```

测试条件：使用 200 个并发线程对同一优惠券进行秒杀请求。

问题现象：由于数据库字段 `stock` 为 `BIGINT UNSIGNED`，扣成负数时会触发异常。

```
Data truncation: BIGINT UNSIGNED value is out of range
```

如果字段 `stock` 不是 `UNSIGNED`，库存就会变为负数，多次测试200个线程会超卖9个。

## 超卖问题 - 版本号解决

Apifox自动化测试请求示例：

```
POST /voucher-order/seckill/stockAsVersion/{voucherId}
```

解决超卖方案：乐观锁（查不加锁，修改判断数据是否正确）

```sql
update voucher set stock = stock - 1 where id = ? and stock == ?;
```

测试条件：使用 100 个并发线程对同一优惠券进行秒杀请求。预期效果：扣减100库存。

问题现象：100的库存，只扣减了13个，所以87个用户扣减失败。

## 超卖问题 - 库存大于零解决

Apifox自动化测试请求示例：

```
POST /voucher-order/seckill/stockGreaterZero/{voucherId}
```

解决方案：乐观锁

```sql
update voucher set stock = stock - 1 where id = ? and stock > 0;
```

因为`id`是索引字段，所以`InnoDB`会定位到对应的行，然后会对该行加上行级排他锁（`X`锁）。加锁后，会检查`stock > 0`这个条件是否满足。满足，则扣减库存并更新到数据库；否则，不进行更新操作。这便解决了库存超卖问题。

> 排他锁的作用是阻止其他事务对该行进行读（除非是一致性非锁定读）和写操作，直到当前事务提交或回滚。

预期结果：不超卖，也不少卖。

## 一人一单 - 初版

Apifox自动化测试请求示例：

```
POST /voucher-order/seckill/onePersonOneOrder/{voucherId}
```

在扣减库存前新增一个逻辑用来判断是不是有了订单：

```java
// 3. 查询订单表，看看有没有数据
VoucherOrder existingOrder = voucherOrderMapper.findByUserIdAndVoucherId(UserContext.getUserId(), voucherId);
	if (existingOrder != null) {
	return Result.error("每人限领一张");
}

// 4. 扣减库存
```

测试结果：100个请求（同一个用户id），写入了3条订单数据，说明还没完全做到一人一单。

存在问题：现在的问题还是和之前一样，并发查询数据库，都不存在订单，然后都会去扣减库存。所以还是需要加锁，但是乐观锁比较适合更新数据，而现在是插入数据，需要使用悲观锁操作。

> 问题：存在并发问题，不能实现一人一单。

## 一人一单 - synchronized

加锁：查询加锁、插入加锁。

```java
    @Transactional
    public synchronized Result<Long> createVoucherOrderSynchronized(long voucherId) {
        // 3. 查询订单表，看看有没有数据
        VoucherOrder existingOrder = voucherOrderMapper.findByUserIdAndVoucherId(UserContext.getUserId(), voucherId);
        if (existingOrder != null) {
            return Result.error("每人限领一张");
        }

        // 4. 扣减库存
        int updateCount = voucherMapper.decreaseStockGreaterZero(voucherId);
        if (updateCount <= 0) {
            return Result.error("库存不足");
        }

        // 5. 创建订单
        return Result.success(createVoucherOrder(voucherId).getId());
    }
```

> 问题：锁方法的锁粒度太大，导致所有请求串行。

这样确实能实现一人一单，但`synchronized`锁粒度太大，会导致所有请求都串行。实际上只需要针对每个用户加锁即可，所以改成锁`userId`。

```java
    @Transactional
    public Result<Long> createVoucherOrderSynchronized(long voucherId) {
        Long userId = UserContext.getUserId();
        synchronized (userId.toString().intern()) {
            // 3. 查询订单表，看看有没有数据
            VoucherOrder existingOrder = voucherOrderMapper.findByUserIdAndVoucherId(userId, voucherId);
            if (existingOrder != null) {
                return Result.error("每人限领一张");
            }

            // 4. 扣减库存
            int updateCount = voucherMapper.decreaseStockGreaterZero(voucherId);
            if (updateCount <= 0) {
                return Result.error("库存不足");
            }

            // 5. 创建订单
            return Result.success(createVoucherOrder(voucherId).getId());
        }
    }
```

> 问题：锁释放了，但事务还没结束。

## 一人一单 - 单体系统 FianlMethod

上面这个代码还是存在问题，该方法被spring的事务控制，如果在方法内部加锁，可能会导致当前方法事务还没有提交，但是锁已经释放，这也会导致问题，将当前方法整体包裹起来，确保事务不会出现问题。

```java
Long userId = UserContext.getUserId();
synchronized (userId.toString().intern()) {
    return createVoucherOrderFinalMethod(voucherId);
}
```

> 问题：事务没生效。

但以上做法依然有问题，因为`createVoucherOrderFinalMethod`方法的调用，其实是通过`this.`的方式调用的，事务想要生效，还得利用代理来生效，所以需要获得原始的事务对象， 来操作事务。

```java
Long userId = UserContext.getUserId();
synchronized (userId.toString().intern()) {
    VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
    return proxy.createVoucherOrderFinalMethod(voucherId);
}
```

需要在启动类上添加对应注解，并在pom文件中引入对应依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <!-- 无需指定版本，Spring Boot 父工程已统一管理 -->
</dependency>

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
```

> 问题：集群环境下 synchronized 锁失效。

## 一人一单 - SETNX 分布式锁

获取userId的对象锁实际上是获取对象监视器锁，在一个JVM中只有一个userId的对象监视器锁，多个JVM就有多个，集群环境下有多个Tomcat，也即有多个JVM，synchronized 只锁所在JVM里的对象，管不了其他JVM内容，所以集群下 synchronized 无法保证一人一单。

解决方案：使用Redis的分布式锁（主要命令：SETNX命令），业务代码改造如下：

```java
Long userId = UserContext.getUserId();
SimpleRedisLock simpleRedisLock = new SimpleRedisLock("voucher:" + voucherId + userId, redisTemplate);
boolean isLock = simpleRedisLock.tryLock(1200);
// 加锁失败
if (!isLock) {
    return Result.error("请勿重复下单");
}
try {
    VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
    return proxy.createVoucherOrderFinalMethod(voucherId);
} finally {
    // 释放锁
    simpleRedisLock.unlock();
}
```

这里Redis的key是“lock:voucher:2:1”这种形式，value是线程ID。

Apifox测试接口：

```
POST /voucher-order/seckill/distributedLockWithRedis/{voucherId}
```

> 问题：存在锁误删问题。每个请求删除锁时应该只删除自己加的锁（注意请求，哪怕是同一个用户）。

## 一人一单 - Lua 脚本解决误删

先分析下误删问题：现在有两个请求（线程），都是同一个用户抢购同一个优惠券。

- 当请求1持有锁后内部出现了阻塞，导致锁自动释放；
- 这时请求2来尝试获得锁，就拿到了这把锁（因为是同一个用户抢购同一个优惠券，key是一样的）；
- 然后请求2在持有锁的过程中：线程1恢复，继续执行，当请求1走到删除逻辑时，就会把线程2加的锁进行删除。

误删在这个项目中会出现什么问题呢？

在上面的分析过程中，结合业务实际来看，线程拿到锁后会“从数据库查有没有订单，如果没有在创建订单，否则返回错误”。因为开启了事务，线程1和线程2会在同一个事务中代码中执行，可能线程1和线程2都查询到数据库没有订单数据，然后执行创建订单逻辑，导致一人多单。（其实这不是误删导致的问题，这是锁未续期导致的问题）。

- 如果第1个请求成功创建了订单，然后删了第2个请求的锁，2个线程会进入临界区，可能导致一人多单，但这不是误删导致的，是锁未续期导致。
- 如果第1个请求没能成功创建订单，然后删了第2个请求的锁，此时又来了第3个请求，第2个请求和第3个请求同时进入临界区代码，可能会导致一人多单，这是误删导致的（请求1误删了请求2，导致请求3进来了）。

误删问题产生的根源：锁释放时，没有判断是不是自己的锁。

解决办法：这些请求要获取的锁的key相同，但value可以设置为跟自己线程相关的唯一值。

```java
private static final String ID_PREFIX = UUID.randomUUID() + "-"; // 当请求来到时初始化后就唯一确定了

public boolean tryLock(long timeoutSec) {
    String threadId = String.valueOf(Thread.currentThread().threadId());
    Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX + name, ID_PREFIX + threadId, timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
    return Boolean.TRUE.equals(success);
}

public void unlock() {
    String threadId = ID_PREFIX + Thread.currentThread().threadId();
    String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
    if (threadId.equals(id)) {
        redisTemplate.delete(KEY_PREFIX + name);
    }
}
```

> 问题：删除锁不是原子操作，仍存在误删可能。

误删可能分析：

- 当请求1要删除锁时，已经判断完是自己的锁了，即`threadId.equals(id) == true`，但此时请求1的锁超时释放了；
- 请求1的锁超时释放后，恰巧请求2进来了，它获取了这把锁。然后请求1执行删除逻辑，就误删了请求2的锁。

如果是原子命令：请求1判断是自己的锁则一定会删除自己的锁，不存在“判断完是，结果锁超时释放了“的情况，即锁要么在判断前超时释放，要么等着判断完后被删除。

解决办法：使用Lua脚本。

```java
// 加载脚本
private static final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
static {
    redisScript.setLocation(new ClassPathResource("unlock.lua"));
    redisScript.setResultType(Long.class);
}

public void unlock() {
    // 调用lua脚本进行解锁
    redisTemplate.execute(
            redisScript,
            java.util.Collections.singletonList(KEY_PREFIX + name),
            ID_PREFIX + Thread.currentThread().threadId()
    );
}
```

其中Lua脚本如下：

```lua
-- 这里的 KEYS[1] 就是锁的key，这里的ARGV[1] 就是当前线程标示
-- 获取锁中的标示，判断是否与当前线程标示一致
if (redis.call('GET', KEYS[1]) == ARGV[1]) then
  -- 一致，则删除锁
  return redis.call('DEL', KEYS[1])
end
-- 不一致，则直接返回
return 0
```

现在已经是一个生产级可用的锁了，但也确实还存在一些极端情况的问题。

> 问题：该业务中现在还存在一个问题 --> 锁无法续期。

## 一人一单 - Redisson 分布式锁

基于SETNX命令实现的分布式锁，还存在以下问题：不可重入、不可重试、无法续期、主从导致锁失效等。

Redisson是一个在Redis的基础上实现的Java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的Java常用对象，还提供了许多分布式服务，其中就包含了各种分布式锁的实现。比如：可重入锁、公平锁、联锁（MultiLock）、红锁、读写锁、信号量、闭锁等。

将原有代码改造如下：

先添加Redisson的依赖：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.45.0</version>
</dependency>
```

然后配置RedissonClient：

```java
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        // 创建Redisson配置
        Config config = new Config();
        // 配置单节点模式的Redis服务器地址
        config.useSingleServer().setAddress("redis://127.0.0.1:6378");
        // 创建Redisson客户端实例
        return Redisson.create(config);
    }
}
```

最后把业务代码修改下就可以了：

```java
Long userId = UserContext.getUserId();
// Redisson 获取分布式锁
RLock lock = redissonClient.getLock("voucher:" + voucherId + userId);
boolean isLock = lock.tryLock(); // 获取锁等待时间、锁超时释放时间、时间单位
// 加锁失败
if (!isLock) {
    return Result.error("请勿重复下单");
}
try {
    VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
    return proxy.createVoucherOrderFinalMethod(voucherId);
} finally {
    // 释放锁
    lock.unlock();
}
```

Redisson有以下锁：

| 锁类型                   | 获取方法                                   | 是否可重入 | 是否支持自动续期（无参 lock () 时） | 核心特性与适用场景                                           |
| ------------------------ | ------------------------------------------ | ---------- | ----------------------------------- | ------------------------------------------------------------ |
| 可重入锁（非公平）       | `getLock(String key)`                      | 是         | 是（默认 30 秒过期，每 10 秒续期）  | 最常用的分布式锁，支持重入和自动续期，适合绝大多数分布式互斥场景。 |
| 公平锁                   | `getFairLock(String key)`                  | 是         | 是（同可重入锁机制）                | 按请求顺序获取锁，避免线程饥饿，适合对公平性要求高的场景（性能略低于非公平锁）。 |
| 读写锁（读锁）           | `getReadWriteLock(String key).readLock()`  | 是         | 是（随写锁续期逻辑生效）            | 共享锁，多个线程可同时获取，适合读多写少场景（与写锁互斥）。 |
| 读写锁（写锁）           | `getReadWriteLock(String key).writeLock()` | 是         | 是（同可重入锁机制）                | 排他锁，仅允许一个线程获取，适合写操作场景（与读锁、其他写锁互斥）。 |
| 红锁（`RedLock`）        | `getRedLock(RLock... locks)`               | 是         | 否（必须指定过期时间）              | 基于多个独立 Redis 节点的高可用锁，容忍部分节点故障，适合对一致性要求极高的场景。 |
| 联锁（`MultiLock`）      | `getMultiLock(RLock... locks)`             | 是         | 否（必须指定过期时间）              | 组合多个锁，需全部获取成功才算锁定，适合需要同时锁定多个资源的场景。 |
| 信号量（Semaphore）      | `getSemaphore(String key)`                 | 否         | 否（许可有过期时间，不可续期）      | 基于许可的并发控制，限制同时访问资源的线程数，适合限流场景（如控制并发请求数）。 |
| 可过期信号量             | `getPermitExpirableSemaphore(String key)`  | 否         | 否（许可过期后自动释放）            | 信号量的增强版，每个许可可设置过期时间，适合临时资源分配场景。 |
| 闭锁（`CountDownLatch`） | `getCountDownLatch(String key)`            | 否         | 无续期概念（触发后永久失效）        | 协调多线程同步，等待所有子线程完成后再执行，适合批量任务等待场景（如初始化操作）。 |

以上除了后面三个，其他都是可重试的锁（主要在于`lock()` 方法和`tryLock()` 方法。，前者一般无限重试，后者超时重试）。

`Redisson`可重入锁机制原理（简单版）：

- **锁的存储结构**：`Redisson` 的可重入锁使用 `Redis` 的哈希（`Hash`）数据结构来存储锁信息。其中，键（`Key`）表示锁的名称，字段（`Field`）为线程的唯一标识，值（`Value`）为该线程获取锁的次数。
- **获取锁过程**：当一个线程尝试获取锁时，`Redisson` 会先检查 `Redis` 中是否存在该锁的键。如果不存在，则将该线程的唯一标识作为字段，值设为 1 存入哈希中，表示该线程成功获取锁。如果锁已经存在，会检查字段是否为当前线程的标识，如果是，则将`值加 1`，表示该线程再次获取了锁，实现了可重入。
- **释放锁过程**：当线程释放锁时，会将对应字段的`值减 1`。如果减 1 后的值为 0，则表示该线程已经完全释放了锁，此时会删除 `Redis` 中的该锁键。

`Redisson`自动续期机制原理（`WatchDog` 机制）（简单版）：

- **默认过期时间**：当一个线程获取锁时，如果没有指定锁的过期时间，`Redisson` 会默认给锁设置一个 30 秒的过期时间（可以通过配置修改）。
- **定时任务**：当抢锁完毕后，`Redisson` 会启动一个后台定时任务（`看门狗线程 TimerTask`），该任务会隔一定时间（默认是过期时间的 1/3，即 10 秒）后去检查锁是否还存在，如果存在则会自动延长锁的过期时间，重新设置为 30 秒。并且如果操作成功，那么此时就会递归调用自己，再重新设置一个`TimerTask`，让其10秒后再检查，进而完成锁的续约。
- **任务终止条件**：当线程释放锁时，会停止 `WatchDog` 定时任务，不再进行续期操作。

> 所以现在 不可重入、不可重试、不可续期都解决了，那么主从导致锁失效呢？

在主从模式下，主库用于写操作，所以请求1获取主库的锁，但这时主库宕机了，选了一个从节点晋升为主库，新的主库里没有请求1的锁，请求2再来，也就能获取到锁了。所以在主从模式下，锁可能会失效。

解决办法：将每个节点都看成一样的，获取锁时必须获取到所有节点的锁，也即`Redisson`提出的联锁（`MultiLock`）。只有所有的服务器都写入成功，才算是加锁成功，假设现在某个节点挂了，那么获得锁时，只要有一个节点拿不到，就不能加锁成功，这保证了加锁的可靠性。

原理总结：当调用 `multiLock.lock()` 或 `multiLock.tryLock(...)` 时，内部执行以下步骤：

遍历子锁列表，依次调用每个子锁的 `tryLock` 方法（带短暂超时，默认 100 ms），尝试获取锁。若所有子锁都获取成功，则联锁获取成功，进入业务逻辑。若有任何一个子锁获取失败，则立即释放已获取的所有子锁（避免部分锁定导致死锁），然后重新进入循环重试（无参 `lock()` 会无限重试，`tryLock` 会在超时前重试）。重试时会优化顺序：优先尝试获取上一次失败的子锁，减少无效遍历（Redisson 内部通过记录失败位置实现）。

# 问题排查

## 🚨 Spring Boot 返回 406

问题表现： 接口返回 `406 Not Acceptable`，日志提示：

```
HttpMediaTypeNotAcceptableException: No acceptable representation
```

根因：返回对象 `Result<T>` 没有 getter/setter，导致 Jackson 无法序列化为 JSON，从而直接触发 406。

解决：给返回类加上 Lombok 的 `@Data`（或自己写 getter/setter）：

```java
@Data
public class Result<T> {
    private boolean success;
    private String errorMsg;
    private T data;
}
```

> 返回对象不能序列化 → JSON 生成失败 → 406。给类加 getters 即可解决。
