package com.eckrin.stock.service;

import com.eckrin.stock.domain.Stock;
import com.eckrin.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decreaseWithSerializable(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }

    public void decreaseWithRetry(Long id, Long quantity) {
        int maxRetries = 300;
        int retryCount = 0;
        boolean success = false;

        while (!success && retryCount < maxRetries) {
            try {
                decreaseWithSerializable(id, quantity);
                success = true;
            } catch (OptimisticLockingFailureException ex) {
                retryCount++;
                if (retryCount == maxRetries) {
                    throw ex;
                }
            }
        }
    }

    public synchronized void decreaseWithoutTx(Long id, Long quantity) {
        // Stock 조회, 재고 감소후 갱신값 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }

    @Transactional
    public void decreaseWithTx(Long id, Long quantity) {
        // Stock 조회, 재고 감소후 갱신값 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 부모 트랜잭션과 별도 Datasource 사용
    public void decreaseWithTxRequiresNew(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
