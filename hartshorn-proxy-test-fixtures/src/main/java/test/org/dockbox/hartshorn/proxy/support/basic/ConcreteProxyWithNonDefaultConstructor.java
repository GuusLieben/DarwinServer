/*
 * Copyright 2019-2024 the original author or authors.
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

package test.org.dockbox.hartshorn.proxy.support.basic;

/**
 * A concrete class with a non-default constructor.
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
@SuppressWarnings("ClassCanBeRecord") // Intended for proxy testing
public class ConcreteProxyWithNonDefaultConstructor {

    private final String message;

    public ConcreteProxyWithNonDefaultConstructor(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }
}
