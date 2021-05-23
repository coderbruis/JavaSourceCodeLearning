package com.learnjava.lambda;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bruis
 * 操作Map
 */
public class LambdaMapDemo {

    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
//        test01();
//        test02();
        test03();
//        test04();
    }

    /**
     * 需求，将employee集合中的元素根据date进行排序
     */
    public static void test01() {
        List<LambdaComparatorDemo.Employee> sortedByDate = LambdaComparatorDemo.employees
                .stream()
                .sorted()
                .sorted((a, b) -> {
                    int result;
                    try {
                        result = getDate(b.getDate()).compareTo(getDate(a.getDate()));
                    } catch (Exception e) {
                        result = 0;
                    }
                    return result;
                })
                .collect(Collectors.toList());
        System.out.println(sortedByDate);
    }

    /**
     * HashMap的merge方法，如果key相同，则通过merge来对key相同的袁术进行处理
     */
    public static void test02() {
        String key = "money";
        Map<String, Integer> map = new HashMap<String, Integer>(){{put(key, 100);}};

        // 第三个参数时BiFunction，聚合函数（可以这么理解）
        map.merge(key,100,(oldValue, newValue) -> oldValue + newValue);
//        map.merge(key, 100,Integer::sum);
        System.out.println(map);
    }

    /**
     * 对map进行排序
     */
    public static void test03() {
        Map<String, Integer> codes = new HashMap<>();
        codes.put("2021-03", 1);
        codes.put("2021-02", 49);
        codes.put("2021-05", 33);
//        codes.put("2021-04-01", 1);
//        codes.put("2021-04-15", 49);
//        codes.put("2021-04-10", 33);
//        codes.put("2021-04-05", 86);
//        codes.put("2021-04-20", 92);

        // 先将Map转化为List，通过collect处理后再转为Map
        Map<String, Integer> sortedMap = codes.entrySet()
                .stream()
//                .sorted((c1, c2) -> c2.getValue().compareTo(c1.getValue()))
//                .sorted(Map.Entry.comparingByValue())
                .sorted((c1, c2) -> c2.getKey().compareTo(c1.getKey()))
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (oldVal, newVal) -> oldVal,
                                LinkedHashMap::new
                        )
                );

        sortedMap.entrySet().forEach(System.out::println);

    }

    /**
     * 将list转为map，并用其中一个value作为key
     */
    public static void test04() {
        LinkedHashMap<String, LambdaComparatorDemo.Employee> collect = LambdaComparatorDemo.employees
                .stream()
                .collect(
                        Collectors.toMap(
                                LambdaComparatorDemo.Employee::getDate,
                                // 这样是返回本身对象的一个表达式，还可以用Function.identity()
                                // Function.indentity() 就是 t -> t
//                                employee -> employee,
                                Function.identity(),
                                (oldVal, newVal) -> {
                                    // 重复的key就将年纪相加，然后FirstName通过--->加起来
                                    oldVal.setAge(oldVal.getAge() + newVal.getAge());
                                    oldVal.setFirstName(oldVal
                                            .getFirstName()
                                            .concat("--->")
                                            .concat(newVal.getFirstName()));
                                    return oldVal;
                                },
                                LinkedHashMap::new
                        )
                );
        // 这样打印出的map元素不好观察
//        System.out.println(collect);
//        collect.entrySet().forEach(System.out::println);

        LinkedHashMap<String, LambdaComparatorDemo.Employee> sortedCollect = collect
                .entrySet()
                .stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                // 上面已经对重复key做处理了，这里就直接范围oldVal就行
                                (oldVal, newVal) -> oldVal,
                                LinkedHashMap::new
                        )
                );

        // 根据日期排序
        sortedCollect.entrySet()
                .forEach(System.out::println);
    }

    public static Date getDate(String date) throws Exception {
        return format.parse(date);
    }
}
