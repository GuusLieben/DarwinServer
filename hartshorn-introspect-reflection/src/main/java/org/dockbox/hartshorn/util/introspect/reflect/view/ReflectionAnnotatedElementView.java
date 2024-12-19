/*
 * Copyright 2019-2024 the original author or authors.
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

package org.dockbox.hartshorn.util.introspect.reflect.view;

import org.dockbox.hartshorn.context.DefaultContext;
import org.dockbox.hartshorn.util.introspect.ElementAnnotationsIntrospector;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionElementAnnotationsIntrospector;
import org.dockbox.hartshorn.util.introspect.view.AnnotatedElementView;
import org.dockbox.hartshorn.util.introspect.view.IntrospectorAwareView;
import org.dockbox.hartshorn.util.introspect.view.wildcard.WildcardElementAnnotationsIntrospector;

import java.lang.reflect.AnnotatedElement;

/**
 * A view that provides access to the annotations of an {@link AnnotatedElement}. This view is backed by a
 * {@link ReflectionElementAnnotationsIntrospector} that is created based on the provided {@link Introspector}.
 *
 * @since 0.4.13
 *
 * @author Guus Lieben
 */
public abstract class ReflectionAnnotatedElementView extends DefaultContext implements AnnotatedElementView, IntrospectorAwareView {

    private final Introspector introspector;
    private ElementAnnotationsIntrospector annotationsIntrospector;

    protected ReflectionAnnotatedElementView(Introspector introspector) {
        this.introspector = introspector;
    }

    protected abstract AnnotatedElement annotatedElement();

    @Override
    public ElementAnnotationsIntrospector annotations() {
        if(this.annotationsIntrospector == null) {
            AnnotatedElement element = this.annotatedElement();
            this.annotationsIntrospector = element != null
                    ? new ReflectionElementAnnotationsIntrospector(this.introspector, element)
                    : new WildcardElementAnnotationsIntrospector();
        }
        return this.annotationsIntrospector;
    }

    @Override
    public Introspector introspector() {
        return this.introspector;
    }
}
