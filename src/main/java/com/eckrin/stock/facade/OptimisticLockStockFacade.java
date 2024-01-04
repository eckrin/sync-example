package com.eckrin.stock.facade;

import com.eckrin.stock.service.OptimisticLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OptimisticLockStockFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    // update 쿼리 실패를 대비한 facade 클래스
    public void decrease(Long id, Long quantity) throws InterruptedException {
        while(true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break; // update쿼리가 성공시
            } catch(Exception e) { // update 실패시
                Thread.sleep(50); // 50ms후 재시도
            }
        }
    }
}
