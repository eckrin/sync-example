package com.eckrin.stock.facade;

import com.eckrin.stock.repository.LockRepository;
import com.eckrin.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NamedLockStockFacade {

    private final LockRepository lockRepository;
    private final StockService stockService;

    @Transactional
    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(id.toString()); // 네임드 락 얻어오기
            stockService.decreaseWithTx(id, quantity); // Requires_new로 하지 않으면 무한루프
        } finally {
            lockRepository.releaseLock(id.toString()); // 네임드 락 해제
        }
    }
}
