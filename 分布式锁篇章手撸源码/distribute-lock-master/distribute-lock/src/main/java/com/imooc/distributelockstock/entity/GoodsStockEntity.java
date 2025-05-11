package com.imooc.distributelockstock.entity;

import lombok.Data;

@Data
public class GoodsStockEntity {
    private Long id;

    private Integer stock;

    private Integer version;
}
