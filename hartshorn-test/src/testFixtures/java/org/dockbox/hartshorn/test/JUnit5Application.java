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

package org.dockbox.hartshorn.test;

import org.dockbox.hartshorn.api.Hartshorn;
import org.dockbox.hartshorn.api.HartshornApplication;
import org.dockbox.hartshorn.di.ApplicationContextAware;
import org.dockbox.hartshorn.di.Modifier;
import org.dockbox.hartshorn.di.annotations.Activator;
import org.dockbox.hartshorn.di.annotations.InjectConfig;
import org.dockbox.hartshorn.di.annotations.InjectPhase;
import org.dockbox.hartshorn.test.util.JUnitInjector;
import org.dockbox.hartshorn.test.util.LateJUnitInjector;

import java.lang.reflect.Field;

import lombok.Getter;

@Activator(
        value = JUnit5Bootstrap.class,
        prefix = Hartshorn.PACKAGE_PREFIX,
        configs = {
                @InjectConfig(JUnitInjector.class),
                @InjectConfig(value = LateJUnitInjector.class, phase = InjectPhase.LATE)
        })
public class JUnit5Application {

    @Getter
    private static final JUnitInformation information = new JUnitInformation();

    public static void prepareBootstrap() throws NoSuchFieldException, IllegalAccessException {
        final Field instance = ApplicationContextAware.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        HartshornApplication.create(JUnit5Application.class, Modifier.ACTIVATE_ALL).run();
    }
}
