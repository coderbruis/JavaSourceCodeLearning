package com.learnjava.concurent;

import java.util.concurrent.*;

public class ForkJoinDemo {

    /**
     *
     * Fork/join用法；
     * 1. 让任务类Task继承RecursiveTask<T>， 实现RecursiveTask的compute()方法
     * 2. 在compute()方法里，调用Task.fork()方法，开启子任务，然后再调用Task.join()合并结果
     * 3. 通过线程池调用Fork/Join框架：
     *   ForkJoinPool pool = new ForkJoinPool();
     *   Future<Integer> future = pool.submit(task);
     *   T t = future.get(1, TimeUnit.SECONDS);
     *
     */

    public static void main(String ... args) throws ExecutionException, InterruptedException, TimeoutException {

        ForkJoinPool pool = new ForkJoinPool();

        int[] array = {100,400,200,90,80,300,600,10,20,-10,30,2000,1000};

        MaxNumber task = new MaxNumber(array, 0, array.length - 1);

        Future<Integer> future = pool.submit(task);

        System.out.println("Result:" + future.get(1, TimeUnit.SECONDS));

    }

    /**
     * how to find the max number in array by Fork/Join
     */
    private static class MaxNumber extends RecursiveTask<Integer> {

        private int threshold = 2;

        private int[] array; // the data array

        private int index0 = 0;
        private int index1 = 0;

        public MaxNumber(int[] array, int index0, int index1) {
            this.array = array;
            this.index0 = index0;
            this.index1 = index1;
        }

        @Override
        protected Integer compute() {
            int max = Integer.MIN_VALUE;
            if ((index1 - index0) <= threshold) {

                for (int i = index0;i <= index1; i ++) {
                    max = Math.max(max, array[i]);
                }

            } else {
                //fork/join
                int mid = index0 + (index1 - index0) / 2;
                MaxNumber lMax = new MaxNumber(array, index0, mid);
                MaxNumber rMax = new MaxNumber(array, mid + 1, index1);

                lMax.fork();
                rMax.fork();

                int lm = lMax.join();
                int rm = rMax.join();

                max = Math.max(lm, rm);

            }

            return max;
        }
    }
}
