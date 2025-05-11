package com.imooc.distributelockstock.controller;

import com.imooc.distributelockstock.service.StockSDKService;
import com.imooc.distributelockstock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockSDKService stockSDKService;

    @GetMapping("/stock/deductStock/{goodsId}/{count}")
    public  String deductStock(@PathVariable Long goodsId, @PathVariable Integer count) {
        return stockSDKService.deductStockSDKLock(goodsId, count);
    }
}
