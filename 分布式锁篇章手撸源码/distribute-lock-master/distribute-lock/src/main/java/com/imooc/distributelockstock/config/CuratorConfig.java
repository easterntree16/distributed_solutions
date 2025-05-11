package com.imooc.distributelockstock.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorConfig {

    @Bean
    public CuratorFramework curatorFramework(){
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
        CuratorFramework curatorFramework = CuratorFrameworkFactory
                .newClient("127.0.0.1:2181", retryPolicy);
        curatorFramework.start();
        return curatorFramework;
    }
}
