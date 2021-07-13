/*
 * Copyright (C) 2020 Guus Lieben
 *
 * This framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.hartshorn.di.context;

import org.dockbox.hartshorn.api.domain.Exceptional;
import org.dockbox.hartshorn.util.PrefixContext;

import java.util.List;

public class ReflectionContext extends PrefixContext implements Context {

    private final DefaultContext context;

    public ReflectionContext(Iterable<String> initialPrefixes) {
        super(initialPrefixes);
        this.context = new DefaultContext() {
        };
    }

    @Override
    public <C extends Context> Exceptional<C> first(Class<C> context) {
        return this.context.first(context);
    }

    @Override
    public <C extends Context> List<C> all(Class<C> context) {
        return this.context.all(context);
    }

    @Override
    public <C extends Context> void add(C context) {
        this.context.add(context);
    }
}
