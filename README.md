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
