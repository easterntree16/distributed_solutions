package com.imooc.user_register.controller;

import com.imooc.user_register.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping("/register")
    public String register(String phone, String name) {
        return userInfoService.register(phone, name);
    }
}
