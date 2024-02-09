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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Reference-counting wrapper for resources. Used in {@link ResourceCache}.
 *
 * @param <R> resource
 *
 * @author Tim Lange
 */
public class SharedResource<R extends AutoCloseable> implements AutoCloseable {
    private final R resource;

    private final AtomicLong refcount = new AtomicLong();

    // Consumer that atomically decreases the refcount and evicts the unused resource.
    private final Consumer<AtomicLong> dec;

    /**
     * Create a shared resource
     *
     * @param resource resource
     * @param dec      Atomic consumer that decreases the reference count and may evict the shared resource
     */
    public SharedResource(R resource, Consumer<AtomicLong> dec) {
        this.resource = resource;
        this.dec = dec;
    }

    /**
     * Retrieve the resource reference
     *
     * @return resource
     */
    public R get() {
        return resource;
    }

    /**
     * Do not call manually! {@link ResourceCache} enforces that each cached resource is already acquired.
     */
    void acquire() {
        refcount.incrementAndGet();
    }

    @Override
    public void close() throws Exception {
        dec.accept(refcount);
    }
}
