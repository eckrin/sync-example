package com.eckrin.stock.service;

import com.eckrin.stock.domain.Stock;
import com.eckrin.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

//    @Transactional
    public synchronized void decrease(Long id, Long quantity) {
        // Stock 조회, 재고 감소후 갱신값 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
