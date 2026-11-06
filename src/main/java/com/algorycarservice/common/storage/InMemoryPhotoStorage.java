package com.algorycarservice.common.storage;

import com.algorycarservice.common.exception.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test stand-in so the suite needs no SeaweedFS. */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "memory")
public class InMemoryPhotoStorage implements PhotoStorage {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        objects.put(key, bytes.clone());
    }

    @Override
    public byte[] get(String key) {
        byte[] bytes = objects.get(key);
        if (bytes == null) {
            throw new ResourceNotFoundException("photo");
        }
        return bytes.clone();
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }

    public int size() {
        return objects.size();
    }
}
