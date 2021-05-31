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

package org.dockbox.selene.di;

import org.dockbox.selene.di.context.ApplicationContext;
import org.dockbox.selene.di.context.SeleneApplicationContext;

import lombok.Getter;

public class ApplicationContextAware {

    @Getter
    private ApplicationContext context;

    private static ApplicationContextAware instance;

    public void create(Class<?> activationSource, Modifier... modifiers) {
        this.context = new SeleneApplicationContext(activationSource, modifiers);
    }

    public static ApplicationContextAware instance() {
        return instance;
    }

    protected static void instance(ApplicationContextAware bootstrap) {
        ApplicationContextAware.instance = bootstrap;
    }

}
