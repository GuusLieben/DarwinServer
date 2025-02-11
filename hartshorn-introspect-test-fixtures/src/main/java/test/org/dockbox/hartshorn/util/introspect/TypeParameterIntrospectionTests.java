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

import org.dockbox.hartshorn.util.CollectionUtilities;
import org.dockbox.hartshorn.util.GenericType;
import org.dockbox.hartshorn.util.collections.BiMultiMap;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.TypeParameterList;
import org.dockbox.hartshorn.util.introspect.TypeParametersIntrospector;
import org.dockbox.hartshorn.util.introspect.view.TypeParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tests for introspecting type parameters.
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
@SuppressWarnings({"rawtypes", "InterfaceMayBeAnnotatedFunctional"})
public abstract class TypeParameterIntrospectionTests {

    protected abstract Introspector introspector();

    private interface TypeWithoutTypeBounds<T> {}
    private interface TypeWithSingleTypeBound<T extends Number> {}
    private interface TypeWithMultipleTypeBounds<T extends Number & Function<?,?> & Predicate<?>> {}

    @Test
    public void testVariableGenericTypeWithSingleUpperbound() {
        TypeView<TypeWithSingleTypeBound> typeView = this.introspector().introspect(TypeWithSingleTypeBound.class);
        Assertions.assertSame(TypeWithSingleTypeBound.class, typeView.type());

        this.testVariableWithExpectedBounds(typeView, Number.class);
    }

    @Test
    public void testVariableGenericTypeWithMultipleUpperbound() {
        TypeView<TypeWithMultipleTypeBounds> typeView = this.introspector().introspect(TypeWithMultipleTypeBounds.class);
        Assertions.assertSame(TypeWithMultipleTypeBounds.class, typeView.type());

        this.testVariableWithExpectedBounds(typeView, Number.class, Function.class, Predicate.class);
    }

    @Test
    void testVariableGenericTypeWithoutExplicitUpperbounds() {
        TypeView<TypeWithoutTypeBounds> typeView = this.introspector().introspect(TypeWithoutTypeBounds.class);
        Assertions.assertSame(TypeWithoutTypeBounds.class, typeView.type());

        this.testVariableWithExpectedBounds(typeView, Object.class);
    }

    private void testVariableWithExpectedBounds(TypeView<?> type, Class<?>... expectedBounds) {
        List<TypeParameterView> inputTypes = type.typeParameters().allInput().asList();
        Assertions.assertEquals(1, inputTypes.size());

        TypeParameterView parameterView = inputTypes.get(0);
        Assertions.assertTrue(parameterView.isBounded());
        Assertions.assertTrue(parameterView.isVariable());

        Set<TypeView<?>> upperBounds = parameterView.upperBounds();
        Assertions.assertEquals(expectedBounds.length, upperBounds.size());
        for (Class<?> expectedBound : expectedBounds) {
            Assertions.assertTrue(upperBounds.stream().anyMatch(typeView -> typeView.type().equals(expectedBound)));
        }
    }

    private interface NumberPredicate<U extends Number> extends Predicate<U> {}
    private interface IntegerPredicate extends NumberPredicate<Integer> {}

    @Test
    void testOutputParameterWithConcreteValue() {
        TypeView<IntegerPredicate> typeView = this.introspector().introspect(IntegerPredicate.class);
        Assertions.assertSame(IntegerPredicate.class, typeView.type());

        List<TypeParameterView> outputTypes = typeView.typeParameters().allOutput().asList();
        Assertions.assertEquals(1, outputTypes.size());

        TypeParameterView parameterView = outputTypes.get(0);
        Assertions.assertFalse(parameterView.isVariable());

        TypeView<?> resolvedType = parameterView.resolvedType().get();
        Assertions.assertSame(Integer.class, resolvedType.type());
    }

    @Test
    void testOutputParameterWithVariableValue() {
        TypeView<NumberPredicate> typeView = this.introspector().introspect(NumberPredicate.class);
        Assertions.assertSame(NumberPredicate.class, typeView.type());

        List<TypeParameterView> outputTypes = typeView.typeParameters().allOutput().asList();
        Assertions.assertEquals(1, outputTypes.size());

        TypeParameterView parameterView = outputTypes.get(0);
        Assertions.assertTrue(parameterView.isVariable());
        Assertions.assertTrue(parameterView.resolvedType().absent());
        Assertions.assertTrue(parameterView.isBounded());

        Set<TypeView<?>> upperBounds = parameterView.upperBounds();
        Assertions.assertEquals(1, upperBounds.size());
        Assertions.assertSame(Number.class, CollectionUtilities.first(upperBounds).type());
    }

    @Test
    void testInputRepresentingOutputVariable() {
        TypeView<NumberPredicate> typeView = this.introspector().introspect(NumberPredicate.class);
        Assertions.assertSame(NumberPredicate.class, typeView.type());

        List<TypeParameterView> inputTypes = typeView.typeParameters().allInput().asList();
        Assertions.assertEquals(1, inputTypes.size());

        TypeParameterView parameterView = inputTypes.get(0);
        Assertions.assertTrue(parameterView.isVariable());
        Assertions.assertTrue(parameterView.resolvedType().absent());
        Assertions.assertTrue(parameterView.isBounded());

        Set<TypeParameterView> represents = parameterView.represents();
        Assertions.assertEquals(1, represents.size());

        TypeParameterView representing = CollectionUtilities.first(represents);
        Assertions.assertTrue(representing.isVariable());
        Assertions.assertSame(Predicate.class, representing.consumedBy().type());
    }

    private interface NumberFunctionAndPredicate<U extends Number> extends Function<U, U>, Predicate<U> {}

    @Test
    void testInputRepresentingMultipleOutputVariables() {
        TypeView<NumberFunctionAndPredicate> typeView = this.introspector().introspect(NumberFunctionAndPredicate.class);
        Assertions.assertSame(NumberFunctionAndPredicate.class, typeView.type());

        List<TypeParameterView> inputTypes = typeView.typeParameters().allInput().asList();
        Assertions.assertEquals(1, inputTypes.size());

        TypeParameterView parameterView = inputTypes.get(0);
        Assertions.assertTrue(parameterView.isVariable());
        Assertions.assertTrue(parameterView.resolvedType().absent());
        Assertions.assertTrue(parameterView.isBounded());

        Set<TypeParameterView> represents = parameterView.represents();
        Assertions.assertEquals(3, represents.size());

        Map<Class<?>, List<TypeParameterView>> representationsByType = represents.stream()
                .collect(Collectors.groupingBy(typeParameterView -> typeParameterView.consumedBy().type()));
        Assertions.assertTrue(representationsByType.containsKey(Function.class));
        Assertions.assertTrue(representationsByType.containsKey(Predicate.class));

        List<TypeParameterView> functionRepresentations = representationsByType.get(Function.class);
        Assertions.assertEquals(2, functionRepresentations.size());
        Assertions.assertTrue(functionRepresentations.stream().allMatch(TypeParameterView::isVariable));

        List<TypeParameterView> predicateRepresentations = representationsByType.get(Predicate.class);
        Assertions.assertEquals(1, predicateRepresentations.size());
    }

    @Test
    void testOutputToInputReferencesCorrectDeclarations() {
        TypeView<NumberPredicate> typeView = this.introspector().introspect(NumberPredicate.class);
        Assertions.assertSame(NumberPredicate.class, typeView.type());

        List<TypeParameterView> outputParameters = typeView.typeParameters().allOutput().asList();
        Assertions.assertEquals(1, outputParameters.size());

        TypeParameterView outputParameter = outputParameters.get(0);
        Assertions.assertTrue(outputParameter.isVariable());
        Assertions.assertSame(NumberPredicate.class, outputParameter.declaredBy().type());
        Assertions.assertEquals("U", outputParameter.name());

        TypeParameterView inputParameter = outputParameter.asInputParameter();
        Assertions.assertTrue(inputParameter.isVariable());
        Assertions.assertSame(Predicate.class, inputParameter.declaredBy().type());
        Assertions.assertEquals("T", inputParameter.name());
    }

    @Test
    void testInputToInputReturnsSelf() {
        TypeView<NumberPredicate> typeView = this.introspector().introspect(NumberPredicate.class);
        Assertions.assertSame(NumberPredicate.class, typeView.type());

        List<TypeParameterView> inputParameters = typeView.typeParameters().allInput().asList();
        Assertions.assertEquals(1, inputParameters.size());

        TypeParameterView inputParameter = inputParameters.get(0);
        Assertions.assertTrue(inputParameter.isVariable());
        Assertions.assertSame(NumberPredicate.class, inputParameter.declaredBy().type());
        Assertions.assertEquals("U", inputParameter.name());

        TypeParameterView asInputParameter = inputParameter.asInputParameter();
        Assertions.assertSame(inputParameter, asInputParameter);
    }

    private interface XYtoYXFunction<X, Y> extends Function<Y, X> {}

    @Test
    void testInputToOutputCorrectlyUpdatesIndex() {
        TypeView<XYtoYXFunction> typeView = this.introspector().introspect(XYtoYXFunction.class);
        Assertions.assertSame(XYtoYXFunction.class, typeView.type());

        List<TypeParameterView> inputParameters = typeView.typeParameters().allInput().asList();
        Assertions.assertEquals(2, inputParameters.size());

        assertParameterAtIndexReferencesParameterAtIndex(typeView, 0, 1);
        assertParameterAtIndexReferencesParameterAtIndex(typeView, 1, 0);
    }

    private static void assertParameterAtIndexReferencesParameterAtIndex(TypeView<?> typeView, int inputIndex, int outputIndex) {
        Option<TypeParameterView> parameter = typeView.typeParameters().atIndex(inputIndex);
        Assertions.assertTrue(parameter.present());
        TypeParameterView parameterView = parameter.get();
        Assertions.assertEquals(inputIndex, parameterView.index());

        Set<TypeParameterView> parameterRepresents = parameterView.represents();
        Assertions.assertEquals(1, parameterRepresents.size());

        TypeParameterView parameterRepresentsView = CollectionUtilities.first(parameterRepresents);
        Assertions.assertEquals(outputIndex, parameterRepresentsView.index());
    }

    @Test
    void testInputIsInput() {
        TypeView<NumberPredicate> typeView = this.introspector().introspect(NumberPredicate.class);
        Assertions.assertSame(NumberPredicate.class, typeView.type());

        List<TypeParameterView> inputParameters = typeView.typeParameters().allInput().asList();
        Assertions.assertEquals(1, inputParameters.size());

        TypeParameterView inputParameter = inputParameters.get(0);
        Assertions.assertTrue(inputParameter.isInputParameter());
        Assertions.assertFalse(inputParameter.isOutputParameter());
    }

    @Test
    void testOutputIsOutput() {
        TypeView<NumberPredicate> typeView = this.introspector().introspect(NumberPredicate.class);
        Assertions.assertSame(NumberPredicate.class, typeView.type());

        List<TypeParameterView> outputParameters = typeView.typeParameters().allOutput().asList();
        Assertions.assertEquals(1, outputParameters.size());

        TypeParameterView outputParameter = outputParameters.get(0);
        Assertions.assertTrue(outputParameter.isOutputParameter());
        Assertions.assertFalse(outputParameter.isInputParameter());
    }

    @Test
    void testResolveForSelfWithExplicitParameters() {
        TypeView<Collection<String>> typeView = this.introspector().introspect(new GenericType<>() {});
        Assertions.assertSame(Collection.class, typeView.type());

        TypeParametersIntrospector typeParameters = typeView.typeParameters();
        List<TypeParameterView> collectionParameters = typeParameters.inputFor(Collection.class).asList();
        Assertions.assertEquals(1, collectionParameters.size());

        TypeParameterView collectionParameter = collectionParameters.get(0);
        Assertions.assertFalse(collectionParameter.isVariable()); // Should not be 'E'
        Assertions.assertSame(String.class, collectionParameter.resolvedType().get().type());
    }

    @Test
    void testResolveForDirectParentWithExplicitParameters() {
        TypeView<Collection<String>> typeView = this.introspector().introspect(new GenericType<>() {});
        Assertions.assertSame(Collection.class, typeView.type());

        TypeParametersIntrospector typeParameters = typeView.typeParameters();
        List<TypeParameterView> collectionParameters = typeParameters.inputFor(Iterable.class).asList();
        Assertions.assertEquals(1, collectionParameters.size());

        TypeParameterView collectionParameter = collectionParameters.get(0);
        Assertions.assertFalse(collectionParameter.isVariable()); // Should not be 'E'
        Assertions.assertSame(String.class, collectionParameter.resolvedType().get().type());
    }

    @Test
    void testResolveForIndirectParentWithExplicitParameters() {
        TypeView<LinkedList<String>> typeView = this.introspector().introspect(new GenericType<>() {});
        Assertions.assertSame(LinkedList.class, typeView.type());

        TypeParametersIntrospector typeParameters = typeView.typeParameters();
        List<TypeParameterView> collectionParameters = typeParameters.inputFor(Iterable.class).asList();
        Assertions.assertEquals(1, collectionParameters.size());

        TypeParameterView collectionParameter = collectionParameters.get(0);
        Assertions.assertFalse(collectionParameter.isVariable()); // Should not be 'E'
        Assertions.assertSame(String.class, collectionParameter.resolvedType().get().type());
    }

    @Test
    void testOutputAsMapResolvesAllToParent() {
        TypeView<NumberFunctionAndPredicate> typeView = this.introspector().introspect(NumberFunctionAndPredicate.class);
        Assertions.assertSame(NumberFunctionAndPredicate.class, typeView.type());

        TypeParametersIntrospector typeParameters = typeView.typeParameters();
        TypeParameterList outputParameters = typeParameters.allOutput();
        BiMultiMap<TypeParameterView, TypeParameterView> multiMap = outputParameters.asMap();
        // Should be 1:1 mappings, so the size should be the same
        Assertions.assertEquals(outputParameters.count(), multiMap.size());

        Set<TypeParameterView> keys = multiMap.keySet();
        Assertions.assertEquals(outputParameters.count(), keys.size());

        Collection<TypeParameterView> values = multiMap.allValues();
        // Output parameters should only map to the input parameters of their target types
        Assertions.assertEquals(outputParameters.count(), values.size());
    }

    @Test
    void testInputAsMapResolvesAllToOutput() {
        TypeView<NumberFunctionAndPredicate> typeView = this.introspector().introspect(NumberFunctionAndPredicate.class);
        Assertions.assertSame(NumberFunctionAndPredicate.class, typeView.type());

        TypeParametersIntrospector typeParameters = typeView.typeParameters();
        TypeParameterList inputParameters = typeParameters.allInput();

        BiMultiMap<TypeParameterView, TypeParameterView> multiMap = inputParameters.asMap();
        Set<TypeParameterView> keys = multiMap.keySet();
        // Note: unlike output parameters, I -> O mappings are not 1:1, but are 1:n (where n is the number of
        // output parameters), so the size of the map will be different, but the keys should be the same
        Assertions.assertEquals(inputParameters.count(), keys.size());

        Assertions.assertEquals(1, keys.size());
        Collection<TypeParameterView> values = multiMap.get(CollectionUtilities.first(keys));
        TypeParameterList outputParameters = typeParameters.allOutput();
        Assertions.assertEquals(outputParameters.count(), values.size());

        List<TypeParameterView> outputParametersList = outputParameters.asList();
        Assertions.assertTrue(values.containsAll(outputParametersList));
    }
}
