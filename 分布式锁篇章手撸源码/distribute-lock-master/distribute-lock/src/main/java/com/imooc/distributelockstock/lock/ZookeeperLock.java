package com.imooc.distributelockstock.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZookeeperLock extends AbstractLock {

    private ZooKeeper zooKeeper;
    private String uuid;
    private String lockPath;
    //key 代表的是加锁的线程， value代表的是加锁的次数
    private final ConcurrentHashMap<Thread, Integer> threadLockCountMap = new ConcurrentHashMap();

    public ZookeeperLock(ZooKeeper zooKeeper, String lockName) {
        this.zooKeeper = zooKeeper;
        this.lockName = lockName;
        this.uuid = UUID.randomUUID().toString();
    }

    Watcher watcher = watchedEvent -> {
        synchronized (ZookeeperLock.this) {
            System.out.println("来唤醒了");
            ZookeeperLock.this.notify();
        }
    };

    @Override
    public void lock() {
        //普通的临时节点+watch的实现
//        watchLock();

        if (threadLockCountMap.containsKey(Thread.currentThread())) {
            Integer lockCount = threadLockCountMap.get(Thread.currentThread());
            threadLockCountMap.put(Thread.currentThread(), ++lockCount);
            return;
        }
        //临时顺序节点+watch的实现
        seqWatchLock();
        threadLockCountMap.put(Thread.currentThread(), 1);
    }

    private void seqWatchLock() {
        //1、首先去创建一个临时顺序节点
        try {
            String node = zooKeeper.create(lockPath(), uuid.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            isFirstNode(node);
            lockPath = node;
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void isFirstNode(String node) {
        while (true) {
            try {
                //获取分布式锁父节点下，所有客户端创建的锁节点
                List<String> children = zooKeeper.getChildren(getBasePath(), null);
                Collections.sort(children);
                //需要判断我们本客户端创建的节点是在这个集合中，是否是第一个
                String[] split = node.split("/");
                int i = children.indexOf(split[split.length - 1]);
                if (i > 0) {
                    synchronized (this) {
                        //如果大于0,说明本客户端创建的节点锁不是所有排队里面的第一个
                        zooKeeper.getData(getBasePath() + "/" + children.get(i - 1), watcher, new Stat());
                        wait();
                    }
                }
                break;
            } catch (KeeperException | InterruptedException e) {
            }
        }
    }

    private void watchLock() {
        while (true) {
            try {
                //客户端A加锁成功，然后客户端B来加锁，后面又来了一个客户C来加锁。
                String path = zooKeeper.create(lockPath(), uuid.getBytes()
                        , ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                if (lockPath().equals(path)) {
                    break;
                }
            } catch (KeeperException e) {
                try {
                    synchronized (this) {
                        zooKeeper.getData(lockPath(), watcher, new Stat());
                        wait();
                    }
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    @Override
    public void unlock() {
        try {
            if (threadLockCountMap.containsKey(Thread.currentThread())) {
                Integer lockCount = threadLockCountMap.get(Thread.currentThread());
                if (--lockCount > 0) {
                    threadLockCountMap.put(Thread.currentThread(), lockCount);
                    return;
                }
                threadLockCountMap.remove(Thread.currentThread());
            }
//            byte[] data = zooKeeper.getData(lockPath(), null, new Stat());
            byte[] data = zooKeeper.getData(lockPath, null, new Stat());
            if (uuid.equals(new String(data))) {
//                zooKeeper.delete(lockPath(), -1);
                zooKeeper.delete(lockPath, -1);
            }
        } catch (Exception ignored) {
        }
    }

    private String getBasePath() {
        return "/" + "distribute-lock";
    }

    private String lockPath() {
        return getBasePath() + "/" + lockName;
    }
}
