package com.eckrin.stock.service;

import com.eckrin.stock.domain.Stock;
import com.eckrin.stock.repository.StockRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void init() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 재고감소() {
        stockService.decrease(1L, 1L);

        Stock ex = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(ex.getQuantity()).isEqualTo(99);
    }

    @Test
    public void 동시요청() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isNotEqualTo(0); // race condition으로 인하여 원하는 결과가 나오지 않음
    }
}