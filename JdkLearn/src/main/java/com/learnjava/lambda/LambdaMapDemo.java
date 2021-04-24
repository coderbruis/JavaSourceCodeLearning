package com.learnjava.lambda;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bruis
 * 操作Map
 */
public class LambdaMapDemo {

    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        test01();
    }

    /**
     * 需求，将employee集合中的元素根据date进行排序
     */
    public static void test01() {
        List<LambdaComparatorDemo.Employee> sortedByDate = LambdaComparatorDemo.employees
                .stream()
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

    public static Date getDate(String date) throws Exception {
        return format.parse(date);
    }
}
