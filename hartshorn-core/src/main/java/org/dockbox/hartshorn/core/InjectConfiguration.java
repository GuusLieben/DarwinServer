/*
 * Copyright 2019-2022 the original author or authors.
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

package org.dockbox.hartshorn.core;

import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.inject.Binder;
import org.dockbox.hartshorn.core.inject.DelegatedBinder;

import lombok.Setter;

public abstract class InjectConfiguration implements DelegatedBinder {

    @Setter private Binder binder;

    public abstract void collect(ApplicationContext context);

    public final Binder binder() {
        if (this.binder == null) throw new IllegalStateException("No binder provided!");
        return this.binder;
    }
}
