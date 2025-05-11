package com.imooc.distributelockstock.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock extends AbstractLock {
    private StringRedisTemplate stringRedisTemplate;
    private final long defaultExpireTime = 30000;
    private final String uuid;
    private Long expireTime = 0L;

    public RedisLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public void lock() {
        lock(TimeUnit.MILLISECONDS, defaultExpireTime);
    }

    @Override
    public void lock(TimeUnit timeUnit, Long expireTime) {
        this.expireTime = expireTime;
        while (true) {
            //使用lua脚本加锁
//            String luaScript = "if(redis.call('exists', KEYS[1]) == 0) then redis.call('set', KEYS[1], ARGV[1]) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else return 0;  end";
            String luaScript = "if(redis.call('exists', KEYS[1]) == 0) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; end if (redis.call('hexists',KEYS[1], ARGV[1]) == 1) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else return 0; end";
            Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class)
                    , Collections.singletonList(this.lockName),
                    uuid,
                    expireTime.toString());
            //1、去使用setnx指令进行加锁
//            Boolean result = stringRedisTemplate.opsForValue()
//                    .setIfAbsent(this.lockName, uuid, expireTime, timeUnit);
//            if (result != null && result){
            if (result != null && result.equals(1L)) {
                new Thread(() -> {
                    while (true) {
                        String expireLua = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return 0; else redis.call('pexpire', KEYS[1], ARGV[2]) return 1; end";
                        Long expireResult = stringRedisTemplate.execute(new DefaultRedisScript<>(expireLua, Long.class)
                                , Collections.singletonList(this.lockName),
                                uuid,
                                expireTime.toString());
                        if (expireResult == null || expireResult.equals(0L)) {
                            break;
                        }

                        try {
                            Thread.sleep(expireTime / 2);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean tryLockInternal(TimeUnit timeUnit, Long expireTime) {
        this.expireTime = expireTime;
        String luaScript = "if(redis.call('exists', KEYS[1]) == 0) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; end if (redis.call('hexists',KEYS[1], ARGV[1]) == 1) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else return 0; end";
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class)
                , Collections.singletonList(this.lockName),
                uuid,
                expireTime.toString());
        if (result != null && result.equals(1L)) {
            new Thread(() -> {
                while (true) {
                    String expireLua = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return 0; else redis.call('pexpire', KEYS[1], ARGV[2]) return 1; end";
                    Long expireResult = stringRedisTemplate.execute(new DefaultRedisScript<>(expireLua, Long.class)
                            , Collections.singletonList(this.lockName),
                            uuid,
                            expireTime.toString());
                    if (expireResult == null || expireResult.equals(0L)) {
                        break;
                    }

                    try {
                        Thread.sleep(expireTime / 2);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
            return true;
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return tryLock(time, defaultExpireTime, unit);
    }

    @Override
    public boolean tryLock(long time, long expireTime, TimeUnit unit) throws InterruptedException {
        //先记录一个开始时间，然后再记录一个当前时间，拿当前时间-开始时间 > time,那么就跳出循环，否则呢，就一直等待获取锁
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        boolean lockResult = false;
        while (currentTime - startTime <= time) {
            //获取锁
            boolean result = tryLockInternal(unit, expireTime);
            if (result) {
                lockResult = result;
                break;
            }
            currentTime = System.currentTimeMillis();
        }
        return lockResult;
    }

    @Override
    public void unlock() {
        //释放锁lua脚本
//        String luaScript = "if(redis.call('exists', KEYS[1])== 0) then return 0; end if (redis.call('get', KEYS[1]) == ARGV[1]) then redis.call('del', KEYS[1]) return 1; else return 0; end";
        String luaScript = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return 0; end local lockCount = redis.call('hincrby', KEYS[1], ARGV[1], -1) if(lockCount > 0) then redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else redis.call('del', KEYS[1]) return 1; end";
        stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class)
                , Collections.singletonList(this.lockName), uuid, expireTime.toString());
//        //判断当前持有锁线程是否等于本线程，是则删除锁，不是直接return
//        String result = stringRedisTemplate.opsForValue().get(this.lockName);
//        if (this.uuid.equals(result)){
//            stringRedisTemplate.delete(this.lockName);
//        }
    }
}
