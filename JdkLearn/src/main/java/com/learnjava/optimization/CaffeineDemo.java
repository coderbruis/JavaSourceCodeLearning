package com.learnjava.optimization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 代码Demo（SpringBoot自带的缓存类）
 *
 *
 * @author lhy
 * @date 2021/7/22
 */
public class CaffeineDemo {

    private static Cache<String, ProductVo> productVoCache;

    public static RemovalListener<String, ProductVo> listener = (k, v, cause) -> {
        // 业务逻辑


        // 触发异常
        switch (cause) {
            // 过期
            case EXPIRED:
                break;
            // 手动删除
            case EXPLICIT:
                break;
            //  被替换
            case REPLACED:
                break;
            // 垃圾回收
            case COLLECTED:
                break;
            // 超过数量限制
            case SIZE:
                break;
            default:
                break;
        }
    };

    public static void main(String[] args) {
        // 初始化
        // afterPropertiesSet();
    }

    /**
     * 模拟Spring的类初始化的时候对缓存进行初始化
     */
    public static void afterPropertiesSet() {
        productVoCache = Caffeine.newBuilder()
                .softValues()
                .refreshAfterWrite(7200, TimeUnit.SECONDS)
                .removalListener(listener)
                // .build(k -> loadSync(k))
                // 非static类中，可以使用这种方式.build(this::loadSync);
                .build(CaffeineDemo::loadSync);
    }

    /**
     * 获取对应缓存内容
     * @param key
     * @return
     */
    public static ProductVo getProductVo(String key) {
        return productVoCache.get(key, CaffeineDemo::loadSync);
    }

    /**
     * 对对应商品进行缓存
     * @param key
     */
    public static void putProductVo(String key) {
        productVoCache.put(key, loadSync(key));
    }

    private static ProductVo loadSync(String key) {
        // 业务逻辑
        return new ProductVo();
    }





















    public static class ProductVo {

        private String productName;

        private BigDecimal price;

        public ProductVo() {}

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
