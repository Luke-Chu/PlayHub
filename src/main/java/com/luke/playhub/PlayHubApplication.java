package com.luke.playhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.luke.playhub.mapper")
@SpringBootApplication
public class PlayHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlayHubApplication.class, args);
    }

}
