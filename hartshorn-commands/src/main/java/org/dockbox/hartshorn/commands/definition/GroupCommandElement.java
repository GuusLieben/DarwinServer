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

package org.dockbox.hartshorn.commands.definition;

import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.commands.CommandSource;

import java.util.Collection;
import java.util.List;

/**
 * Represents a group of {@link CommandElement command elements}. This is typically used when
 * a single argument is only valid in combination with another argument.
 */
public class GroupCommandElement implements CommandElement<List<CommandElement<?>>> {

    private final List<CommandElement<?>> elements;
    private final String name;
    private final boolean optional;
    private final int size;

    public GroupCommandElement(final List<CommandElement<?>> elements, final boolean optional) {
        this.elements = elements;
        final List<String> names = elements.stream().map(CommandElement::name).toList();
        this.name = "group: " + String.join(", ", names);
        this.size = elements.stream().mapToInt(CommandElement::size).sum();
        this.optional = optional;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean optional() {
        return this.optional;
    }

    @Override
    public Exceptional<List<CommandElement<?>>> parse(final CommandSource source, final String argument) {
        return Exceptional.of(this.elements);
    }

    @Override
    public Collection<String> suggestions(final CommandSource source, final String argument) {
        throw new UnsupportedOperationException("Collecting suggestions from element groups is not supported, target singular elements instead.");
    }

    @Override
    public int size() {
        return this.size;
    }
}
