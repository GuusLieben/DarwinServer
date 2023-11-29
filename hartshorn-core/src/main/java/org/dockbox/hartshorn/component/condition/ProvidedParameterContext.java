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

package org.dockbox.hartshorn.component.condition;

import org.dockbox.hartshorn.context.DefaultProvisionContext;
import org.dockbox.hartshorn.util.introspect.view.ExecutableElementView;
import org.dockbox.hartshorn.util.introspect.view.ParameterView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class ProvidedParameterContext extends DefaultProvisionContext {

    private final Map<ParameterView<?>, Object> arguments = new HashMap<>();

    private ProvidedParameterContext(Map<ParameterView<?>, Object> arguments) {
        this.arguments.putAll(arguments);
    }

    public static ProvidedParameterContext of(Map<ParameterView<?>, Object> arguments) {
        return new ProvidedParameterContext(arguments);
    }

    public static ProvidedParameterContext of(List<ParameterView<?>> parameters, List<Object> arguments) {
        if (parameters.size() != arguments.size()) {
            throw new IllegalArgumentException("Parameters and arguments must be of the same size");
        }
        Map<ParameterView<?>, Object> argumentMap = IntStream.range(0, parameters.size()).boxed()
                .collect(
                        HashMap::new,
                        (parameterViews, index) -> parameterViews.put(parameters.get(index), arguments.get(index)),
                        Map::putAll
                );
        return new ProvidedParameterContext(argumentMap);
    }

    public static ProvidedParameterContext of(ExecutableElementView<?> executable, List<Object> arguments) {
        return of(executable.parameters().all(), arguments);
    }

    public Map<ParameterView<?>, Object> arguments() {
        return this.arguments;
    }
}
