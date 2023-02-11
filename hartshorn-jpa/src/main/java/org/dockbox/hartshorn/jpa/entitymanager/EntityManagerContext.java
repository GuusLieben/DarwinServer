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

package org.dockbox.hartshorn.jpa.entitymanager;

import org.dockbox.hartshorn.context.DefaultProvisionContext;

import java.util.function.Supplier;

import jakarta.persistence.EntityManager;

public class EntityManagerContext extends DefaultProvisionContext {

    private final Supplier<EntityManager> supplier;

    public EntityManagerContext(final Supplier<EntityManager> supplier) {
        this.supplier = supplier;
    }

    public EntityManager entityManager() {
        return this.supplier.get();
    }
}
