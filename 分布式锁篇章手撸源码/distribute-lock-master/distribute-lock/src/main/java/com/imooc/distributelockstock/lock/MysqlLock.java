package com.imooc.distributelockstock.lock;

import com.imooc.distributelockstock.dao.DistributeLockDao;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.UUID;

public class MysqlLock extends AbstractLock {

    private DistributeLockDao dao;
    private DataSourceTransactionManager dataSourceTransactionManager;
    private TransactionStatus status;
    private String uuid;

    public MysqlLock(DistributeLockDao dao, String lockName, DataSourceTransactionManager dataSourceTransactionManager) {
        this.dao = dao;
        this.lockName = lockName;
        this.dataSourceTransactionManager = dataSourceTransactionManager;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public void lock() {
        recordLock();
    }

    private void recordLock() {
        while (true) {
            try {
                Integer result = dao.insert(this.lockName, this.uuid);
                if (result > 0) {
                    break;
                }
            } catch (Exception e) {
                //唯一索引校验导致insert失败
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void forUpdateLock() {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        status = dataSourceTransactionManager.getTransaction(transactionDefinition);
        while (true) {
            try {
                //加锁就是执行for update
                dao.queryLockNameForUpdate(this.lockName);
                break;
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public void unlock() {
//        dataSourceTransactionManager.commit(status);
        dao.delete(this.lockName, this.uuid);
    }
}
