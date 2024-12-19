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

package org.dockbox.hartshorn.inject.component;

import org.dockbox.hartshorn.util.ApplicationRuntimeException;

/**
 * Exception to indicate a component is invalid. This can be caused by a number of reasons, such as missing annotations,
 * invalid constructors, or incorrect types.
 *
 * @since 0.4.12
 *
 * @author Guus Lieben
 */
public class InvalidComponentException extends ApplicationRuntimeException {
    public InvalidComponentException(String message) {
        super(message);
    }
}
