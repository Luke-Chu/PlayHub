# PlayHub 趣桌玩汇

单独做一些关键技术难点，以及测试。





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
