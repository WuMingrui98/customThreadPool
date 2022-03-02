package com.wmr.threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j(topic = "TestPool")
public class TestPool {
    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(2, 2, 1000, TimeUnit.MICROSECONDS,
                // 1) 死等
//                RejectPolicyImp.ALWAYS_WAIT
                // 2) 带超时等待
                RejectPolicyImp.TIMED_WAIT
                // 3) 让调用者放弃任务执行
//                RejectPolicyImp.GIVE_UP
                // 4) 让调用者抛出异常
//                RejectPolicyImp.Throw_EXECPTION
                // 5) 让调用者自己执行任务
//                RejectPolicyImp.RUN_BY_ITSELF
        );
        for (int i = 0; i < 10; i++) {
            int j = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("{}", j);
            });
        }
    }
}
