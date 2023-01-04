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

package org.dockbox.hartshorn.util.introspect.view.wildcard;

import org.dockbox.hartshorn.util.GenericType;
import org.dockbox.hartshorn.util.introspect.TypeFieldsIntrospector;
import org.dockbox.hartshorn.util.introspect.view.FieldView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class WildcardTypeFieldsIntrospector implements TypeFieldsIntrospector<Object> {

    @Override
    public Option<FieldView<Object, ?>> named(final String name) {
        return Option.empty();
    }

    @Override
    public List<FieldView<Object, ?>> all() {
        return Collections.emptyList();
    }

    @Override
    public List<FieldView<Object, ?>> annotatedWith(final Class<? extends Annotation> annotation) {
        return Collections.emptyList();
    }

    @Override
    public <F> List<FieldView<Object, ? extends F>> typed(final Class<F> type) {
        return Collections.emptyList();
    }

    @Override
    public <F> List<FieldView<Object, ? extends F>> typed(final GenericType<F> type) {
        return Collections.emptyList();
    }
}
