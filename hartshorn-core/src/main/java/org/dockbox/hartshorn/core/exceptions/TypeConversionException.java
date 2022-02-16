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

package org.dockbox.hartshorn.core.exceptions;

import org.dockbox.hartshorn.core.context.element.TypeContext;

public class TypeConversionException extends ApplicationException {

    public TypeConversionException(final Class<?> type, final String value) {
        this(TypeContext.of(type), value);
    }

    public TypeConversionException(final TypeContext<?> type, final String value) {
        super("Could not convert '" + value + "' to type " + type.name());
    }

    public TypeConversionException(final TypeContext<?> type, final String value, final Throwable cause) {
        super("Could not convert '" + value + "' to type " + type.name(), cause);
    }
}
