package com.wmr.threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j(topic = "BlockingQueue")
public class BlockingQueue<T> {
    // 1. 任务队列
    private final Deque<T> queue = new ArrayDeque<>();

    // 2. 锁（用于保证并发时的线程安全）
    private final ReentrantLock lock = new ReentrantLock();

    // 3. 生产者条件变量（当任务队列满的时候，生产者线程阻塞在这里）
    private final Condition fullWaitSet = lock.newCondition();

    // 4. 消费者条件变量（当任务队列空的时候，消费者线程阻塞在这里）
    private final Condition emptyWaitSet = lock.newCondition();

    // 5. 阻塞队列的容量
    private final int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }


    /**
     * 从任务队列中取出任务，如果任务队列为空的时候，会一直阻塞
     *
     * @return 返回从任务队列中取出的任务
     */
    public T take() {
        lock.lock();
        try {
            // 当任务队列为空的时候，需要阻塞当前线程
            while (queue.isEmpty()) {
                try {
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T task = queue.removeFirst();
            log.debug("取出任务{}", task);
            // 唤醒阻塞的生产者线程
            fullWaitSet.signalAll();
            return task;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从任务队列中取出任务，如果任务队列为空的时候，会按照设定的超时时间阻塞，如果超时则返回空值
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 返回从任务队列中取出的任务，如果超时则返回null
     */
    public T take(long timeout, TimeUnit unit) {
        // 将时间统一转为纳秒
        long nanos = unit.toNanos(timeout);
        lock.lock();
        try {
            // 当任务队列为空的时候，需要阻塞当前线程
            while (queue.isEmpty()) {
                if (nanos <= 0) return null;
                try {
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T task = queue.removeFirst();
            log.debug("取出任务{}", task);
            // 唤醒阻塞的生产者线程
            fullWaitSet.signalAll();
            return task;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将任务放入任务队列中，如果任务队列满了，则会一直阻塞
     *
     * @param task 需要放入的任务
     */
    public void put(T task) {
        lock.lock();
        try {
            // 当任务队列为满的时候，需要阻塞当前线程
            while (queue.size() == capacity) {
                try {
                    log.debug("等待加入任务队列 {} ...", task);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列 {}", task);
            queue.offerLast(task);
            // 唤醒阻塞的消费者线程
            emptyWaitSet.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将任务放入任务队列中，如果任务队列满了，会按照会按照设定的超时时间阻塞，如果超时则直接返回false。
     *
     * @param task    需要放入的任务
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 如果成功放入队列，则返回true，否则返回false
     */
    public boolean put(T task, long timeout, TimeUnit unit) {
        long nanos = unit.toNanos(timeout);
        lock.lock();
        try {
            // 当任务队列为满的时候，需要阻塞当前线程
            while (queue.size() == capacity) {
                if (nanos <= 0) {
                    log.debug("加入任务队列失败{} ...", task);
                    return false;
                }
                try {
                    log.debug("等待加入任务队列 {} ...", task);
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列 {}", task);
            queue.offerLast(task);
            // 唤醒阻塞的消费者线程
            emptyWaitSet.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void putUseRejectPolicy(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        boolean equalCap = true;
        try {
            equalCap = queue.size() == capacity;
        } finally {
            lock.unlock();
        }
        // 判断队列是否满
        if (equalCap) {
            rejectPolicy.reject(this, task);
        } else { // 队列没有满
            lock.lock();
            try {
                log.debug("加入任务队列 {}", task);
                queue.addLast(task);
                emptyWaitSet.signalAll();
            } finally {
                lock.unlock();
            }

        }
    }



    /**
     * @return 返回任务队列的大小
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}
