package com.imooc.distributelockstock.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@Slf4j
@SpringBootTest
public class ZookeeperTest {
    ZooKeeper zooKeeper;

    @Before
    public void before() throws IOException {
        zooKeeper = new ZooKeeper("127.0.0.1:2181", 30000, null);
    }

    //1、调用zookeeper创建节点
    @Test
    public void testCreateNode() throws IOException, InterruptedException, KeeperException {
        String result = zooKeeper.create("/javaClient", "javaValue".getBytes()
                , ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        Assert.assertTrue("/javaClient".equals(result));
    }

    @Test
    public void testGetNode() throws IOException, InterruptedException, KeeperException {
        Stat stat = new Stat();
        byte[] data = zooKeeper.getData("/javaClient", null, stat);
        log.info("result:{}", new String(data));
        Assert.assertTrue("javaValue".equals(new String(data)));
    }

    @Test
    public void testExistsNode() throws IOException, InterruptedException, KeeperException {
        Stat stat = zooKeeper.exists("/javaClient", null);
        if (stat == null){
            String result = zooKeeper.create("/javaClient", "javaValue".getBytes()
                    , ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            log.info("create result:{}", result);
        } else {
            byte[] data = zooKeeper.getData("/javaClient", null, stat);
            log.info("get result:{}", new String(data));
        }
    }

    @Test
    public void testSetNode() throws IOException, InterruptedException, KeeperException {
        zooKeeper.setData("/javaClient", "updateData".getBytes(), -1);
    }

    @Test
    public void testDeleteNode() throws IOException, InterruptedException, KeeperException {
        zooKeeper.delete("/javaClient", -1);
    }
}
