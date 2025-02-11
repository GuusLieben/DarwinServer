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

package test.org.dockbox.hartshorn.inject.provider;

import java.util.stream.Stream;

import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.ComponentResolutionException;
import org.dockbox.hartshorn.inject.annotations.Inject;
import org.dockbox.hartshorn.launchpad.ApplicationContext;
import org.dockbox.hartshorn.test.annotations.TestBinding;
import org.dockbox.hartshorn.test.annotations.TestComponents;
import org.dockbox.hartshorn.test.junit.HartshornIntegrationTest;
import org.dockbox.hartshorn.util.ApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import test.org.dockbox.hartshorn.inject.populate.PopulatedType;

@HartshornIntegrationTest(includeBasePackages = false)
public class ProviderBehaviorTests {

    @Inject
    private ApplicationContext applicationContext;

    @Test
    public void testStaticBindingCanBeProvided() {
        this.applicationContext.bind(SampleInterface.class).to(SampleImplementation.class);
        SampleInterface provided = this.applicationContext.get(SampleInterface.class);
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleImplementation.class, providedClass);

        Assertions.assertEquals(SampleImplementation.NAME, provided.name());
    }

    @Test
    public void testStaticBindingWithMetaCanBeProvided() {
        ComponentKey<SampleInterface> key = ComponentKey.of(SampleInterface.class, "demo");
        this.applicationContext.bind(key).to(SampleImplementation.class);
        SampleInterface provided = this.applicationContext.get(key);
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleImplementation.class, providedClass);

        Assertions.assertEquals(SampleImplementation.NAME, provided.name());
    }

    @Test
    public void testInstanceBindingCanBeProvided() {
        this.applicationContext.bind(SampleInterface.class).singleton(new SampleImplementation());
        SampleInterface provided = this.applicationContext.get(SampleInterface.class);
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleImplementation.class, providedClass);

        Assertions.assertEquals(SampleImplementation.NAME, provided.name());
    }

    @Test
    public void testInstanceBindingWithMetaCanBeProvided() {
        ComponentKey<SampleInterface> key = ComponentKey.of(SampleInterface.class, "demo");
        this.applicationContext.bind(key).singleton(new SampleImplementation());
        SampleInterface provided = this.applicationContext.get(key);
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleImplementation.class, providedClass);

        Assertions.assertEquals(SampleImplementation.NAME, provided.name());
    }

    @Test
    public void testProviderBindingCanBeProvided() {
        this.applicationContext.bind(SampleInterface.class).to(SampleImplementation::new);
        SampleInterface provided = this.applicationContext.get(SampleInterface.class);
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleImplementation.class, providedClass);

        Assertions.assertEquals(SampleImplementation.NAME, provided.name());
    }

    @Test
    public void testProviderBindingWithMetaCanBeProvided() {
        ComponentKey<SampleInterface> key = ComponentKey.of(SampleInterface.class, "demo");
        this.applicationContext.bind(key).to(SampleImplementation::new);
        SampleInterface provided = this.applicationContext.get(key);
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleImplementation.class, providedClass);

        Assertions.assertEquals(SampleImplementation.NAME, provided.name());
    }

    @Test
    @TestComponents(components = SampleConfiguration.class)
    public void testScannedMetaBindingsCanBeProvided() {

        // Ensure that the binding is not bound to the default name
        Assertions.assertThrows(ComponentResolutionException.class, () -> this.applicationContext.get(SampleInterface.class));

        SampleInterface provided = this.applicationContext.get(ComponentKey.of(SampleInterface.class, "meta"));
        Assertions.assertNotNull(provided);

        Class<? extends SampleInterface> providedClass = provided.getClass();
        Assertions.assertSame(SampleMetaAnnotatedImplementation.class, providedClass);

        Assertions.assertEquals("MetaAnnotatedHartshorn", provided.name());
    }

    @Test
    @TestComponents(
            components = PopulatedType.class,
            bindings = @TestBinding(type = SampleInterface.class, implementation = SampleImplementation.class)
    )
    public void unboundTypesCanBeProvided() {
        PopulatedType provided = this.applicationContext.get(PopulatedType.class);
        Assertions.assertNotNull(provided);
        Assertions.assertNotNull(provided.sampleInterface());
    }

    @Test
    void testFailureInComponentConstructorYieldsInitializationException() {
        ComponentResolutionException exception = Assertions.assertThrows(ComponentResolutionException.class, () -> this.applicationContext.get(
                ErrorInConstructorObject.class));
        Throwable cause = exception.getCause();
        Assertions.assertNotNull(cause);
        Assertions.assertTrue(cause instanceof ApplicationException);

        ApplicationException applicationException = (ApplicationException) cause;
        Assertions.assertEquals("Failed to create instance of type " + ErrorInConstructorObject.class.getName(), applicationException.getMessage());
    }

    @Test
    void testFailingConstructorIsRethrown() {
        ComponentResolutionException exception = Assertions.assertThrows(ComponentResolutionException.class, () -> this.applicationContext.get(
                TypeWithFailingConstructor.class));
        Assertions.assertTrue(exception.getCause() instanceof ApplicationException);

        ApplicationException applicationException = (ApplicationException) exception.getCause();
        Assertions.assertTrue(applicationException.getCause() instanceof IllegalStateException);

        IllegalStateException illegalStateException = (IllegalStateException) applicationException.getCause();
        Assertions.assertEquals(TypeWithFailingConstructor.ERROR_MESSAGE, illegalStateException.getMessage());
    }

    @Test
    void testStringProvision() {
        ComponentKey<String> key = ComponentKey.of(String.class, "license");
        this.applicationContext.bind(key).singleton("MIT");
        String license = this.applicationContext.get(key);
        Assertions.assertEquals("MIT", license);
    }

    @ParameterizedTest
    @MethodSource("providers")
    @TestComponents(components = { SampleFieldImplementation.class, SampleProviderConfiguration.class})
    void testProvidersCanApply(String meta, String name, boolean field, String fieldMeta, boolean singleton) {
        if (field) {
            if (fieldMeta == null) {this.applicationContext.bind(SampleField.class).to(SampleFieldImplementation.class);}
            else {
                this.applicationContext.bind(ComponentKey.of(SampleField.class, fieldMeta)).to(SampleFieldImplementation.class);
            }
        }

        ProvidedInterface provided;
        if (meta == null) {
            provided = this.applicationContext.get(ProvidedInterface.class);
        }
        else {
            provided = this.applicationContext.get(ComponentKey.of(ProvidedInterface.class, meta));
        }
        Assertions.assertNotNull(provided);

        String actual = provided.name();
        Assertions.assertNotNull(name);
        Assertions.assertEquals(name, actual);

        if (singleton) {
            ProvidedInterface second;
            if (meta == null) {
                second = this.applicationContext.get(ProvidedInterface.class);
            }
            else {
                second = this.applicationContext.get(ComponentKey.of(ProvidedInterface.class, meta));
            }
            Assertions.assertNotNull(second);
            Assertions.assertSame(provided, second);
        }
    }

    private static Stream<Arguments> providers() {
        return Stream.of(
                Arguments.of(null, "Provision", false, null, false),
                Arguments.of("named", "NamedProvision", false, null, false),
                Arguments.of("parameter", "ParameterProvision", true, null, false),
                Arguments.of("namedParameter", "NamedParameterProvision", true, "named", false),
                Arguments.of("singleton", "SingletonProvision", false, null, true)
        );
    }
}
