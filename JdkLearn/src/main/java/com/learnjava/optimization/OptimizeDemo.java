package com.learnjava.optimization;

import java.util.HashMap;
import java.util.Map;

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
        }
        map.put(key, mapValue);
    }
}
