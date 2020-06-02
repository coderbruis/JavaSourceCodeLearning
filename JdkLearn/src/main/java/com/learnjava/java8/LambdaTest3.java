package com.learnjava.java8;

import java.util.Arrays;
import java.util.List;

public class LambdaTest3 {

    /**
     * Stream实例
     * @param args
     */

    public static void main(String[] args) {
        List<String> strings = Arrays.asList("abc", "", "bc", "efg", "abcd", "", "jkl");


        strings.stream() //创建流
                .filter(s -> s.startsWith("a"))
                .map(String::toUpperCase) //转换成大写 String::toUpperCase 等同于  string -> string.toUpperCase()
                .sorted() //排序
                .forEach(System.out::println); // for循环打印

        strings.stream()
                .filter(s -> s.startsWith("a"))
                .map(s -> s.toUpperCase())
                .map(String::toUpperCase)
                .sorted()
                .forEach(System.out::println);

        // C1
        // C2
    }
}
