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

package org.dockbox.hartshorn.core.services.parameter;

import org.dockbox.hartshorn.core.annotations.inject.Context;
import org.dockbox.hartshorn.core.annotations.inject.Required;
import org.dockbox.hartshorn.core.boot.ExceptionHandler;
import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.context.ParameterLoaderContext;
import org.dockbox.hartshorn.core.context.element.ParameterContext;
import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.core.exceptions.ApplicationException;

public class ContextParameterLoaderRule implements ParameterLoaderRule<ParameterLoaderContext> {

    @Override
    public boolean accepts(final ParameterContext<?> parameter, final int index, final ParameterLoaderContext context, final Object... args) {
        return parameter.annotation(Context.class).present() && parameter.type().childOf(org.dockbox.hartshorn.core.context.Context.class);
    }

    @Override
    public <T> Exceptional<T> load(final ParameterContext<T> parameter, final int index, final ParameterLoaderContext context, final Object... args) {
        final TypeContext<org.dockbox.hartshorn.core.context.Context> type = (TypeContext<org.dockbox.hartshorn.core.context.Context>) parameter.type();
        final ApplicationContext applicationContext = context.applicationContext();
        final String name = parameter.annotation(Context.class).map(Context::value).orNull();

        final Exceptional<org.dockbox.hartshorn.core.context.Context> out = name == null
                ? applicationContext.first(type)
                : applicationContext.first(applicationContext, type.type(), name);

        final boolean required = parameter.annotation(Required.class).map(Required::value).or(false);
        if (required && out.absent()) return ExceptionHandler.unchecked(new ApplicationException("Parameter " + parameter.name() + " on " + parameter.declaredBy().qualifiedName() + " is required"));

        return out.map(c -> (T) c);
    }
}
