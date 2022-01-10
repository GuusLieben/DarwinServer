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

package org.dockbox.hartshorn.core.context.element;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(staticName = "of")
public class MethodModifier<T, P> implements ElementModifier<MethodContext<T, P>> {

    @Getter
    private final MethodContext<T, P> element;

    public void invoker(final MethodInvoker<T, P> invoker) {
        this.element.invoker(invoker);
    }

    public void access(final boolean expose) {
        this.element().method().setAccessible(expose);
    }

    public static void defaultInvoker(final MethodInvoker<?, ?> invoker) {
        MethodContext.defaultInvoker(invoker);
    }
}
