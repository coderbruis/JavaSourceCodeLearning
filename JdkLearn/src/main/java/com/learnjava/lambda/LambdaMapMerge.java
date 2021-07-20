package com.learnjava.lambda;

import java.util.HashMap;
import java.util.Map;

/**
 * 关于Map的合并操作
 *
 * @author lhy
 * @date 2021/7/20
 */
public class LambdaMapMerge {

    public static void main(String[] args) {
//        mapMerge();
//        mapMerge2();
    }

    /**
     * value为int类型的map merge操作，将两个map，相同key merge在一起
     *
     * key：string
     * value：int
     */
    public static void mapMerge() {
        Map<String, Integer> map1= new HashMap<>();
        map1.put("one",1);
        map1.put("two",2);
        map1.put("three",3);
        Map<String,Integer> map2= new HashMap<>();
        map2.put("one",1);
        map2.put("two",2);

        map1.forEach((key, value) -> map2.merge(key, value, Integer::sum));
        System.out.println(map2);
    }

    /**
     * value为int类型的map merge操作，将两个map，相同key merge在一起
     *
     * key：string
     * value：String
     */
    public static void mapMerge2() {
        Map<String,String> map1= new HashMap<>();
        map1.put("one","1");
        map1.put("two","2");
        map1.put("three","3");
        Map<String,String> map2= new HashMap<>();
        map2.put("one","1");
        map2.put("two","2");

        map1.forEach((key, value) -> map2.merge(key, value,(total, num) -> String.valueOf(Integer.parseInt(total) + Integer.parseInt(num))));

        System.out.println(map2);

    }
}
