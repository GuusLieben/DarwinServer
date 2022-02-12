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

package org.dockbox.hartshorn.core.proxy.javassist;

import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.context.element.FieldContext;
import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.core.exceptions.ApplicationException;
import org.dockbox.hartshorn.core.proxy.DefaultProxyFactory;
import org.dockbox.hartshorn.core.proxy.LazyProxyManager;
import org.dockbox.hartshorn.core.proxy.Proxy;

import java.lang.reflect.InvocationTargetException;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class JavassistProxyFactory<T> extends DefaultProxyFactory<T> {

    static {
        ProxyFactory.nameGenerator = classname -> DefaultProxyFactory.NAME_GENERATOR.get(classname);
    }

    public JavassistProxyFactory(final Class<T> type, final ApplicationContext applicationContext) {
        super(type, applicationContext);
    }

    @Override
    public Exceptional<T> proxy() throws ApplicationException {
        final LazyProxyManager<T> manager = new LazyProxyManager<>(this.applicationContext(), this);
        final MethodHandler methodHandler = new JavassistProxyMethodHandler(manager, this.applicationContext());

        final Exceptional<T> proxy = this.type().isInterface()
                ? this.interfaceProxy(methodHandler)
                : this.concreteOrAbstractProxy(methodHandler);

        proxy.present(manager::proxy);
        return proxy;
    }

    protected Exceptional<T> concreteOrAbstractProxy(final MethodHandler methodHandler) throws ApplicationException {
        final ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(this.type());
        factory.setInterfaces(new Class[] { Proxy.class});

        try {
            final T proxy = (T) factory.create(new Class<?>[0], new Object[0], methodHandler);
            if (this.typeDelegate() != null) this.restoreFields(this.typeDelegate(), proxy);
            return Exceptional.of(proxy);
        }
        catch (final InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new ApplicationException(e);
        }
    }

    protected Exceptional<T> interfaceProxy(final MethodHandler methodHandler) throws ApplicationException {
        final T proxy = (T) java.lang.reflect.Proxy.newProxyInstance(
                this.type().getClassLoader(),
                new Class[] { this.type(), Proxy.class },
                (final var self, final var method, final var args) -> methodHandler.invoke(self, method, null, args));
        return Exceptional.of(proxy);
    }

    protected void restoreFields(final T existing, final T proxy) {
        final TypeContext<T> typeContext = this.applicationContext().environment().manager().isProxy(existing)
                ? TypeContext.of(this.type())
                : TypeContext.of(this.typeDelegate());
        for (final FieldContext<?> field : typeContext.fields()) {
            if (field.isStatic()) continue;
            field.set(proxy, field.get(existing).orNull());
        }
    }
}
