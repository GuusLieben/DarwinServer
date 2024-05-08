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

package org.dockbox.hartshorn.hsl.parser.statement;

import java.util.Set;

import org.dockbox.hartshorn.hsl.ast.expression.Expression;
import org.dockbox.hartshorn.hsl.ast.statement.BlockStatement;
import org.dockbox.hartshorn.hsl.ast.statement.RepeatStatement;
import org.dockbox.hartshorn.hsl.parser.TokenParser;
import org.dockbox.hartshorn.hsl.parser.TokenStepValidator;
import org.dockbox.hartshorn.hsl.token.type.TokenTypePair;
import org.dockbox.hartshorn.hsl.token.type.LoopTokenType;
import org.dockbox.hartshorn.util.option.Option;

/**
 * TODO: #1061 Add documentation
 *
 * @since 0.4.13
 *
 * @author Guus Lieben
 */
public class RepeatStatementParser extends AbstractBodyStatementParser<RepeatStatement> {

    @Override
    public Option<? extends RepeatStatement> parse(TokenParser parser, TokenStepValidator validator) {
        if (parser.match(LoopTokenType.REPEAT)) {
            TokenTypePair parameters = parser.tokenRegistry().tokenPairs().parameters();
            validator.expectAfter(parameters.open(), "repeat");
            Expression value = parser.expression();
            validator.expectAfter(parameters.close(), "repeat value");
            BlockStatement loopBody = this.blockStatement("repeat", value, parser, validator);
            return Option.of(new RepeatStatement(value, loopBody));
        }
        return Option.empty();
    }

    @Override
    public Set<Class<? extends RepeatStatement>> types() {
        return Set.of(RepeatStatement.class);
    }
}
