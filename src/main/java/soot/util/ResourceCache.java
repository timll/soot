package soot.util;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2004 Ondrej Lhotak
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Caches auto-closable resources and evicts entries if no one holds the resource anymore.
 *
 * @param <K> key
 * @param <V> value
 *
 * @author Tim Lange
 */
public class ResourceCache<K, V extends AutoCloseable> {
    private static final Logger logger = LoggerFactory.getLogger(ResourceCache.class);

    private final class RemovalConsumer implements Consumer<AtomicLong> {
        private final K key;

        private RemovalConsumer(K key) {
            this.key = key;
        }

        @Override
        public void accept(AtomicLong refcount) {
            map.compute(key, (k, v) -> {
                // v can only be null if one provocated a resource leak via invalidateAll.
                if (v == null || refcount.decrementAndGet() > 0)
                    return v;

                try {
                    v.get().close();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException("Reference was already cleared!");
                }
            });
        }
    }

    private final ConcurrentHashMap<K, SharedResource<V>> map = new ConcurrentHashMap<>();

    private final CheckedFunction<K, V> resourceSupplier;

    /**
     * Create a resource cache
     *
     * @param resourceSupplier initializes the resource
     */
    public ResourceCache(CheckedFunction<K, V> resourceSupplier) {
        this.resourceSupplier = resourceSupplier;
    }

    public SharedResource<V> get(K key) throws ExecutionException {
        final Exception[] thrownException = new Exception[1];
        SharedResource<V> res = map.compute(key, (k, v) -> {
            if (v == null) {
                try {
                    v = new SharedResource<>(resourceSupplier.apply(k), new RemovalConsumer(k));
                } catch (Exception e) {
                    thrownException[0] = e;
                    return null;
                }
            }

            v.acquire();
            return v;
        });
        if (thrownException[0] != null)
            throw new ExecutionException(thrownException[0]);
        return res;
    }

    /**
     * Invalidate all entries. Make sure to not hold any open shared resources to prevent resource leaks.
     */
    public void invalidateAll() {
        map.replaceAll((k, v) -> {
            try {
                v.get().close();
            } catch (Exception e) {
                logger.error("Failed to close resource. Close all shared resources before calling invalidateAll to prevent resource leaks.", e);
            }
            return null;
        });
    }
}
