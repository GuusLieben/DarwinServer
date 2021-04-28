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

package org.dockbox.selene.commands;

import org.dockbox.selene.api.BootstrapPhase;
import org.dockbox.selene.api.Phase;
import org.dockbox.selene.api.SeleneInformation;
import org.dockbox.selene.commands.annotations.ArgumentProvider;
import org.dockbox.selene.di.preload.Preloadable;
import org.dockbox.selene.di.Provider;
import org.dockbox.selene.util.Reflect;

@Phase(BootstrapPhase.PRE_INIT)
class ArgumentProvisionScanner implements Preloadable {

    @Override
    public void preload() {
        CommandBus bus = Provider.provide(CommandBus.class);
        Reflect.registerModuleInitBus(bus::register);
        Reflect.registerModulePostInit(bus::apply);

        // Register additional argument types early on, before modules are constructed
        Reflect.annotatedTypes(SeleneInformation.PACKAGE_PREFIX, ArgumentProvider.class)
                .forEach(Provider::provide);
    }
}
