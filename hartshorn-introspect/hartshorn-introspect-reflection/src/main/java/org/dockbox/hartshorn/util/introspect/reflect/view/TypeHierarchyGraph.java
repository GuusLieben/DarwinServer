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

package org.dockbox.hartshorn.util.introspect.reflect.view;

import org.dockbox.hartshorn.util.graph.ContainableGraphNode;
import org.dockbox.hartshorn.util.graph.MutableContainableGraphNode;
import org.dockbox.hartshorn.util.graph.SimpleGraph;
import org.dockbox.hartshorn.util.graph.SimpleGraphNode;
import org.dockbox.hartshorn.util.introspect.view.TypeView;

import java.util.List;

public class TypeHierarchyGraph extends SimpleGraph<TypeView<?>> {

    public static TypeHierarchyGraph of(final TypeView<?> type) {
        final TypeHierarchyGraph graph = new TypeHierarchyGraph();
        graph.addRoot(createNode(type));
        return graph;
    }

    private static ContainableGraphNode<TypeView<?>> createNode(final TypeView<?> type) {
        final MutableContainableGraphNode<TypeView<?>> node = new SimpleGraphNode<>(type);
        final List<TypeView<?>> interfaces = type.genericInterfaces();
        for (final TypeView<?> anInterface : interfaces) {
            node.addChild(createNode(anInterface));
        }
        final TypeView<?> superClass = type.genericSuperClass();
        if (!superClass.isVoid()) {
            node.addChild(createNode(superClass));
        }
        return node;
    }
}
