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

package org.dockbox.hartshorn.data.service;

import org.dockbox.hartshorn.core.annotations.activate.AutomaticActivation;
import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.dockbox.hartshorn.core.proxy.ProxyFactory;
import org.dockbox.hartshorn.core.proxy.bytebuddy.BytebuddyProxyFactory;
import org.dockbox.hartshorn.core.services.ProxyDelegationPostProcessor;
import org.dockbox.hartshorn.data.annotations.UsePersistence;
import org.dockbox.hartshorn.data.jpa.JpaRepository;

import java.util.List;

@AutomaticActivation
public class JpaRepositoryDelegationPostProcessor extends ProxyDelegationPostProcessor<JpaRepository, UsePersistence> {
    @Override
    public Class<UsePersistence> activator() {
        return UsePersistence.class;
    }

    @Override
    protected Class<JpaRepository> parentTarget() {
        return JpaRepository.class;
    }

    @Override
    protected JpaRepository concreteDelegator(final ApplicationContext context, final ProxyFactory<JpaRepository, ?> handler, final TypeContext<? extends JpaRepository> parent) {
        final List<TypeContext<?>> list = TypeContext.of(((BytebuddyProxyFactory) handler).type()).typeParameters(JpaRepository.class);
        final Class<?> type = list.get(0).type();
        return context.get(JpaRepositoryFactory.class).repository(type);
    }
}
