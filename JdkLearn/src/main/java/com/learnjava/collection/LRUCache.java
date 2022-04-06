package com.learnjava.collection;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/4/6
 */
public class LRUCache<K, V> implements Iterable<K> {

    private int MAX = 3;
    private LinkedHashMap<K, V> cache = new LinkedHashMap<>();

    public void cache(K key, V value) {
        if (cache.containsKey(key)) {
            cache.remove(key);
        } else if (cache.size() >= MAX) {
            Iterator<K> iterator = cache.keySet().iterator();
            K first = iterator.next();
            cache.remove(first);
        }
        cache.put(key, value);
    }

    public V getValue(K k) {
        return cache.get(k);
    }

    @Override
    public void forEach(Consumer<? super K> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<K> spliterator() {
        return Iterable.super.spliterator();
    }

    @Override
    public Iterator<K> iterator() {
        Iterator<K> iterator = cache.keySet().iterator();
        return new Iterator<K>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public K next() {
                return iterator.next();
            }
        };
    }

    public static void main(String[] args) {
        LRUCache<String, String> cache = new LRUCache<>();
        cache.cache("1", "1A");
        cache.cache("2", "2A");
        cache.cache("3", "3A");
        cache.cache("1", "1A");

        for (String next : cache) {
            System.out.println(cache.getValue(next));
        }
    }
}
