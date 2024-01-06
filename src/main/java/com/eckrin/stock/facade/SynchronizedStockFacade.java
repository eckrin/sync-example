package com.eckrin.stock.facade;

import com.eckrin.stock.repository.LockRepository;
import com.eckrin.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SynchronizedStockFacade {

    private final StockService stockService;

    public synchronized void decrease(Long id, Long quantity) {
        stockService.decreaseWithTransactional(id, quantity);
    }
}
