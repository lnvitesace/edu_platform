package com.edu.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@TestConfiguration
public class InMemoryRedisTestConfig {
    @Bean
    public StringRedisTemplate redisTemplate() {
        return new InMemoryStringRedisTemplate();
    }

    public static class InMemoryStringRedisTemplate extends StringRedisTemplate {
        private final Map<String, String> store = new ConcurrentHashMap<>();
        private final ValueOperations<String, String> ops = new InMemoryValueOperations(store);

        InMemoryStringRedisTemplate() {
            setConnectionFactory(new NoOpRedisConnectionFactory());
        }

        public void clear() {
            store.clear();
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return ops;
        }

        @Override
        public Set<String> keys(String pattern) {
            String prefix = pattern.replace("*", "");
            Set<String> keys = ConcurrentHashMap.newKeySet();
            for (String key : store.keySet()) {
                if (key.startsWith(prefix)) {
                    keys.add(key);
                }
            }
            return keys;
        }

        @Override
        public Boolean delete(String key) {
            return store.remove(key) != null;
        }

        @Override
        public Long delete(Collection<String> keys) {
            long count = 0;
            for (String key : keys) {
                if (store.remove(key) != null) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Boolean hasKey(String key) {
            return store.containsKey(key);
        }
    }

    static class InMemoryValueOperations implements ValueOperations<String, String> {
        private final Map<String, String> store;

        InMemoryValueOperations(Map<String, String> store) {
            this.store = store;
        }

        @Override
        public void set(String key, String value) {
            store.put(key, value);
        }

        @Override
        public String setGet(String key, String value, long timeout, TimeUnit unit) {
            store.put(key, value);
            return value;
        }

        @Override
        public String setGet(String key, String value, java.time.Duration timeout) {
            store.put(key, value);
            return value;
        }

        @Override
        public void set(String key, String value, long timeout, TimeUnit unit) {
            store.put(key, value);
        }

        @Override
        public Boolean setIfAbsent(String key, String value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
            return setIfAbsent(key, value);
        }

        @Override
        public Boolean setIfPresent(String key, String value) {
            if (!store.containsKey(key)) {
                return false;
            }
            store.put(key, value);
            return true;
        }

        @Override
        public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) {
            return setIfPresent(key, value);
        }

        @Override
        public void multiSet(Map<? extends String, ? extends String> map) {
            store.putAll(map);
        }

        @Override
        public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) {
            boolean allAbsent = map.keySet().stream().noneMatch(store::containsKey);
            if (allAbsent) {
                store.putAll(map);
            }
            return allAbsent;
        }

        @Override
        public String get(Object key) {
            return store.get(key);
        }

        @Override
        public String getAndDelete(String key) {
            String value = store.get(key);
            store.remove(key);
            return value;
        }

        @Override
        public String getAndExpire(String key, long timeout, TimeUnit unit) {
            return store.get(key);
        }

        @Override
        public String getAndExpire(String key, java.time.Duration timeout) {
            return store.get(key);
        }

        @Override
        public String getAndPersist(String key) {
            return store.get(key);
        }

        @Override
        public String getAndSet(String key, String value) {
            String old = store.get(key);
            store.put(key, value);
            return old;
        }

        @Override
        public java.util.List<String> multiGet(java.util.Collection<String> keys) {
            return keys.stream().map(store::get).toList();
        }

        @Override
        public Long increment(String key) {
            throw new UnsupportedOperationException("increment not supported");
        }

        @Override
        public Long increment(String key, long delta) {
            throw new UnsupportedOperationException("increment not supported");
        }

        @Override
        public Double increment(String key, double delta) {
            throw new UnsupportedOperationException("increment not supported");
        }

        @Override
        public Long decrement(String key) {
            throw new UnsupportedOperationException("decrement not supported");
        }

        @Override
        public Long decrement(String key, long delta) {
            throw new UnsupportedOperationException("decrement not supported");
        }

        @Override
        public Integer append(String key, String value) {
            throw new UnsupportedOperationException("append not supported");
        }

        @Override
        public String get(String key, long start, long end) {
            throw new UnsupportedOperationException("range get not supported");
        }

        @Override
        public void set(String key, String value, long offset) {
            throw new UnsupportedOperationException("set with offset not supported");
        }

        @Override
        public Long size(String key) {
            String value = store.get(key);
            return value == null ? 0L : (long) value.length();
        }

        @Override
        public Boolean setBit(String key, long offset, boolean value) {
            throw new UnsupportedOperationException("setBit not supported");
        }

        @Override
        public Boolean getBit(String key, long offset) {
            throw new UnsupportedOperationException("getBit not supported");
        }

        @Override
        public java.util.List<Long> bitField(String key, org.springframework.data.redis.connection.BitFieldSubCommands subCommands) {
            throw new UnsupportedOperationException("bitField not supported");
        }

        @Override
        public RedisOperations<String, String> getOperations() {
            return null;
        }
    }

    static class NoOpRedisConnectionFactory implements RedisConnectionFactory {
        @Override
        public boolean getConvertPipelineAndTxResults() {
            return false;
        }

        @Override
        public RedisConnection getConnection() {
            return null;
        }

        @Override
        public RedisClusterConnection getClusterConnection() {
            return null;
        }

        @Override
        public RedisSentinelConnection getSentinelConnection() {
            return null;
        }

        @Override
        public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
            return null;
        }
    }
}
