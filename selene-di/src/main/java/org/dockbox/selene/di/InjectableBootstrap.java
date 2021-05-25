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

import com.google.common.collect.Multimap;

import org.dockbox.selene.di.annotations.InjectPhase;
import org.dockbox.selene.di.context.ManagedSeleneContext;
import org.dockbox.selene.di.inject.InjectionModifier;
import org.dockbox.selene.di.services.ServiceProcessor;
import org.dockbox.selene.util.Reflect;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

@SuppressWarnings({ "AbstractClassWithoutAbstractMethods", "unchecked", "rawtypes" })
public abstract class InjectableBootstrap extends ApplicationContextAware {

    private static InjectableBootstrap instance;

    public abstract void init();

    public void create(String prefix, Class<?> activationSource, List<Annotation> activators, Multimap<InjectPhase, InjectConfiguration> configs) {
        super.create(activationSource);
        for (Annotation activator : activators) {
            ((ManagedSeleneContext) this.getContext()).addActivator(activator);
        }
        instance(this);
        this.lookupProcessors(prefix);
        this.lookupModifiers(prefix);

        Reflections.log = null; // Don't output Reflections

        for (InjectConfiguration config : configs.get(InjectPhase.EARLY)) super.getContext().bind(config);
        super.getContext().bind(prefix);
        for (InjectConfiguration config : configs.get(InjectPhase.LATE)) super.getContext().bind(config);
    }

    private void lookupProcessors(String prefix) {
        final Collection<Class<? extends ServiceProcessor>> processors = Reflect.subTypes(prefix, ServiceProcessor.class);
        for (Class<? extends ServiceProcessor> processor : processors) {
            if (!Reflect.isConcrete(processor)) continue;

            final ServiceProcessor raw = super.getContext().raw(processor, false);
            if (this.getContext().hasActivator(raw.activator()))
                super.getContext().add(raw);
        }
    }

    private void lookupModifiers(String prefix) {
        final Collection<Class<? extends InjectionModifier>> modifiers = Reflect.subTypes(prefix, InjectionModifier.class);
        for (Class<? extends InjectionModifier> modifier : modifiers) {
            if (!Reflect.isConcrete(modifier)) continue;

            final InjectionModifier raw = super.getContext().raw(modifier, false);
            if (this.getContext().hasActivator(raw.activator()))
                super.getContext().add(raw);
        }
    }

    public static InjectableBootstrap instance() {
        return (InjectableBootstrap) ApplicationContextAware.instance();
    }
}
