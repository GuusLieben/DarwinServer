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

package org.dockbox.hartshorn.cache.annotations;

import org.dockbox.hartshorn.cache.KeyGenerator;
import org.dockbox.hartshorn.util.introspect.annotations.AliasFor;
import org.dockbox.hartshorn.util.introspect.annotations.Extends;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method decorator to indicate it can be used to update a cache. This
 * requires the method parameter to be assignable to the type stored in
 * the cache.
 * @author Guus Lieben
 * @since 21.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Extends(CacheDecorator.class)
public @interface UpdateCache {

    /**
     * @see CacheDecorator#cacheName()
     */
    @AliasFor("cacheName")
    String value() default "";

    /**
     * @see CacheDecorator#keyGenerator()
     */
    Class<? extends KeyGenerator> keyGenerator() default KeyGenerator.class;

    /**
     * @see CacheDecorator#key()
     */
    String key() default "";
}
