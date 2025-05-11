package com.imooc.distributelockstock.service;

import com.example.lockspringbootstarter.lock.AbstractLock;
import com.example.lockspringbootstarter.lock.manager.LockManager;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

//@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Service
public class StockSDKService {
    @Autowired
    private StringRedisTemplate template;

    @Autowired
    private LockManager lockManager;

    public String deductStockSDKLock(Long goodsId, Integer count) {
        AbstractLock lock = null;
        try {
            lock = lockManager.getLock("lock" + goodsId);
            lock.lock();
            //1、查询商品库存的库存数量
            String stock = template.opsForValue().get("stock" + goodsId);
            if (StringUtil.isNullOrEmpty(stock)) {
                return "商品不存在";
            }
            Integer lastStock = Integer.parseInt(stock);
            //2、判断库存数量是否足够
            if (lastStock < count) {
                return "库存不足";
            }
            //3、如果库存数量足够，那么就去扣减库存
            template.opsForValue().set("stock" + goodsId, String.valueOf(lastStock - count));
            return "库存扣减成功";
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
