package com.learnjava.java8;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class LambdaTest2 {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        //Predicate<Integer> predicate = n -> true
        //n是一个参数传递到Predicate接口的test方法
        //n如果存在则test方法返回true

        //传递参数n
        eval(list, n -> true);


    }

    public static void eval(List<Integer> list, Predicate<Integer> predicate) {
        for (Integer n : list) {
            if (predicate.test(n)) {
                System.out.println(n + " ");
            }
        }
    }
}
