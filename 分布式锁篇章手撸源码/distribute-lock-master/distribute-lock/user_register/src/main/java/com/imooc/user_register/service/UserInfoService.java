package com.imooc.user_register.service;

import com.imooc.user_register.dao.UserInfoDao;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService {

    @Autowired
    private UserInfoDao userInfoDao;

    @Autowired
    private RedissonClient redissonClient;

    public String register(String phone, String name) {
        RLock lock = redissonClient.getMultiLock(redissonClient.getLock("lock" + phone), redissonClient.getLock("lock" + name));
        lock.lock();
        try {
            //根据手机号查询库，判断是否存在相同手机号的账号
            Integer phoneCount = userInfoDao.selectByPhone(phone);
            Integer nameCount = userInfoDao.selectByName(name);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (phoneCount > 0) {
                return "手机号已经存在，不允许注册。";
            }
            if (nameCount > 0) {
                return "用户名已经存在，不允许注册。";
            }
            //进行注册，插入数据库
            userInfoDao.insert(phone, name);
            return "手机号注册成功。";
        } catch (DuplicateKeyException e) {
            return "手机号已经存在，不允许注册。";
        } finally {
            lock.unlock();
        }
    }
}
