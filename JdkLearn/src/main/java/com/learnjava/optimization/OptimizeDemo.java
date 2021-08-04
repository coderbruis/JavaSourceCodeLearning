package com.learnjava.optimization;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * 代码优化技巧总结
 *
 * @author lhy
 * @date 2021/7/19
 */
public class OptimizeDemo {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        mergeData(map);

        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        concurrentMergeData(concurrentMap);
    }

    /**
     * 对于通过map来聚合数据（非Lambda方式）
     * @param map
     */
    public static void mergeData(Map<String, Integer> map) {
        String key = "mapKey";
        int value = 1;
        // 普通方式
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + value);
        } else {
            map.put(key,value);
        }

        // 简洁方式
        Integer mapValue = map.get(key);
        if (null != mapValue) {
            mapValue += value;
        } else {
            mapValue = value;
        }
        map.put(key, mapValue);
    }

    /**
     * 针对mergeData里map的put操作，在并发情况下会存在put的时候，以及有其他线程已经put成功了，导致线程不安全，
     * 所以需要使用并发集合列的putIfAbsent方法
     * @param map
     */
    public static void concurrentMergeData(Map<String, Integer> map) {
        String key = "mapKey";
        int value = 1;
        Integer mapValue = map.get(key);
        if (null != mapValue) {
            mapValue += value;
        } else {
            mapValue = value;
        }
        map.putIfAbsent(key, mapValue);

        // computeIfAbsent方法对map中的key只进行重新计算，如果不存在这个key，则添加到map中
        map.computeIfAbsent(key, (k) -> {
            // 其他计算
            int a = 1, b = 2;
            return a + b;
        });
    }
}
