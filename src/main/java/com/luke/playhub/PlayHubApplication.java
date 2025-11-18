package com.luke.playhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.luke.playhub.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
public class PlayHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlayHubApplication.class, args);
    }

}
