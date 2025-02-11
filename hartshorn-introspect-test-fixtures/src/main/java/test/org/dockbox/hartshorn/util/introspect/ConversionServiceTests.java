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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.convert.ConversionService;
import org.dockbox.hartshorn.util.introspect.convert.Converter;
import org.dockbox.hartshorn.util.introspect.convert.ConverterCache;
import org.dockbox.hartshorn.util.introspect.convert.ConverterRegistry;
import org.dockbox.hartshorn.util.introspect.convert.DefaultValueProvider;
import org.dockbox.hartshorn.util.introspect.convert.GenericConverter;
import org.dockbox.hartshorn.util.introspect.convert.GenericConverters;
import org.dockbox.hartshorn.util.introspect.convert.NullAccess;
import org.dockbox.hartshorn.util.introspect.convert.StandardConversionService;
import org.dockbox.hartshorn.util.option.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import test.org.dockbox.hartshorn.util.introspect.support.basic.TestEnumType;

/**
 * Tests for the {@link ConversionService} interface, driven by configurable {@link Introspector}
 * implementations.
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
public abstract class ConversionServiceTests {

    protected abstract Introspector introspector();

    protected ConversionService conversionService() {
        return new StandardConversionService(this.introspector()).withDefaults();
    }

    @Test
    void testPrimitiveWrapperFromEmptyString() {
        ConversionService conversionService = this.conversionService();
        Integer empty = conversionService.convert("", Integer.class);
        Assertions.assertNotNull(empty);
        Assertions.assertEquals(0, empty);
    }

    @Test
    void testNumberFromString() {
        ConversionService conversionService = this.conversionService();
        Integer valid = conversionService.convert("12", Integer.class);
        Assertions.assertNotNull(valid);
        Assertions.assertEquals(12, valid);
    }

    @Test
    void testPrimitiveFromString() {
        ConversionService conversionService = this.conversionService();
        int primitive = conversionService.convert("12", int.class);
        Assertions.assertEquals(12, primitive);
    }

    @Test
    void testEnumFromString() {
        ConversionService conversionService = this.conversionService();
        TestEnumType a = conversionService.convert("A", TestEnumType.class);
        Assertions.assertNotNull(a);
        Assertions.assertSame(TestEnumType.A, a);

        TestEnumType d = conversionService.convert("D", TestEnumType.class);
        Assertions.assertNull(d);
    }

    @Test
    void testOptionFromObject() {
        ConversionService conversionService = this.conversionService();
        Option<?> option = conversionService.convert("test", Option.class);
        Assertions.assertNotNull(option);
        Assertions.assertTrue(option.present());
        Assertions.assertEquals("test", option.get());
    }

    @Test
    void testObjectFromOption() {
        ConversionService conversionService = this.conversionService();
        String converted = conversionService.convert(Option.of("test"), String.class);
        Assertions.assertNotNull(converted);
        Assertions.assertEquals("test", converted);
    }

    @Test
    void testOptionDefaultValue() {
        ConversionService conversionService = this.conversionService();
        Option<?> optionFromNull = conversionService.convert(null, Option.class);
        Assertions.assertNotNull(optionFromNull);
        Assertions.assertFalse(optionFromNull.present());
    }

    @Test
    void testStringDefaultValue() {
        ConversionService conversionService = this.conversionService();
        String stringFromNull = conversionService.convert(null, String.class);
        Assertions.assertNotNull(stringFromNull);
        Assertions.assertEquals("", stringFromNull);
    }

    @Test
    void testPrimitiveDefaultValue() {
        ConversionService conversionService = this.conversionService();
        int intFromNull = conversionService.convert(null, int.class);
        Assertions.assertEquals(0, intFromNull);
    }

    @Test
    void testPrimitiveWrapperDefaultValue() {
        ConversionService conversionService = this.conversionService();
        Integer integerFromNull = conversionService.convert(null, Integer.class);
        Assertions.assertNotNull(integerFromNull);
        Assertions.assertEquals(0, integerFromNull);
    }

    @Test
    void testConcreteSetDefaultValue() {
        ConversionService conversionService = this.conversionService();
        Set<?> hashSet = conversionService.convert(null, TreeSet.class);
        Assertions.assertNotNull(hashSet);
        Assertions.assertTrue(hashSet.isEmpty());
        Assertions.assertTrue(hashSet instanceof TreeSet);
    }

    @Test
    void testSetDefaultValue() {
        ConversionService conversionService = this.conversionService();
        Set<?> set = conversionService.convert(null, Set.class);
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    void testListDefaultValue() {
        ConversionService conversionService = this.conversionService();
        List<?> list = conversionService.convert(null, List.class);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    void testConcreteListDefaultValue() {
        ConversionService conversionService = this.conversionService();
        List<?> arrayList = conversionService.convert(null, CopyOnWriteArrayList.class);
        Assertions.assertNotNull(arrayList);
        Assertions.assertTrue(arrayList.isEmpty());
        Assertions.assertTrue(arrayList instanceof CopyOnWriteArrayList);
    }

    @Test
    void testCollectionFromObject() {
        ConversionService conversionService = this.conversionService();
        List<?> linkedList = conversionService.convert("test", LinkedList.class);
        Assertions.assertNotNull(linkedList);
        Assertions.assertFalse(linkedList.isEmpty());
        Assertions.assertEquals("test", linkedList.get(0));
    }

    @Test
    void testCollectionFromCollection() {
        ConversionService conversionService = this.conversionService();
        Set<?> setFromList = conversionService.convert(List.of(1, 2, 3), Set.class);
        Assertions.assertNotNull(setFromList);
        Assertions.assertFalse(setFromList.isEmpty());
        Assertions.assertEquals(3, setFromList.size());
    }

    @Test
    public void testCollectionFromOption() {
        ConversionService conversionService = this.conversionService();
        List<?> listFromOption = conversionService.convert(Option.of("123"), List.class);
        Assertions.assertNotNull(listFromOption);
        Assertions.assertFalse(listFromOption.isEmpty());
        Assertions.assertEquals(1, listFromOption.size());
        Assertions.assertEquals("123", listFromOption.get(0));
    }

    @Test
    void testArrayFromCollection() {
        ConversionService conversionService = this.conversionService();
        String[] array = conversionService.convert(List.of("1", "2", "3"), String[].class);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("1", array[0]);
        Assertions.assertEquals("2", array[1]);
        Assertions.assertEquals("3", array[2]);
    }

    @Test
    void testArrayFromObject() {
        ConversionService conversionService = this.conversionService();
        String[] array = conversionService.convert("test", String[].class);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(1, array.length);
        Assertions.assertEquals("test", array[0]);
    }

    @Test
    void testImplicitlyTypedConverterIsAdaptedCorrectly() {
        this.testConverterTypeIsAdaptedCorrectly(
                registry -> registry.addConverter(new SimpleConverter()),
                "1", Integer.class,
                ConverterType.CONVERTER
        );
    }

    @Test
    void testLambdaConverterWithoutExplicitTypesIsRejected() {
        ConverterCache converterCache = new GenericConverters();
        ConverterCache defaultValueProviderCache = new GenericConverters();
        ConverterRegistry registry = new StandardConversionService(this.introspector(), converterCache, defaultValueProviderCache);
        // Not allowed because the source and target types cannot practically be determined due to type erasure
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.addConverter((Converter<String, Integer>) source -> Integer.parseInt(source)));
    }

    @Test
    void testLambdaConverterWithExplicitTypesIsAdaptedCorrectly() {
        this.testConverterTypeIsAdaptedCorrectly(
                registry -> registry.addConverter(String.class, Integer.class, source -> Integer.parseInt(source)),
                "1", Integer.class,
                ConverterType.CONVERTER
        );
    }

    @Test
    void testExplicitlyTypedConverterIsAdapterCorrectly() {
        this.testConverterTypeIsAdaptedCorrectly(
                registry -> registry.addConverter(String.class, Integer.class, new SimpleConverter()),
                "1", Integer.class,
                ConverterType.CONVERTER
        );
    }

    @Test
    void testImplicitDefaultValueProviderIsAdaptedCorrectly() {
        this.testConverterTypeIsAdaptedCorrectly(
                registry -> registry.addDefaultValueProvider(new DefaultValueProvider<String>() {
                    @Override
                    public @Nullable String defaultValue() {
                        return "";
                    }
                }),
                NullAccess.getInstance(), String.class,
                ConverterType.DEFAULT_VALUE_PROVIDER
        );
    }

    @Test
    void testExplicitDefaultValueProviderIsAdaptedCorrectly() {
        this.testConverterTypeIsAdaptedCorrectly(
                registry -> registry.addDefaultValueProvider(String.class, () -> ""),
                NullAccess.getInstance(), String.class,
                ConverterType.DEFAULT_VALUE_PROVIDER
        );
    }

    private void testConverterTypeIsAdaptedCorrectly(Consumer<ConverterRegistry> registerAction, Object source, Class<?> targetType, ConverterType converterType) {
        ConverterCache converterCache = new GenericConverters();
        ConverterCache defaultValueProviderCache = new GenericConverters();
        ConverterRegistry registry = new StandardConversionService(this.introspector(), converterCache, defaultValueProviderCache);
        Assertions.assertTrue(converterCache.converters().isEmpty());
        Assertions.assertTrue(defaultValueProviderCache.converters().isEmpty());

        registerAction.accept(registry);

        ConverterCache shouldBeEmpty = converterType == ConverterType.CONVERTER ? defaultValueProviderCache : converterCache;
        ConverterCache shouldBePopulated = converterType == ConverterType.CONVERTER ? converterCache : defaultValueProviderCache;

        Assertions.assertTrue(shouldBeEmpty.converters().isEmpty());
        Assertions.assertFalse(shouldBePopulated.converters().isEmpty());
        Assertions.assertEquals(1, shouldBePopulated.converters().size());

        GenericConverter converter = shouldBePopulated.getConverter(source, targetType);
        Assertions.assertNotNull(converter);
    }

    private enum ConverterType {
        CONVERTER,
        DEFAULT_VALUE_PROVIDER,
    }

    private static class SimpleConverter implements Converter<String, Integer> {
        @Override
        public Integer convert(String source) {
            return Integer.parseInt(source);
        }
    }
}
