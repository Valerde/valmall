package com.sovava.search.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 继承Thread
 * 实现Runnable接口
 * 实现Callable接口 ， + futureTask , (可以拿到返回结果 ， 也可以处理异常）
 * 线程池
 */
@Slf4j
public class ThreadTest {
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        log.debug("main...start...");
//        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
//            log.debug("Thread0当前线程：{}", Thread.currentThread().getId());
//            int i = 10 / 5;
//            log.debug("Thread0运行结果：{}", i);
//        }, executorService);

        /**
         * 方法完成后的感知
         */
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            log.debug("Thread0当前线程：{}", Thread.currentThread().getId());
//            int i = 10 / 0;
//            log.debug("Thread0运行结果：{}", i);
//            return i;
//        }, executorService).whenCompleteAsync((res, exception) -> {//虽然监听到异常了 ， 但是没法修改数据
//            log.debug("结果是{},异常是{}", res, exception);
//        }).exceptionally((ex) -> {//可以感知异常，同时返回默认值
//            ex.printStackTrace();
//            return 3;
//        });

        /**
         * 方法执行完成后的处理
         */
        /*CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            log.debug("Thread0当前线程：{}", Thread.currentThread().getId());
            int i = 10 / 4;
            log.debug("Thread0运行结果：{}", i);
            return i;
        }, executorService).handle((res, exe) -> {
            if (res != null) {
                return res * 2;
            }
            if (exe != null) {
                return 0;
            }
            return 0;
        });*/


        /**
         * 线程串行化
         * thenRunAsync 不能感知到上一步的执行结果 , 无返回值
         * thenApplyAsync可以感知到上一部的返回结果 ， 但是不能改变返回值
         */
//        CompletableFuture.supplyAsync(() -> {
//            log.debug("当前线程：{}", Thread.currentThread().getId());
//            int i = 10 / 2;
//            log.debug("运行结果：{}", i);
//            return i;
//        }, executorService).thenApplyAsync((res) -> {
//            log.debug("任务1.25启动了， 上一部的执行结果为{}", res);
//            return "hello" + res;
//        }, executorService).thenAcceptAsync((res) -> {
//            log.debug("任务二1.5启动了,上一部的返回值为{}", res);
//        }, executorService).thenRunAsync(() -> {
//            log.debug("任务二启动了");
//        }, executorService);

        /**
         * 两个线程组合一起运行
         */
        CompletableFuture<Integer> future01 = CompletableFuture.supplyAsync(() -> {
            log.debug("当前线程任务一：{}", Thread.currentThread().getId());
            int i = 10 / 2;
            log.debug("任务一运行结果：{}", i);
            return i;
        }, executorService);
        CompletableFuture<Integer> future02 = CompletableFuture.supplyAsync(() -> {
            log.debug("当前线程任务二：{}", Thread.currentThread().getId());
            int i = 10 / 5;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("任务二运行结果：{}", i);
            return i;
        }, executorService);
//        future01.runAfterBothAsync(future02, () -> {
//            log.debug("任务一和任务二都执行完了");
//        }, executorService);
//
//        future01.thenAcceptBothAsync(future02, (res1, res2) -> {
//            log.debug("任务一执行结果是{},任务二执行结果是{}", res1, res2);
//        }, executorService);
//        CompletableFuture<Integer> future3 = future01.thenCombineAsync(future02, (res1, res2) -> {
//            log.debug("任务一执行结果是{},任务二执行结果是{},二者相乘为{}", res1, res2, res1 * res2);
//            return res1 * res2;
//        }, executorService);
//        Integer integer = future3.get();

        int 傻逼 = 7;

        /**
         * 两个任务只要有一个执行完成 ， 就执行任务三
         */
        future01.runAfterEitherAsync(future02, () -> {
            log.debug("任务一和任务二有一个执行完了");
        }, executorService);

        future01.acceptEitherAsync(future02, (res) -> {
            log.debug("结果是{}", res);
        }, executorService);

        CompletableFuture<Integer> future03 = future01.applyToEitherAsync(future02, (res) -> {
            log.debug("其中一个返回结果是{},把它改成了{}", res, res * res);
            return res * res;
        }, executorService);
        Integer integer = future03.get();
        log.debug("二者中的任何一个执行成功的返回结果是{}", integer);
//        log.debug("二者相乘结果为{}", integer);


        /**
         * 多任务组合
         */
        CompletableFuture<String> future001 = CompletableFuture.supplyAsync(() -> {
            log.debug("查询图片地址");
            return "hello.jpg";
        }, executorService);
        CompletableFuture<Integer> future002 = CompletableFuture.supplyAsync(() -> {
            log.debug("查询数据库");
            return 5;
        }, executorService);
        CompletableFuture<Integer> future003 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("查询缓存");
            return 5;
        }, executorService);

//        CompletableFuture<Void> all = CompletableFuture.allOf(future001, future002, future003);
        CompletableFuture<Object> any = CompletableFuture.anyOf(future001, future002, future003);
        any.handle((res, ex) -> {
            log.debug("进来了");
            String s = "";
            Integer integer1 = 0;
            Integer integer2 = 0;
            try {
                s = future001.get();
                integer1 = future002.get();
                integer2 = future003.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.debug("所有结果为{},{},{}", s, integer1, integer2);
            return "ok";
        });

//        Integer integer = future1.get();
//        log.debug("执行结果{}", integer);

//        voidCompletableFuture.
        log.debug("main...end...");
    }

    /**
     * 测试线程的
     *
     * @param args
     * @throws Exception
     */
    public static void thread(String[] args) throws Exception {
        log.debug("main...start...");
//        一/继承Thread
//        Thread0 thread0 = new Thread0();
//        thread0.start();


//        二/实现Runnable接口
//        Runnable1 runnable1 = new Runnable1();
//        new Thread(runnable1).start();

//        三/实现Callable接口 ， + futureTask , (可以拿到返回结果 ， 也可以处理异常）
//        Callable01 callable01 = new Callable01();
//        Integer call = callable01.call();
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//        //阻塞等待线程执行完成 ， 获取返回结果
//        Integer integer = futureTask.get();

//        四/线程池
        //  以后的业务代码里面 ， 都不采用以上的三种方法 ， 我们将所有的多线程异步任务都交给线程池来执行
        //当前系统中池只能有一两个 ， 每一个异步任务直接提交给线程池

        //五/ 区别
        //1.2不能获得返回值  3可以获得返回值
        //1.2.3 都不能控制资源
        // 4 可以控制资源，性能稳定
//        executorService.execute(new Runnable1());

        /**
         *
         * 七大参数
         * corePoolSize【5】核心线程数量 （一直存在除非allowCoreThreadTimeOut),线程池 ， 创建好之后就准备接受任务
         *              5个 Thread thread = new Thread(); thread.start();
         * maximumPoolSize [200] 最大线程数量，控制资源
         * keepAliveTime 存活时间，具体说，当线程数大于核心线程数时，空闲线程在等待新任务到达的最大时间，如果超过这个时间还没有任务请求，该空闲线程就会被销毁。
         * unit 时间单位
         * BlockingQueue<Runnable> workQueue 阻塞队列 ， 如果任务有很多 ， 就会将目前比较多的任务放到队列里面 ，只要有线程空闲，就会去队列里面取出新的人物继续执行
         * ThreadFactory 线程的创建工厂
         * RejectedExecutionHandler 并发超出线程数和工作队列时候的任务请求处理策略，使用了策略设计模式
         *
         *
         * ** 工作顺序：
         *      1. 线程池创建 ， 准备好core数量的核心线程准备接受任务
         *          1.1 核心线程满了就将再进来的任务放到阻塞队列里面空闲的core就会自己去阻塞队列获取任务执行
         *          1.2 阻塞队列满了，就直接开启新线程执行，但不能超过maximumPoolSize 的大小
         *          1.3 如果max也满了 ， 还有新任务进来 ， 那么就采用采用指定的reject拒绝策略
         *          1.4 max有空闲 ， 在指定的存活时间以后 ， 就会释放到max-core的线程数量
         *
         *          new LinkedBlockingDeque<>()默认是Integer的最大值 ， 可能造成内存不够
         *
         *  一个线程池7，max20，队列50，100个并法进来是如何分配的
         *  7个会被立即执行 ， 50个进入队列 ， 再开13个进行执行 ， 剩下的30个使用拒绝策略.
         *  如果剩下的线程不想被抛弃 ， 采用CallerRunsPolicy ， 会执行run方法
         */
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(10 * 10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        Executors.newCachedThreadPool(); // 缓存线程池，核心线程数为0
        Executors.newFixedThreadPool(10);//固定大小线程池 ， 核心线程数等于最大线程数
        Executors.newScheduledThreadPool(10); //定时任务的线程池
        Executors.newSingleThreadScheduledExecutor();// 但线程的线程池 ， 保证任务按顺序执行

//        log.debug("返回结果{}", integer);
        log.debug("main...end...");
    }

    public static class Thread0 extends Thread {
        @Override
        public void run() {
            log.debug("Thread0当前线程：{}", Thread.currentThread().getId());
            int i = 10 / 5;
            log.debug("Thread0运行结果：{}", i);
        }
    }

    public static class Runnable1 implements Runnable {

        @Override
        public void run() {
            log.debug("Runnable1当前线程：{}", Thread.currentThread().getId());
            int i = 10 + 5;
            log.debug("Runnable1运行结果：{}", i);
        }
    }

    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            log.debug("Callable01当前线程：{}", Thread.currentThread().getId());
            int i = 10 - 5;
            log.debug("Callable01运行结果：{}", i);
            return i;
        }
    }
}
