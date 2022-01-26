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

package org.dockbox.hartshorn.events.handle;

import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.dockbox.hartshorn.events.parents.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

public final class EventHandlerRegistry {

    @Getter private final Map<TypeContext<? extends Event>, EventHandler> handlers = new ConcurrentHashMap<>();

    public EventHandler handler(final TypeContext<? extends Event> type) {
        EventHandler handler = this.handlers.get(type);
        if (null == handler) {
            this.computeHierarchy(handler = new EventHandler(type));
            this.handlers.put(type, handler);
        }
        return handler;
    }

    public void computeHierarchy(final EventHandler subject) {
        for (final EventHandler handler : this.handlers.values()) {
            if (subject == handler) continue;
            if (subject.subtypeOf(handler)) subject.addSuperTypeHandler(handler);
            else if (handler.subtypeOf(subject)) handler.addSuperTypeHandler(subject);
        }
    }
}
