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

package org.dockbox.hartshorn.web;

import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.context.DefaultCarrierContext;
import org.dockbox.hartshorn.core.context.element.MethodContext;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.web.annotations.PathSpec;
import org.dockbox.hartshorn.web.annotations.http.HttpRequest;

import lombok.Getter;

public class RequestHandlerContext extends DefaultCarrierContext {

    @Getter private final MethodContext<?, ?> methodContext;
    @Getter private final HttpRequest httpRequest;
    @Getter private final String pathSpec;

    public RequestHandlerContext(final ApplicationContext applicationContext, final MethodContext<?, ?> methodContext) {
        super(applicationContext);
        this.methodContext = methodContext;
        final Exceptional<HttpRequest> request = methodContext.annotation(HttpRequest.class);
        if (request.absent()) throw new IllegalArgumentException(methodContext.parent().name() + "#" + methodContext.name() + " is not annotated with @Request or an extension of it.");
        this.httpRequest = request.get();

        final Exceptional<PathSpec> annotation = methodContext.parent().annotation(PathSpec.class);
        String spec = this.httpRequest().value();
        spec = spec.startsWith("/") ? spec : '/' + spec;

        if (annotation.present()) {
            String root = annotation.get().pathSpec();
            if (root.endsWith("/")) root = root.substring(0, root.length()-1);
            spec = root + spec;
        }

        this.pathSpec = spec.startsWith("/") ? spec : '/' + spec;
    }
}
