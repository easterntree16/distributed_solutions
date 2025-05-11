package com.example.mysqlexpiretask.task;

import com.example.mysqlexpiretask.dao.RecordLockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class AutoExpireTask {

    @Autowired
    private RecordLockDao recordLockDao;

    @Scheduled(cron = "*/5 * * * * ?")
    public void autoExpire() {
        //首先就是从库里面查询出来已经过期的分布式锁
        List<Integer> idList = recordLockDao.selectExpireLock(getExpireTime());
        //然后就是删除对应的分布式锁
        if (!CollectionUtils.isEmpty(idList)) {
            idList.forEach(id -> {
                System.out.println("删除的分布式锁id=" + id);
                recordLockDao.delete(id);
            });
        }
    }

    private String getExpireTime() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, -10);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(calendar.getTime());
    }
}
