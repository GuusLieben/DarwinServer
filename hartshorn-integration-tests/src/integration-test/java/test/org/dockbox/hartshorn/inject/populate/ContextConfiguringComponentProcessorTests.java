/*
 * Copyright 2019-2025 the original author or authors.
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

package test.org.dockbox.hartshorn.inject.populate;

import org.dockbox.hartshorn.inject.annotations.Inject;
import org.dockbox.hartshorn.inject.ContextKey;
import org.dockbox.hartshorn.proxy.Proxy;
import org.dockbox.hartshorn.proxy.ProxyOrchestrator;
import org.dockbox.hartshorn.test.annotations.TestComponents;
import org.dockbox.hartshorn.test.junit.HartshornIntegrationTest;
import org.dockbox.hartshorn.util.option.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@HartshornIntegrationTest(
    includeBasePackages = false,
    componentPostProcessors = SimpleContextConfiguringComponentProcessor.class
)
public class ContextConfiguringComponentProcessorTests {

    @Test
    @TestComponents(components = EmptyComponent.class)
    void testNonContextComponentIsProcessed(@Inject EmptyComponent emptyComponent, @Inject ProxyOrchestrator proxyOrchestrator) {
        Assertions.assertNotNull(emptyComponent);
        Assertions.assertTrue(proxyOrchestrator.isProxy(emptyComponent));

        Proxy<EmptyComponent> component = (Proxy<EmptyComponent>) emptyComponent;
        Option<SimpleContext> context = component.manager().firstContext(ContextKey.of(SimpleContext.class));
        Assertions.assertTrue(context.present());
        Assertions.assertEquals("Foo", context.get().value());
    }

    @Test
    @TestComponents(components = ContextComponent.class)
    void testContextComponentIsProcessed(@Inject ContextComponent contextComponent, @Inject ProxyOrchestrator proxyOrchestrator) {
        Assertions.assertNotNull(contextComponent);
        Assertions.assertFalse(proxyOrchestrator.isProxy(contextComponent));

        Option<SimpleContext> context = contextComponent.firstContext(ContextKey.of(SimpleContext.class));
        Assertions.assertTrue(context.present());
        Assertions.assertEquals("Foo", context.get().value());
    }
}
