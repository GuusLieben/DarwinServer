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

package org.dockbox.selene.api.events.listeners;

import org.dockbox.selene.api.events.SampleEvent;
import org.dockbox.selene.api.events.annotations.Listener;
import org.dockbox.selene.api.events.annotations.Listener.Priority;
import org.junit.jupiter.api.Assertions;

public class PriorityEventListener {

    private Priority last = null;

    @Listener(Priority.FIRST)
    public void onFirst(SampleEvent event) {
        Assertions.assertNull(this.last);
        this.last = Priority.FIRST;
    }

    @Listener(Priority.EARLY)
    public void onEarly(SampleEvent event) {
        Assertions.assertEquals(this.last, Priority.FIRST);
        this.last = Priority.EARLY;
    }

    @Listener(Priority.NORMAL)
    public void onNormal(SampleEvent event) {
        Assertions.assertEquals(this.last, Priority.EARLY);
        this.last = Priority.NORMAL;
    }

    @Listener(Priority.LATE)
    public void onLate(SampleEvent event) {
        Assertions.assertEquals(this.last, Priority.NORMAL);
        this.last = Priority.LATE;
    }

    @Listener(Priority.LAST)
    public void onLast(SampleEvent event) {
        Assertions.assertEquals(this.last, Priority.LATE);
        this.last = Priority.LAST;
    }

    public Priority getLast() {
        return this.last;
    }
}
