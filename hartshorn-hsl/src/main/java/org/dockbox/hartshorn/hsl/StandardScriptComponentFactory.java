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

package org.dockbox.hartshorn.hsl;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.component.Service;
import org.dockbox.hartshorn.component.condition.RequiresActivator;
import org.dockbox.hartshorn.hsl.interpreter.Interpreter;
import org.dockbox.hartshorn.hsl.interpreter.ResultCollector;
import org.dockbox.hartshorn.hsl.lexer.Lexer;
import org.dockbox.hartshorn.hsl.modules.NativeModule;
import org.dockbox.hartshorn.hsl.parser.StandardTokenParser;
import org.dockbox.hartshorn.hsl.parser.TokenParser;
import org.dockbox.hartshorn.hsl.runtime.ExecutionOptions;
import org.dockbox.hartshorn.hsl.semantic.Resolver;
import org.dockbox.hartshorn.hsl.token.Token;

import java.util.List;
import java.util.Map;

@Service
@RequiresActivator(UseExpressionValidation.class)
public class StandardScriptComponentFactory implements ScriptComponentFactory {

    @Override
    public Lexer lexer(String source) {
        return new Lexer(source);
    }

    @Override
    public TokenParser parser(List<Token> tokens) {
        return new StandardTokenParser(tokens);
    }

    @Override
    public Resolver resolver(Interpreter interpreter) {
        return new Resolver(interpreter);
    }

    @Override
    public Interpreter interpreter(ResultCollector resultCollector, Map<String, NativeModule> modules, ApplicationContext applicationContext) {
        return this.interpreter(resultCollector, modules, new ExecutionOptions(), applicationContext);
    }

    @Override
    public Interpreter interpreter(ResultCollector resultCollector, Map<String, NativeModule> modules, ExecutionOptions options, ApplicationContext applicationContext) {
        return new Interpreter(resultCollector, modules, options, applicationContext);
    }
}
