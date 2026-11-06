package com.algorycarservice.common.storage;

/** Object store for uploaded photos, addressed by key inside the configured bucket. */
public interface PhotoStorage {

    void put(String key, byte[] bytes, String contentType);

    byte[] get(String key);

    void delete(String key);

    /** For clean-up after the database row is gone: a leftover file is not worth failing the request for. */
    default void deleteQuietly(String key) {
        try {
            delete(key);
        } catch (RuntimeException ignored) {
            // Logged by the implementation.
        }
    }
}
