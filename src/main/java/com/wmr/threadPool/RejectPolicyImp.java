package com.wmr.threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j(topic = "RejectPolicyImp")
public class RejectPolicyImp {
    // 策略1：死等
    public final static RejectPolicy<Runnable> ALWAYS_WAIT = ((queue, task) -> {
        queue.put(task);
    });

    // 策略2：带超时等待
    public final static RejectPolicy<Runnable> TIMED_WAIT = ((queue, task) -> {
        queue.put(task, 100, TimeUnit.MILLISECONDS);
    });

    // 策略3：让调用者放弃任务执行
    public final static RejectPolicy<Runnable> GIVE_UP = ((queue, task) -> {
        log.debug("放弃{}", task);
    });

    // 策略4：让调用者抛出异常
    public final static RejectPolicy<Runnable> Throw_EXECPTION = ((queue, task) -> {
        throw new RuntimeException("任务执行失败 " + task);
    });

    // 策略5：让调用者自己执行任务
    public final static RejectPolicy<Runnable> RUN_BY_ITSELF = ((queue, task) -> {
        task.run();
    });
}
