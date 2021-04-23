package com.learnjava.lambda;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class LambdaComparatorDemo {

    public static String[] arrays = {"Milan", "london", "San Francisco", "Tokyo", "New Delhi"};

    public static void main(String[] args) {
        test01();
    }

    /**
     * List字符串排序
     */
    public static void test01() {
        List<String> cities = Arrays.asList(arrays);

        System.out.println(cities);

        // CASE_INSENSITIVE_ORDER 是一个排序器Comparator接口，意思是不区分大小写进行排序
        cities.sort(String.CASE_INSENSITIVE_ORDER);
        System.out.println(cities);

        // 自然排序
        cities.sort(Comparator.naturalOrder());
        System.out.println(cities);

        // 可以将排序器放在Stream管道流中
        Stream.of(arrays)
                .sorted(Comparator.naturalOrder())
                .forEach(System.out::println);
    }























    @Data
    @AllArgsConstructor
    public class Employee {

        private Integer id;
        // 年龄
        private Integer age;
        // 性别
        private String gender;
        private String firstName;
        private String lastName;
    }
}
