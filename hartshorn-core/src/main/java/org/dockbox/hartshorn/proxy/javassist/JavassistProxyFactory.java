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

package org.dockbox.hartshorn.proxy.javassist;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.proxy.DefaultProxyFactory;
import org.dockbox.hartshorn.proxy.JDKInterfaceProxyFactory;
import org.dockbox.hartshorn.proxy.Proxy;
import org.dockbox.hartshorn.proxy.ProxyConstructorFunction;
import org.dockbox.hartshorn.proxy.StandardMethodInterceptor;

import java.util.function.Consumer;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class JavassistProxyFactory<T> extends JDKInterfaceProxyFactory<T> {

    static {
        ProxyFactory.nameGenerator = classname -> DefaultProxyFactory.NAME_GENERATOR.get(classname);
    }

    public JavassistProxyFactory(final Class<T> type, final ApplicationContext applicationContext, final Consumer<Proxy<?>> registrationFunction) {
        super(type, applicationContext, registrationFunction);
    }

    @Override
    protected ProxyConstructorFunction<T> concreteOrAbstractEnhancer(final StandardMethodInterceptor<T> interceptor) {
        final ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(this.type());
        factory.setInterfaces(this.proxyInterfaces(false));

        final MethodHandler methodHandler = new JavassistProxyMethodHandler<>(interceptor);
        return new JavassistProxyConstructorFunction<>(this.type(), factory, methodHandler);
    }
}
