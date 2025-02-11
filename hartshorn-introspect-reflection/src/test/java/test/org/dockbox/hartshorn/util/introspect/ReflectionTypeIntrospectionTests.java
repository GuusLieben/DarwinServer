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

package test.org.dockbox.hartshorn.util.introspect;

import org.dockbox.hartshorn.util.introspect.NativeProxyLookup;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.annotations.VirtualHierarchyAnnotationLookup;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionIntrospector;
import org.junit.jupiter.api.BeforeEach;

public class ReflectionTypeIntrospectionTests extends TypeIntrospectionTests {

    private Introspector introspector;

    @BeforeEach
    public void setup() {
        // Re-use the same introspector while inside a single test, so caching can be tested
        this.introspector = new ReflectionIntrospector(new NativeProxyLookup(), new VirtualHierarchyAnnotationLookup());
    }

    @Override
    protected Introspector introspector() {
        return this.introspector;
    }
}
