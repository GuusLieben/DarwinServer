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

package org.dockbox.hartshorn.proxy;

import org.dockbox.hartshorn.util.TypeUtils;
import org.dockbox.hartshorn.util.introspect.ProxyLookup;

/**
 * A {@link ProxyLookup} implementation for Hartshorn's own proxy implementation. This implementation
 * supports proxies that are created through a {@link ProxyFactory}, or otherwise implement {@link Proxy}
 * directly.
 */
public class HartshornProxyLookup implements ProxyLookup {

    @Override
    public <T> Class<T> unproxy(final T instance) {
        if (instance instanceof Proxy<?> proxy) {
            return TypeUtils.adjustWildcards(proxy.manager().targetClass(), Class.class);
        }
        return TypeUtils.adjustWildcards(instance.getClass(), Class.class);
    }

    @Override
    public boolean isProxy(final Object instance) {
        return instance != null && (instance instanceof Proxy || this.isProxy(instance.getClass()));
    }

    @Override
    public boolean isProxy(final Class<?> candidate) {
        return Proxy.class.isAssignableFrom(candidate) && !Proxy.class.equals(candidate);
    }
}
