package com.wmr.threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "ThreadPool")
public class ThreadPool {
    // 1. 阻塞队列
    private final BlockingQueue<Runnable> taskQueue;

    // 2. 线程集合（线程不安全的，使用的时候要注意）
    private final HashSet<Worker> workers = new HashSet<>();

    // 3. 核心线程数
    private int coreSize;

    // 4. 获取任务时的超时时间
    private long timeout;

    // 5. 超时时间的单位
    private TimeUnit timeUnit;

    // 6. 拒接策略



    public ThreadPool(int coreSize, int capacity, long timeout, TimeUnit timeUnit) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.taskQueue = new BlockingQueue<>(capacity);
    }

    /**
     * 1. 如果工作线程数量小于核心线程数，则产生新的工作线程来执行任务
     * 2. 如果工作线程数量大于等于核心线程数，则将任务放入阻塞队列中
     * @param task 需要执行的任务
     */
    public void execute(Runnable task) {
        synchronized (workers) {
            if(workers.size() < coreSize) {
                Worker worker = new Worker(task);
                log.debug("新增 worker{}, {}", worker, task);
                workers.add(worker);
                worker.start();
            } else {
                taskQueue.put(task);
            }
        }
    }


    class Worker extends Thread {
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            /*
                工作线程执行任务
                1. 当task不为空时，执行task的任务
                2. 当task执行完毕，继续从任务队列中取新的任务
             */
            while(task != null || (task = taskQueue.take()) != null) {
                // 为什么这里需要用到try-catch是因为配合不同的拒接策略
                try {
                    log.debug("正在执行...{}", task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            synchronized (workers) {
                log.debug("worker 被移除{}", this);
                workers.remove(this);
            }
        }
    }
}
