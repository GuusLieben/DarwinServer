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

package test.org.dockbox.hartshorn.hsl;

import java.util.stream.Stream;

import org.dockbox.hartshorn.inject.condition.ConditionContext;
import org.dockbox.hartshorn.inject.condition.ConditionResult;
import org.dockbox.hartshorn.launchpad.ApplicationContext;
import org.dockbox.hartshorn.context.ContextView;
import org.dockbox.hartshorn.hsl.UseExpressionValidation;
import org.dockbox.hartshorn.hsl.condition.ExpressionCondition;
import org.dockbox.hartshorn.hsl.condition.ExpressionConditionContext;
import org.dockbox.hartshorn.hsl.condition.RequiresExpression;
import org.dockbox.hartshorn.test.junit.HartshornIntegrationTest;
import org.dockbox.hartshorn.util.introspect.ElementAnnotationsIntrospector;
import org.dockbox.hartshorn.util.introspect.view.AnnotatedElementView;
import org.dockbox.hartshorn.util.option.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import org.dockbox.hartshorn.inject.annotations.Inject;

@HartshornIntegrationTest(includeBasePackages = false)
@UseExpressionValidation
public class ExpressionConditionTests {

    @Inject
    private ApplicationContext applicationContext;

    public static Stream<Arguments> scripts() {
        return Stream.of(
                Arguments.of("1==1", true), // Yield true
                Arguments.of("1==2", false), // Yield false
                Arguments.of("1==a", false), // Undefined variable, runtime error
                Arguments.of("1@=1", false), // Tokenizing error
                Arguments.of("1++=1", false) // Parsing error
        );
    }

    @ParameterizedTest
    @MethodSource("scripts")
    void testBulkValidations(String expression, boolean matches) {
        ConditionResult result = this.match(expression);
        Assertions.assertEquals(matches, result.matches());
    }

    @Test
    void testApplicationContextIsOptInAvailable() {
        ExpressionConditionContext context = new ExpressionConditionContext(this.applicationContext).includeApplicationContext(true);
        String expression = "applicationContext.getClass().getName() == \"%s\"".formatted(this.applicationContext.getClass().getName());
        ConditionResult result = this.match(expression, context);
        Assertions.assertTrue(result.matches());
    }

    @Test
    void testApplicationContextIsAvailableDefault() {
        String expression = "null != applicationContext";
        ConditionResult result = this.match(expression);
        Assertions.assertTrue(result.matches());
    }

    ConditionResult match(String expression, ContextView... contexts) {
        ExpressionCondition condition = this.applicationContext.get(ExpressionCondition.class);
        AnnotatedElementView element = this.createAnnotatedElement(expression);
        ConditionContext context = new ConditionContext(this.applicationContext, element, null);
        for (ContextView child : contexts) {
            context.addContext(child);
        }
        return condition.matches(context);
    }

    private AnnotatedElementView createAnnotatedElement(String expression) {
        RequiresExpression condition = Mockito.mock(RequiresExpression.class);
        Mockito.when(condition.value()).thenReturn(expression);
        Mockito.doReturn(RequiresExpression.class).when(condition).annotationType();

        ElementAnnotationsIntrospector annotationsIntrospector = Mockito.mock(ElementAnnotationsIntrospector.class);
        Mockito.when(annotationsIntrospector.get(RequiresExpression.class)).thenReturn(Option.of(condition));

        AnnotatedElementView elementView = Mockito.mock(AnnotatedElementView.class);
        Mockito.when(elementView.annotations()).thenReturn(annotationsIntrospector);

        return elementView;
    }
}
