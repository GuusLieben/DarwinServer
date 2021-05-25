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

package org.dockbox.selene.di.context;

import org.dockbox.selene.api.domain.Exceptional;
import org.dockbox.selene.di.InjectionPoint;
import org.dockbox.selene.di.ProvisionFailure;
import org.dockbox.selene.di.inject.InjectionModifier;
import org.dockbox.selene.di.properties.InjectorProperty;
import org.dockbox.selene.di.services.ServiceProcessor;

import java.lang.annotation.Annotation;
import java.util.List;

public interface ApplicationContext extends ApplicationBinder, SeleneContext {

    void add(InjectionPoint<?> property);

    <T> T create(Class<T> type, T typeInstance, InjectorProperty<?>... properties);

    <T> T inject(Class<T> type, T typeInstance, InjectorProperty<?>... properties);

    <T> void enable(T typeInstance);

    <T> T raw(Class<T> type) throws ProvisionFailure;
    <T> T raw(Class<T> type, boolean populate) throws ProvisionFailure;

    void add(ServiceProcessor<?> processor);
    void add(InjectionModifier<?> modifier);

    Class<?> getActivationSource();
    List<Annotation> activators();
    boolean hasActivator(Class<? extends Annotation> activator);
    <A> A activator(Class<A> activator);

    <C extends Context> Exceptional<C> first(Class<C> context);
    <C extends Context> List<C> all(Class<C> context);
    <C extends Context> void add(C context);
}
