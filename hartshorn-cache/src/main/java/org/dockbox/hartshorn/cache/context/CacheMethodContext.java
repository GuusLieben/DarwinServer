/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dockbox.hartshorn.cache.context;

import org.dockbox.hartshorn.cache.Expiration;
import org.dockbox.hartshorn.cache.annotations.Cached;
import org.dockbox.hartshorn.cache.annotations.EvictCache;
import org.dockbox.hartshorn.cache.annotations.UpdateCache;

/**
 * The method-specific cache context containing all required information to
 * manipulate cache methods. Values are typically derived from {@link Cached},
 * {@link EvictCache}, or {@link UpdateCache}.
 *
 * @author Guus Lieben
 * @since 21.2
 */
public interface CacheMethodContext {

    /**
     * The cache name to use, if any.
     */
    String name();

    /**
     * The cache expiration to use.
     */
    Expiration expiration();

}
