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

package org.dockbox.hartshorn.config;

import org.dockbox.hartshorn.context.ContextKey;
import org.dockbox.hartshorn.context.DefaultContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationURIContextList extends DefaultContext {

    public static final ContextKey<ConfigurationURIContextList> CONTEXT_KEY = ContextKey.builder(ConfigurationURIContextList.class)
            .fallback(ConfigurationURIContextList::new)
            .build();

    private final Set<ConfigurationURIContext> contexts = ConcurrentHashMap.newKeySet();

    public Set<ConfigurationURIContext> uris() {
        return this.contexts;
    }

    public void add(final Set<ConfigurationURIContext> contexts) {
        this.contexts.addAll(contexts);
    }

    public void add(final ConfigurationURIContext context) {
        this.contexts.add(context);
    }
}
