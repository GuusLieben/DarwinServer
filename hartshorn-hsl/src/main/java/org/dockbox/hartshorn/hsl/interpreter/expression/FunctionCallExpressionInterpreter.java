/*
 * Copyright 2019-2023 the original author or authors.
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

package org.dockbox.hartshorn.hsl.interpreter.expression;

import org.dockbox.hartshorn.hsl.ast.expression.Expression;
import org.dockbox.hartshorn.hsl.ast.expression.FunctionCallExpression;
import org.dockbox.hartshorn.hsl.interpreter.InterpreterAdapter;
import org.dockbox.hartshorn.hsl.interpreter.ASTNodeInterpreter;
import org.dockbox.hartshorn.hsl.objects.BindableNode;
import org.dockbox.hartshorn.hsl.objects.CallableNode;
import org.dockbox.hartshorn.hsl.objects.ExternalObjectReference;
import org.dockbox.hartshorn.hsl.objects.InstanceReference;
import org.dockbox.hartshorn.hsl.runtime.RuntimeError;
import org.dockbox.hartshorn.util.ApplicationException;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallExpressionInterpreter implements ASTNodeInterpreter<Object, FunctionCallExpression> {

    @Override
    public Object interpret(FunctionCallExpression node, InterpreterAdapter adapter) {
        Object callee = adapter.evaluate(node.callee());

        List<Object> arguments = new ArrayList<>();
        for (Expression argument : node.arguments()) {
            Object evaluated = adapter.evaluate(argument);
            if (evaluated instanceof ExternalObjectReference external) {
                evaluated = external.externalObject();
            }
            arguments.add(evaluated);
        }

        // Can't call non-callable nodes..
        if (!(callee instanceof final CallableNode function)) {
            throw new RuntimeError(node.openParenthesis(), "Can only call functions and classes, but received " + callee + ".");
        }

        try {
            if (callee instanceof InstanceReference instance) {
                return function.call(node.openParenthesis(), adapter.interpreter(), instance, arguments);
            }
            else if (callee instanceof BindableNode<?> bindable){
                return function.call(node.openParenthesis(), adapter.interpreter(), bindable.bound(), arguments);
            }
            else {
                return function.call(node.openParenthesis(), adapter.interpreter(), null, arguments);
            }
        }
        catch (ApplicationException e) {
            throw new RuntimeError(node.openParenthesis(), e.getMessage());
        }
    }
}
