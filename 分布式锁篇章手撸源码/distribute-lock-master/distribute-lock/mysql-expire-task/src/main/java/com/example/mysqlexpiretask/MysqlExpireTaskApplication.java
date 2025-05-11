package com.example.mysqlexpiretask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MysqlExpireTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysqlExpireTaskApplication.class, args);
    }

}
