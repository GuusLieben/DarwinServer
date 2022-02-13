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

package org.dockbox.hartshorn.demo.persistence.services;

import org.dockbox.hartshorn.core.annotations.inject.Required;
import org.dockbox.hartshorn.core.annotations.stereotype.Service;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.core.proxy.Proxy;
import org.dockbox.hartshorn.core.proxy.ProxyManager;
import org.dockbox.hartshorn.core.services.ServicePreProcessor;
import org.dockbox.hartshorn.data.annotations.Configuration;
import org.dockbox.hartshorn.data.jpa.JpaRepository;
import org.dockbox.hartshorn.demo.persistence.domain.User;
import org.dockbox.hartshorn.demo.persistence.events.UserCreatedEvent;
import org.dockbox.hartshorn.events.EventBus;
import org.dockbox.hartshorn.events.annotations.Posting;

import javax.inject.Inject;

/**
 * A simple component providing utility functions through a combination of injected services. Note that
 * unlike a service, a component does not automatically register functional methods (e.g. those annotated
 * with {@link org.dockbox.hartshorn.commands.annotations.Command} or {@link org.dockbox.hartshorn.events.annotations.Listener})
 * and instead only allows field injection. This improves construction times, as {@link ServicePreProcessor service processors}
 * do not activate on components.
 */
@Service
/* Indicates this type is responsible for firing the UserCreated event. This serves solely to satisfy the EventValidator, so any unhandled events are noticed on startup */
@Posting(UserCreatedEvent.class)
@Configuration(source = "classpath:persistence-demo.yml")
public abstract class UserRepository implements JpaRepository<User, Long> {

    @Inject
    @Required
    private EventBus eventBus;

    /**
     * Persists the given {@link User} with the given {@code name} and {@code age}, but without a {@code id}.
     * The {@link User} is saved to a configured {@link JpaRepository}, which is configured through the configuration
     * file indicated using {@link Configuration}.
     *
     * The real repository is not directly known to this service, instead method calls to {@link JpaRepository}
     * are automatically delegated to the real repository which is attached to this {@link UserRepository} instance.
     *
     * <p>However, as this repository overrides the method, it will not be proxied. To still be able to use the backing
     * {@link JpaRepository}, it is accessed through {@link ProxyManager#delegate(Class)}, which in turn is accessed
     * through {@link Proxy#manager()}, which can be cast to the current instance as it is a proxy instance.
     *
     * This is possible as backing implementations are attached as context to the {@link ProxyManager} responsible for
     * this {@link JpaRepository}.
     *
     * <p>After the user has been persisted, and the {@code id} has been generated by the {@link JpaRepository},
     * a {@link UserCreatedEvent} is posted, activating {@link EventListenerService#on(UserCreatedEvent)} which
     * logs the action.
     */
    @Override
    public User save(final User object) {
        final ProxyManager manager = ((Proxy) this).manager();
        final Exceptional<JpaRepository<User, Long>> delegate = manager.delegate(JpaRepository.class);
        delegate.get().save(object);
        this.eventBus.post(new UserCreatedEvent(object));
        return object;
    }
}
