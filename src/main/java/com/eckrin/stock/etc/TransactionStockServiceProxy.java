package com.eckrin.stock.etc;

import com.eckrin.stock.service.StockService;

public class TransactionStockServiceProxy {

    private StockService stockService;

    public TransactionStockServiceProxy(StockService stockService) {
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) {
        startTransaction();

        stockService.decreaseWithoutTx(id, quantity);

        endTransaction();
    }

    private void startTransaction() {
        // 트랜잭션 시작
    }

    private void endTransaction() {
        // 트랜잭션 커밋
    }
}
