package com.learnjava.lambda;

import jdk.nashorn.internal.objects.annotations.Constructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bruis
 * Lambda的List demo
 */
public class LambdaDemoForList {

    public static void main(String[] args) {
        test01();
    }

    public static void test01() {
        List<String> nameStrList = Arrays.asList("abc", "efg", "hig", "hii", "klm");

        List<String> result = nameStrList
                .stream()
                .filter(s -> s.startsWith("h"))
                .map(String::toUpperCase)
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
