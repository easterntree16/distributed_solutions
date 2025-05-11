package com.imooc.distributelockstock.service;

import com.imooc.distributelockstock.dao.DistributeLockDao;
import com.imooc.distributelockstock.dao.StockDao;
import com.imooc.distributelockstock.entity.GoodsStockEntity;
import com.imooc.distributelockstock.lock.*;
import io.etcd.jetcd.Client;
import io.netty.util.internal.StringUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.ZooKeeper;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

//@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Service
public class StockService {
    @Autowired
    private StockDao stockDao;

    @Autowired
    private StringRedisTemplate template;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private DistributeLockDao distributeLockDao;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private Client client;

    private ZooKeeper zooKeeper;

    StockService() {
        try {
            zooKeeper = new ZooKeeper("127.0.0.1:2181", 10000, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //    @Transactional
    public synchronized String deductStock(Long goodsId, Integer count) {
        //1、查询商品库存的库存数量
        Integer stock = stockDao.selectStockByGoodsId(goodsId);

        //2、判断库存数量是否足够
        if (stock < count) {
            return "库存不足";
        }
        //3、如果库存数量足够，那么就去扣减库存
        stockDao.updateStockByGoodsId(goodsId, stock - count);
        return "库存扣减成功";
    }

    public String deductStockOneUpdate(Long goodsId, Integer count) {
        Integer result = stockDao.updateStockByGoodsIdAndCount(goodsId, count);
        if (result > 0) {
            return "库存扣减成功";
        }
        return "库存不足";
    }

    @Transactional
    public synchronized String deductStockForUpdate(Long goodsId, Integer count) {
        //1、查询商品库存的库存数量
        Integer stock = stockDao.selectStockByGoodsIdForUpdate(goodsId);

        //2、判断库存数量是否足够
        if (stock < count) {
            return "库存不足";
        }
        //3、如果库存数量足够，那么就去扣减库存
        stockDao.updateStockByGoodsId(goodsId, stock - count);
        return "库存扣减成功";
    }

    public synchronized String deductStockCAS(Long goodsId, Integer count) {
        Integer result = 0;
        while (result == 0) {
            //1、查询商品库存的库存数量 + version
            List<GoodsStockEntity> goodsStockEntities = stockDao.selectStockAndVersionByGoodsId(goodsId);
            if (CollectionUtils.isEmpty(goodsStockEntities)) {
                return "商品不存在";
            }
            GoodsStockEntity goodsStockEntity = goodsStockEntities.get(0);
            //2、判断库存数量是否足够
            if (goodsStockEntity.getStock() < count) {
                return "库存不足";
            }
            //3、如果库存数量足够，那么就去扣减库存
            result = stockDao.updateStockAndVersionByGoodsIdAndVersion(goodsId,
                    goodsStockEntity.getStock() - count, goodsStockEntity.getVersion());
        }
        return "库存扣减成功";
    }

    public String deductStockRedis(Long goodsId, Integer count) {
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
    }

    public String deductStockRedisLock(Long goodsId, Integer count) {
        AbstractLock lock = null;
        try {
            lock = new RedisLock(template, "lock" + goodsId);
            boolean result = lock.tryLock(5000, TimeUnit.MILLISECONDS);
            if (result) {
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
            }
            System.out.println("获取锁超时");
            return "系统繁忙";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public String deductStockRedisRedisson(Long goodsId, Integer count) {
        RLock lock = null;
        try {
            lock = redissonClient.getLock("lock" + goodsId);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void lock(Lock lock) {
        lock.lock();
        lock.unlock();
    }

    public String deductStockZookeeperLock(Long goodsId, Integer count) {
        AbstractLock lock = null;
        try {
            lock = new ZookeeperLock(zooKeeper, "lock" + goodsId);
            lock.lock();
            lock(lock);
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

    public String deductStockCurator(Long goodsId, Integer count) {
        InterProcessMutex lock = null;
        try {
            lock = new InterProcessMutex(curatorFramework, "/" + "lock" + goodsId);
            lock.acquire();
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String deductStockMysqlLock(Long goodsId, Integer count) {
        AbstractLock lock = null;
        try {
            lock = new MysqlLock(distributeLockDao, "lock" + goodsId, dataSourceTransactionManager);
            lock.lock();
//            lock(lock);
//            Thread.sleep(5000);
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

    public String deductStockEtcdLock(Long goodsId, Integer count) {
        AbstractLock lock = null;
        try {
            lock = new EtcdLock(client, "lock" + goodsId);
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
