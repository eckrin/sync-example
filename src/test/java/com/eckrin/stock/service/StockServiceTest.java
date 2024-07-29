package com.eckrin.stock.service;

import com.eckrin.stock.domain.Stock;
import com.eckrin.stock.facade.*;
import com.eckrin.stock.repository.StockRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
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
    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;
    @Autowired
    private SynchronizedStockFacade synchronizedStockFacade;
    @Autowired
    private OptimisticLockStockFacade optimisticLockStockFacade;
    @Autowired
    private NamedLockStockFacade namedLockStockFacade;
    @Autowired
    private LettuceLockStockFacade lettuceLockStockFacade;
    @Autowired
    private RedissonLockStockFacade redissonLockStockFacade;

    @BeforeEach
    public void init() {
        stockRepository.saveAndFlush(new Stock(1L, 1L, 100L));
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAllInBatch();
    }

    @Test
    public void 재고감소() {
        stockService.decreaseWithoutTx(1L, 1L);

        Stock ex = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(ex.getQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("트랜잭션 고립 수준 변경")
    public void 동시요청_고립수준_isolation() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseWithSerializable(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0); // race condition으로 인하여 원하는 결과가 나오지 않음
    }

    @Test
    @DisplayName("트랜잭션 고립 수준 변경 + 재시도")
    public void 동시요청_고립수준_isolationWithRetry() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseWithRetry(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0); // race condition으로 인하여 원하는 결과가 나오지 않음
    }

    @Test
    @DisplayName("synchronized 사용 (@Transactional 제거)")
    public void 동시요청_트랜잭션_제거_synchronized() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseWithoutTx(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0); // race condition으로 인하여 원하는 결과가 나오지 않읿
    }

    @Test
    @DisplayName("synchronized 사용 (@Transactional 외부에서)")
    public void 동시요청_트랜잭션_외부_synchronized() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    synchronizedStockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("pessimistic lock 사용")
    public void 동시요청_비관락() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pessimisticLockStockService.decrease(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0); // race condition으로 인하여 원하는 결과가 나오지 않읿
    }

    @Test
    @DisplayName("optimistic lock 사용")
    public void 동시요청_낙관락() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    optimisticLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("named lock 사용")
    public void 동시요청_네임드락() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    namedLockStockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("lettuce setnx로 lock 사용")
    public void 동시요청_lettuce_setnx() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    lettuceLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("redisson으로 lock 사용")
    public void 동시요청_redisson() throws InterruptedException {
        int threadCount = 100;
        // 쓰레드 32개를 관리하는 쓰레드 풀 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // 각 쓰레드의 작업 종료를 명시한다.
                }
            });
        }

        latch.await(); // 메인쓰레드는 latch의 count가 0이 되기를 기다린다.

        Stock stock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0);
    }
}