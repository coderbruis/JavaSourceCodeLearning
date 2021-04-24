package com.learnjava.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author bruis
 * 累加器
 */
public class LambdaReduceDemo {

    public static void main(String[] args) {
//        test01();
//        test02();
        test03();
    }

    /**
     * 对整形进行操作
     */
    public static void test01() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        Integer reduce = numbers
                .stream()
                .reduce(0, LambdaReduceDemo::mySum);
//                .reduce(0, Integer::sum);
//                .reduce(0, (total, element) -> total + element);
        System.out.println(reduce);
    }

    /**
     * 对字符串进行操作
     */
    public static void test02() {
        List<String> letters = Arrays.asList("a", "b", "c", "d", "e");
        String reduce = letters
                .stream()
                .reduce("", String::concat);
//                .reduce("", (totol, element) -> totol.concat(element));
        System.out.println(reduce);
    }

    /**
     * 操作Employee集合元素，将所有员工的年龄通过reduce累加起来
     *
     * U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner
     * reduce的三个参数：
     * 1）初始值；
     * 2）累加器（可自己实现逻辑）
     * 3) 合并器（parallelStream模式时的合并）
     *
     */
    public static void test03() {
        // 将Employee集合元素转化为Integer集合元素（流）
        // map的操作就是将a类型元素转化为b类型元素
        Stream<Integer> integerStream = LambdaComparatorDemo
                .employees
                .stream()
                .map(LambdaComparatorDemo.Employee::getAge);

        // 求所有员工的年龄
        Integer totalAge = integerStream.reduce(0, Integer::sum);
        System.out.println(totalAge);

        // 数据量大的话，可以用并行计算
        // 先讲员工集合转化为“并行流”
        Stream<Integer> parallelStream = LambdaComparatorDemo
                .employees
                .parallelStream()
                .map(LambdaComparatorDemo.Employee::getAge);

        // 相比于普通的单个流，parallelStream多了个合并器，将多个CPU计算的结果再合并到同一个流
        Integer reduce = parallelStream.reduce(0, Integer::sum, Integer::sum);
        System.out.println(reduce);

        // 可以不用map将employee转化为Integer对象，可以直接reduce操作，最后通过Integer::sum这个合并器来将结果合并为Integer类型
        Integer total = LambdaComparatorDemo
                .employees
                .stream()
                .reduce(0, (subTotal, emp) -> subTotal + emp.getAge(), Integer::sum);
        System.out.println(total);

        Integer total2 = LambdaComparatorDemo
                .employees
                .stream()
                .reduce(0, LambdaReduceDemo::mySum2, Integer::sum);
        System.out.println(total2);
    }

    /**
     * 可作为BiFunction，传入到reduce作为入参
     * @param a
     * @param b
     * @return
     */
    public static Integer mySum(int a, int b) {
        return a + b;
    }

    public static Integer mySum2(int a, LambdaComparatorDemo.Employee employee) {
        return a + employee.getAge();
    }
}
