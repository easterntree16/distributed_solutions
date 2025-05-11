--简易锁的加锁lua
--判断一下当前是否存在这把锁，如果锁存在，那么就直接加锁失败，如果不存在锁，那么我就set一个锁，并且给锁一个过期时间
if(redis.call('exists', KEYS[1]) == 0) then
    redis.call('set', KEYS[1], ARGV[1])
    redis.call('pexpire', KEYS[1], ARGV[2])
    return 1;
else
    return 0;
end
--eval "if(redis.call('exists', KEYS[1]) == 0) then redis.call('set', KEYS[1], ARGV[1]) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else return 0;  end" 1 lockName uuid 30000

--简易锁的释放锁lua
--判断锁是否存在，如果不存在，直接return，如果存在，那么我需要判断uuid是否等于本线程的uui，如果等于，那么就del，如果不等于就return
if(redis.call('exists', lockName)== 0) then
    return 0;
end
if (redis.call('get', lockName) == uuid) then
    redis.call('del', lockName)
    return 1;
else
    return 0;
end

--eval "if(redis.call('exists', KEYS[1])== 0) then return 0; end if (redis.call('get', KEYS[1]) == ARGV[1]) then redis.call('del', KEYS[1]) return 1; else return 0; end" 1 lockName uuid

--hash key = lockName key = uuid  value = 加锁次数 1 + 1 =2 + 1 =3 3-1 =2 -1 = 1 -1 = 0
--可重入锁的加锁lua脚本
--判断锁是否存在，如果锁不存在，那直接加锁就可以，把重入次数设置1，给锁加一个过期时间
--锁存在，锁的uuid是否是本线程，如果是，那么可重入次数+1，并且锁给一个新的过期时间，如果不是，那么就加锁失败。
if(redis.call('exists', KEYS[1]) == 0) then
    redis.call('hincrby', KEYS[1], ARGV[1], 1)
    redis.call('pexpire', KEYS[1], ARGV[2])
    return 1;
end
if (redis.call('hexists',KEYS[1], ARGV[1]) == 1) then
    redis.call('hincrby', KEYS[1], ARGV[1], 1)
    redis.call('pexpire', KEYS[1], ARGV[2])
    return 1;
else
    return 0;
end

--eval "if(redis.call('exists', KEYS[1]) == 0) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; end if (redis.call('hexists',KEYS[1], ARGV[1]) == 1) then redis.call('hincrby', KEYS[1], ARGV[1], 1) redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else return 0; end" 1 lockName uuid 30000
--可重入锁的释放锁lua脚本
--判断当前持有锁的线程是不是本线程，不是的话，就不需要释放了，
--如果是，那么就对重入次数-1，-1之后判断值是否是>0，如果大于还持有锁，那么就设置一个新的过期时间，如果不>0，那么就可以删除锁了
if(redis.call('hexists', lockName, uuid) == 0) then
    return 0;
end
local lockCount = redis.call('hincrby', lockName, uuid, -1)
if(lockCount > 0) then
    redis.call('pexpire', lockName, 30000)
    return 1;
else
    redis.call('del', lockName)
    return 1;
end

--eval "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return 0; end local lockCount = redis.call('hincrby', KEYS[1], ARGV[1], -1) if(lockCount > 0) then redis.call('pexpire', KEYS[1], ARGV[2]) return 1; else redis.call('del', KEYS[1]) return 1; end" 1 lockName uuid 30000

--lua脚本实现自动续期
--判断当前持有锁的线程是不是本线程，如果是就进行锁续期，如果不是，那么就return
if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return 0;
else
    redis.call('pexpire', KEYS[1], ARGV[2])
    return 1;
end

--eval "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return 0; else redis.call('pexpire', KEYS[1], ARGV[2]) return 1; end" 1 lockName uuid 30000


