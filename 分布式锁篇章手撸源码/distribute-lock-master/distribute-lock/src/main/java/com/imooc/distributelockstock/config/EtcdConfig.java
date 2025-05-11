package com.imooc.distributelockstock.config;

import io.etcd.jetcd.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtcdConfig {

    @Bean
    public Client client(){
        return Client.builder().endpoints("http://127.0.0.1:2379").build();
    }
}
