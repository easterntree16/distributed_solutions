package com.imooc.distributelockstock.lock;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ExecutionException;

public class EtcdLock extends AbstractLock {

    private Client client;
    private Long DEFAULT_LEASE = 20L;
    private Long leaseId;
    private String lockPath;

    public EtcdLock(Client client, String lockName) {
        this.client = client;
        this.lockName = lockName;
    }

    @Override
    public void lock() {
        //创建租约
        createLease();
        //创建锁
        createLock();
    }

    private void createLease() {
        Lease leaseClient = client.getLeaseClient();
        try {
            LeaseGrantResponse leaseGrantResponse = leaseClient.grant(DEFAULT_LEASE).get();
            this.leaseId = leaseGrantResponse.getID();

            StreamObserver<LeaseKeepAliveResponse> observer = new StreamObserver<LeaseKeepAliveResponse>() {
                @Override
                public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            };
            //对lease租约自动续期的api
            leaseClient.keepAlive(leaseId, observer);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void createLock() {
        Lock lockClient = client.getLockClient();
        try {
            LockResponse lockResponse = lockClient.lock(ByteSequence.from(this.lockName.getBytes()), this.leaseId).get();
            if (lockResponse != null) {
                this.lockPath = lockResponse.getKey().toString();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unlock() {
        //第一释放锁
        if (this.lockPath != null) {
            try {
                client.getLockClient().unlock(ByteSequence.from(this.lockPath.getBytes())).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        //第二将lease进行revoke
        if (this.leaseId != null) {
            try {
                client.getLeaseClient().revoke(this.leaseId).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
