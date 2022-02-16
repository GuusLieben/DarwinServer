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

package org.dockbox.hartshorn.core.services;

import org.dockbox.hartshorn.core.Key;
import org.dockbox.hartshorn.core.annotations.stereotype.Service;
import org.dockbox.hartshorn.core.context.ApplicationContext;

import java.lang.annotation.Annotation;

public interface ServicePreProcessor<A extends Annotation> extends ComponentPreProcessor<A> {

    @Override
    default boolean modifies(final ApplicationContext context, final Key<?> key) {
        return key.type().annotation(Service.class).present() && this.preconditions(context, key);
    }

    boolean preconditions(ApplicationContext context, Key<?> key);
}
