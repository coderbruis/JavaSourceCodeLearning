package com.learnjava.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bruis
 * Lambda的List demo
 */
public class LambdaDemoForList {

    public static String[] arrays = {"Monkey", "Lion", "Giraffe", "Lemur"};

    public static String[] arrays2 = {"Monkey", "Lion", "Giraffe", "Lemur", "Lion"};

    public static void main(String[] args) {
//        test01();
//        test02();
        test03();
    }

    /**
     * 去重Distinct + 排序Sort
     */
    public static void test03() {
        // 去重
        List<String> uniqueAnimals = Stream.of(arrays2)
                .distinct()
                .collect(Collectors.toList());
        System.out.println(uniqueAnimals);
        // 排序
        List<String> sortedAnimals = Stream.of(arrays)
                // 对字母是按照自然顺序进行排序的
                .sorted()
                .collect(Collectors.toList());
        System.out.println(sortedAnimals);
    }

    /**
     * Limit + Skip 数据截取
     */
    public static void test02() {

        /**
         * 截取前两位字符串
         */
        List<String> limitN = Stream.of(arrays)
                .limit(2)
                .collect(Collectors.toList());

        /**
         * 过滤掉前两位元素
         */
        List<String> skipN = Stream.of(arrays)
                .skip(2)
                .collect(Collectors.toList());

        System.out.println(limitN);
        System.out.println(skipN);
    }

    /**
     * 过滤 + map处理
     */
    public static void test01() {
        List<String> nameStrList = Arrays.asList("abc", "efg", "hig", "hii", "klm");

        List<String> result = nameStrList
                .stream()
                .filter(s -> s.startsWith("h"))
                .map(String::toUpperCase)
                // 调用自定义的方法
                .map(MyStringUtils::myToUpperCase)
                .collect(Collectors.toList());

        for (String name : result) {
            System.out.println(name);
        }
    }

    private static class MyStringUtils {
        public static String myToUpperCase(String str) {
            return str.toUpperCase();
        }
    }
}
