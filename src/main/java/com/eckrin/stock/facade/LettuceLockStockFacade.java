package com.eckrin.stock.facade;

import com.eckrin.stock.repository.RedisLockRepository;
import com.eckrin.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade {

    private final RedisLockRepository redisLockRepository;
    private final StockService stockService;

    public void decrease(Long key, Long quantity) throws InterruptedException {
        while(!redisLockRepository.lock(key)) { // spinlock 직접 구현 (락을 얻을때까지 sleep 반복)
            Thread.sleep(100);
        }

        try {
            stockService.decreaseWithTx(key, quantity);
        } finally {
            redisLockRepository.unlock(key);
        }
    }
}
