package org.xiaoheshan.piggy.bank.basis.concurrent.exam;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author _Chf
 * @since 09-28-2019
 */
public class MultiBlockingQueue<E> {

    private BlockingQueue<E> mainQueue = new LinkedBlockingQueue<E>();

    private Semaphore gateMutex = new Semaphore(1);

    private Map<Condition<E>, BlockingQueue<E>> conditionBlockingQueueMap = new HashMap<>();

    private Thread dispatchThread = new Thread(() -> {
        while (true) {
            try {
                E element = mainQueue.take();
                for (Map.Entry<Condition<E>, BlockingQueue<E>> entry : conditionBlockingQueueMap.entrySet()) {
                    if (entry.getKey().match(element)) {
                        gateMutex.acquire();
                        entry.getValue().put(element);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    public MultiBlockingQueue(Set<Condition<E>> conditions) {
        for (Condition<E> condition : conditions) {
            conditionBlockingQueueMap.put(condition, new SynchronousQueue<>());
        }
        dispatchThread.start();
    }

    public void offer(E element) {
        mainQueue.offer(element);
    }

    public <V> void take(Condition<E> condition, Consumer<E> consumer) {
        BlockingQueue<E> queue = this.conditionBlockingQueueMap.get(condition);
        try {
            E element = queue.take();
            consumer.accept(element);
            gateMutex.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface Condition<E> {
        boolean match(E e);
    }

    public static void main(String[] args) {
        Set<Condition<Integer>> conditions = new HashSet<>();
        Condition<Integer> divideBy3 = (integer) -> integer % 3 == 0;
        Condition<Integer> divideBy5 = (integer) -> integer % 5 == 0;
        Condition<Integer> divideNotBy3And5 = (integer) -> integer % 3 != 0 && integer % 5 != 0;
        conditions.add(divideBy3);
        conditions.add(divideBy5);
        conditions.add(divideNotBy3And5);
        MultiBlockingQueue<Integer> blockingQueue = new MultiBlockingQueue<>(conditions);

        Thread A = new Thread(() -> {
            int counter = 1;
            while (true) {
                try {
                    blockingQueue.offer(counter++);
                    TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread B = new Thread(() -> {
            while (true) {
                blockingQueue.take(divideBy3, (integer) -> {
                    System.out.println(Thread.currentThread().getName() + ": " + integer);
                });
            }
        }, "B");
        Thread C = new Thread(() -> {
            while (true) {
                blockingQueue.take(divideBy5, (integer) -> {
                    System.out.println(Thread.currentThread().getName() + ": " + integer);
                });
            }
        }, "C");
        Thread D = new Thread(() -> {
            while (true) {
                blockingQueue.take(divideNotBy3And5, (integer) -> {
                    System.out.println(Thread.currentThread().getName() + ": " + integer);
                });
            }
        }, "D");

        A.start();
        B.start();
        C.start();
        D.start();
    }
}
