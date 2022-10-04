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

package org.dockbox.hartshorn.web.mvc;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.component.processing.ComponentProcessingContext;
import org.dockbox.hartshorn.component.processing.ServicePreProcessor;
import org.dockbox.hartshorn.util.introspect.view.MethodView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.web.MvcControllerContext;
import org.dockbox.hartshorn.web.RequestHandlerContext;
import org.dockbox.hartshorn.web.annotations.MvcController;
import org.dockbox.hartshorn.web.annotations.http.HttpRequest;

public class MvcControllerPreProcessor implements ServicePreProcessor {

    @Override
    public <T> boolean preconditions(final ApplicationContext context, final ComponentProcessingContext<T> processingContext) {
        final TypeView<T> type = processingContext.type();
        return type.annotations().has(MvcController.class) && !type.methods().annotatedWith(HttpRequest.class).isEmpty();
    }

    @Override
    public <T> void process(final ApplicationContext context, final ComponentProcessingContext<T> processingContext) {
        final MvcControllerContext controllerContext = context.first(MvcControllerContext.class).get();
        for (final MethodView<T, ?> method : processingContext.type().methods().annotatedWith(HttpRequest.class)) {
            if (method.returnType().isChildOf(ViewTemplate.class)) {
                final RequestHandlerContext handlerContext = new RequestHandlerContext(context, method);
                controllerContext.add(handlerContext);
            }
            else {
                throw new IllegalArgumentException("Method " + method.name() + " must return a ViewTemplate");
            }
        }
    }
}
