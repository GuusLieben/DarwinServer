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

package test.org.dockbox.hartshorn.properties.loader;

import org.dockbox.hartshorn.properties.ConfiguredProperty;
import org.dockbox.hartshorn.properties.MapPropertyRegistry;
import org.dockbox.hartshorn.properties.PropertyRegistry;
import org.dockbox.hartshorn.properties.loader.PropertyRegistryPathLoader;
import org.dockbox.hartshorn.properties.loader.StylePropertyPathFormatter;
import org.dockbox.hartshorn.properties.loader.support.JacksonYamlPropertyRegistryLoader;
import org.dockbox.hartshorn.properties.value.StandardValuePropertyParsers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class JacksonPropertyRegistryPathLoaderTests {

    @Test
    void testComplexYamlConfigurationCanBeLoaded() throws IOException {
        // Given
        PropertyRegistryPathLoader loader = new JacksonYamlPropertyRegistryLoader(new StylePropertyPathFormatter());
        Path path = Path.of("src/test/resources/complex-configuration.yml");

        // When: Loading registry
        PropertyRegistry registry = new MapPropertyRegistry();
        loader.loadRegistry(registry, path);

        // Then: Should contain all expected keys
        List<ConfiguredProperty> properties = registry.find(property -> true);
        Assertions.assertEquals(12, properties.size());

        // Then: Keys should be ordered
        Iterator<ConfiguredProperty> iterator = properties.iterator();
        Assertions.assertEquals("sample.complex.configuration[0].name", iterator.next().name());
        Assertions.assertEquals("sample.complex.configuration[0].value", iterator.next().name());
        Assertions.assertEquals("sample.complex.configuration[1].name", iterator.next().name());
        Assertions.assertEquals("sample.complex.configuration[1].value", iterator.next().name());
        Assertions.assertEquals("sample.complex.configuration[2].name", iterator.next().name());
        Assertions.assertEquals("sample.complex.configuration[2].value", iterator.next().name());
        Assertions.assertEquals("sample.complex.flat", iterator.next().name());
        Assertions.assertEquals("sample.complex.list[0].name", iterator.next().name());
        Assertions.assertEquals("sample.complex.list[0].value", iterator.next().name());
        Assertions.assertEquals("sample.complex.list[1].name", iterator.next().name());
        Assertions.assertEquals("sample.complex.list[1].value", iterator.next().name());
        Assertions.assertEquals("sample.complex.values", iterator.next().name());

        // Then: Property values should be loaded correctly
        registry.value("sample.complex.configuration[0].name")
                .peek(value -> Assertions.assertEquals("name1", value))
                .orElseThrow(() -> new AssertionError("Property not found"));
        registry.value("sample.complex.configuration[0].value")
                .peek(value -> Assertions.assertEquals("value1", value))
                .orElseThrow(() -> new AssertionError("Property not found"));

        registry.value("sample.complex.configuration[1].name")
                .peek(value -> Assertions.assertEquals("name2", value))
                .orElseThrow(() -> new AssertionError("Property not found"));
        registry.value("sample.complex.configuration[1].value")
                .peek(value -> Assertions.assertEquals("value2", value))
                .orElseThrow(() -> new AssertionError("Property not found"));

        registry.value("sample.complex.configuration[2].name")
                .peek(value -> Assertions.assertEquals("name3", value))
                .orElseThrow(() -> new AssertionError("Property not found"));
        registry.value("sample.complex.configuration[2].value")
                .peek(value -> Assertions.assertEquals("value3", value))
                .orElseThrow(() -> new AssertionError("Property not found"));

        registry.value("sample.complex.values", StandardValuePropertyParsers.STRING_LIST)
                .peek(values -> {
                    Assertions.assertEquals(3, values.length);
                    Assertions.assertEquals("value1", values[0]);
                    Assertions.assertEquals("value2", values[1]);
                    Assertions.assertEquals("value3", values[2]);
                })
                .orElseThrow(() -> new AssertionError("Property not found"));

        registry.value("sample.complex.flat")
                .peek(value -> Assertions.assertEquals("value1", value))
                .orElseThrow(() -> new AssertionError("Property not found"));

        registry.value("sample.complex.list[0].name")
                .peek(value -> Assertions.assertEquals("name1", value))
                .orElseThrow(() -> new AssertionError("Property not found"));
        registry.value("sample.complex.list[0].value")
                .peek(value -> Assertions.assertEquals("value1", value))
                .orElseThrow(() -> new AssertionError("Property not found"));

        registry.value("sample.complex.list[1].name")
                .peek(value -> Assertions.assertEquals("name2", value))
                .orElseThrow(() -> new AssertionError("Property not found"));
        registry.value("sample.complex.list[1].value")
                .peek(value -> Assertions.assertEquals("value2", value))
                .orElseThrow(() -> new AssertionError("Property not found"));
    }
}
