package com.imooc.distributelockstock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {"com.imooc.distributelockstock.dao"
        , "com.example.lockspringbootstarter.dao"})
public class DistributeLockStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributeLockStockApplication.class, args);
    }

}
