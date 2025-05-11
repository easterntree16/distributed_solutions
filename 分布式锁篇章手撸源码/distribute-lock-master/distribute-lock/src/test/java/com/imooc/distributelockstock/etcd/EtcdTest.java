package com.imooc.distributelockstock.etcd;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class EtcdTest {

    public Client client;

    @Before
    public void init() {
        client = Client.builder()
                .endpoints("http://127.0.0.1:2379").build();
    }

    @Test
    public void testPut() throws ExecutionException, InterruptedException {
        KV kvClient = client.getKVClient();
        CompletableFuture<PutResponse> result = kvClient.put(ByteSequence.from("key1", StandardCharsets.UTF_8),
                ByteSequence.from("value1", StandardCharsets.UTF_8));
        Assert.notNull(result.get().getHeader(), "不能为空");

        Lease leaseClient = client.getLeaseClient();
        CompletableFuture<LeaseGrantResponse> resultLease = leaseClient.grant(30);
        long leaseId = resultLease.get().getID();
        PutOption putOption = PutOption.newBuilder().withLeaseId(leaseId).build();
        kvClient.put(ByteSequence.from("key2", StandardCharsets.UTF_8),
                ByteSequence.from("value2", StandardCharsets.UTF_8), putOption);
    }

    @Test
    public void grant() throws ExecutionException, InterruptedException {
        Lease leaseClient = client.getLeaseClient();
        CompletableFuture<LeaseGrantResponse> result = leaseClient.grant(30);
        Assert.isTrue(result.get().getID() > 0, "不能为空");
    }

    @Test
    public void timeToLive() throws ExecutionException, InterruptedException {
        Lease leaseClient = client.getLeaseClient();
        CompletableFuture<LeaseGrantResponse> result = leaseClient.grant(30);
        LeaseOption leaseOption = LeaseOption.newBuilder().build();
        CompletableFuture<LeaseTimeToLiveResponse> resultLive = leaseClient.timeToLive(result.get().getID(), leaseOption);
        System.out.println("ttl:" + resultLive.get().getTTl());
        Assert.isTrue(resultLive.get().getTTl() > 0, "不能为空");
    }

    @Test
    public void get() throws ExecutionException, InterruptedException {
        KV kvClient = client.getKVClient();
        CompletableFuture<GetResponse> result = kvClient.get(ByteSequence.from("key1", StandardCharsets.UTF_8));
        Assert.isTrue(result.get().getCount() == 1, "不能为空");

        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        CompletableFuture<GetResponse> result1 = kvClient.get(ByteSequence.from("key", StandardCharsets.UTF_8), getOption);
        Assert.isTrue(result1.get().getCount() == 1, "不能为空");
    }

    Watch.Listener listener = new Watch.Listener() {
        @Override
        public void onNext(WatchResponse watchResponse) {
            System.out.println("onNext");
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    };

    @Test
    public void watch() throws ExecutionException, InterruptedException {
        Watch watchClient = client.getWatchClient();
        watchClient.watch(ByteSequence.from("key1", StandardCharsets.UTF_8), listener);

        WatchOption watchOption = WatchOption.newBuilder().isPrefix(true).build();
        watchClient.watch(ByteSequence.from("key", StandardCharsets.UTF_8), watchOption, listener);
        Thread.sleep(1000000);
    }
}
