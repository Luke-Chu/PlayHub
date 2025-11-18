# PlayHub 趣桌玩汇

单独做一些关键技术难点，以及测试。



## 不解决超卖问题

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

## 版本号解决超卖

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

## 库存大于零解决超卖

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

## 一人一单初版

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

但这个代码还是存在问题，当前方法被spring的事务控制，如果在方法内部加锁，可能会导致当前方法事务还没有提交，但是锁已经释放，这也会导致问题，将当前方法整体包裹起来，确保事务不会出现问题。

```java
Long userId = UserContext.getUserId();
synchronized (userId.toString().intern()) {
    return createVoucherOrderFinalMethod(voucherId);
}
```

但是以上做法依然有问题，因为调用的方法，其实是`this.`的方式调用的，事务想要生效，还得利用代理来生效，所以需要获得原始的事务对象， 来操作事务。

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
