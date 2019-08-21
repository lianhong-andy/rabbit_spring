package com.andy.queue;

import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lianhong
 * @description 队列
 * @date 2019/8/15 0015上午 11:48
 */
public class QueueTest {

    BlockingQueue<String> queue = new ArrayBlockingQueue<String>(3);

    public void produce() throws InterruptedException {
        queue.put("an order message");
        System.out.println("queue = " + queue.toString());
    }

    public String consume() throws InterruptedException {
        String orderMessage = queue.take();
        return orderMessage;
    }

    public int getMessageNumber(){
        return queue.size();
    }

    public static void testQueue(){
        final QueueTest queueTest = new QueueTest();

        class Producer implements Runnable{

            @Override
            public void run() {
                while (true) {
                    try {
                        queueTest.produce();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        class Consumer implements Runnable{

            @Override
            public void run() {
                while (true) {
                    try {
                        queueTest.consume();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        Producer producer = new Producer();
        Consumer consumer = new Consumer();
        executorService.submit(producer);
//        executorService.submit(consumer);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdownNow();

    }

    public static void main(String[] args) {
        QueueTest.testQueue();
    }

}
