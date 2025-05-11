--商品库存表--
CREATE TABLE `goods_stock`
(
    `id`       bigint NOT NULL AUTO_INCREMENT,
    `goods_id` bigint NOT NULL,
    `stock`    int    NOT NULL,
    `version`  int DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY        `goods_stock_goods_id_index` (`goods_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品库存表'

--mysql悲观锁实现分布式锁--
CREATE TABLE `distribute_lock`
(
    `id`        int NOT NULL AUTO_INCREMENT,
    `lock_name` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY         `k_lock_name` (`lock_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

--mysql唯一健实现分布式锁--
CREATE TABLE `record_lock`
(
    `id`          int NOT NULL AUTO_INCREMENT,
    `lock_name`   varchar(100) DEFAULT NULL,
    `modify_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `uuid`        varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `record_lock_lock_name_uindex` (`lock_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--用户信息表--
CREATE TABLE `user_info`
(
    `id`    int NOT NULL AUTO_INCREMENT,
    `phone` varchar(30) DEFAULT NULL,
    `name`  varchar(30) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_info_phone_uindex` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci