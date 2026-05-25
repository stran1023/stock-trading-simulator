package com.tradingapp.market.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Component
public class PriceCache {

    private static final String KEY_PREFIX = "stock:price:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${market.price.cache-ttl-seconds}")
    private long cacheTtlSeconds;

    public PriceCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<BigDecimal> get(String symbol) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + symbol.toUpperCase());
        return value == null ? Optional.empty() : Optional.of(new BigDecimal(value));
    }

    public void set(String symbol, BigDecimal price) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + symbol.toUpperCase(),
                price.toPlainString(),
                Duration.ofSeconds(cacheTtlSeconds)
        );
    }
}
