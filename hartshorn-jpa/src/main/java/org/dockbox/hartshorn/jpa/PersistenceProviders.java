/*
 * Copyright 2019-2023 the original author or authors.
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

package org.dockbox.hartshorn.jpa;

import jakarta.inject.Singleton;
import org.dockbox.hartshorn.component.Service;
import org.dockbox.hartshorn.component.condition.RequiresActivator;
import org.dockbox.hartshorn.component.processing.Binds;
import org.dockbox.hartshorn.component.processing.ProcessingPriority;
import org.dockbox.hartshorn.jpa.annotations.UsePersistence;
import org.dockbox.hartshorn.jpa.entitymanager.EntityManagerLookup;
import org.dockbox.hartshorn.jpa.entitymanager.EntityTypeLookup;
import org.dockbox.hartshorn.jpa.entitymanager.JpaEntityTypeLookup;
import org.dockbox.hartshorn.jpa.entitymanager.ProxyAttachedEntityManagerLookup;
import org.dockbox.hartshorn.jpa.query.context.AggregateJpaQueryContextCreator;
import org.dockbox.hartshorn.jpa.query.context.JpaQueryContextCreator;
import org.dockbox.hartshorn.jpa.query.context.named.ImplicitNamedJpaQueryContextCreator;
import org.dockbox.hartshorn.jpa.query.context.named.NamedJpaQueryContextCreator;
import org.dockbox.hartshorn.jpa.query.context.unnamed.UnnamedJpaQueryContextCreator;
import org.dockbox.hartshorn.util.introspect.util.ParameterLoader;

@Service
@RequiresActivator(UsePersistence.class)
public class PersistenceProviders {

    @Binds("jpa_query")
    public ParameterLoader<?> jpaParameterLoader() {
        return new JpaParameterLoader();
    }

    @Binds
    public EntityManagerLookup entityManagerLookup() {
        return new ProxyAttachedEntityManagerLookup();
    }

    @Binds(phase = ProcessingPriority.HIGH_PRECEDENCE)
    public EntityTypeLookup entityTypeLookup() {
        return new JpaEntityTypeLookup();
    }

    @Binds
    @Singleton
    public JpaQueryContextCreator queryContextFactory(final EntityTypeLookup entityTypeLookup) {
        final AggregateJpaQueryContextCreator contextFactory = new AggregateJpaQueryContextCreator();
        contextFactory.register(0, new ImplicitNamedJpaQueryContextCreator(entityTypeLookup));
        contextFactory.register(25, new UnnamedJpaQueryContextCreator());
        contextFactory.register(50, new NamedJpaQueryContextCreator());
        return contextFactory;
    }
}
