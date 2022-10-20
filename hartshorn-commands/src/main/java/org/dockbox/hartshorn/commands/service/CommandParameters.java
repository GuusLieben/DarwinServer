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

package org.dockbox.hartshorn.commands.service;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.commands.annotations.Parameter;
import org.dockbox.hartshorn.commands.arguments.CustomParameterPattern;
import org.dockbox.hartshorn.commands.arguments.DynamicPatternConverter;
import org.dockbox.hartshorn.commands.context.ArgumentConverterContext;
import org.dockbox.hartshorn.commands.definition.ArgumentConverter;
import org.dockbox.hartshorn.component.processing.ComponentPreProcessor;
import org.dockbox.hartshorn.component.processing.ComponentProcessingContext;
import org.dockbox.hartshorn.component.processing.ProcessingOrder;

/**
 * Scans for any type annotated with {@link Parameter} and registers a {@link DynamicPatternConverter}
 * for each type found.
 */
public class CommandParameters implements ComponentPreProcessor {

    @Override
    public <T> void process(final ApplicationContext context, final ComponentProcessingContext<T> processingContext) {
        if (processingContext.type().annotations().has(Parameter.class)) {
            final Parameter meta = processingContext.type().annotations().get(Parameter.class).get();
            final CustomParameterPattern pattern = context.get(meta.pattern());
            final String parameterKey = meta.value();
            final ArgumentConverter<?> converter = new DynamicPatternConverter<>(processingContext.type().type(), pattern, parameterKey);
            context.first(ArgumentConverterContext.class).present(converterContext -> converterContext.register(converter));
        }
    }

    @Override
    public Integer order() {
        return ProcessingOrder.EARLY;
    }
}
