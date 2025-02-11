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

package test.org.dockbox.hartshorn.proxy.support.standard;

import test.org.dockbox.hartshorn.proxy.support.inheritance.single.InterfaceProxy;

/**
 * Simple record used to verify records cannot be proxied (as they are inherently final).
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
public record RecordProxy() implements InterfaceProxy {
    @Override
    public String name() {
        return "Record";
    }

    @Override
    public int age() {
        return 1;
    }
}
