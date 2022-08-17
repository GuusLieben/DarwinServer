/*
 * Copyright 2019-2022 the original author or authors.
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

package org.dockbox.hartshorn.hsl.ast.statement;

import org.dockbox.hartshorn.hsl.token.Token;
import org.dockbox.hartshorn.hsl.token.TokenType;
import org.dockbox.hartshorn.hsl.visitors.StatementVisitor;

import java.util.List;

public class ConstructorStatement extends ParametricExecutableStatement {

    private final Token keyword;
    private final Token className;

    public ConstructorStatement(final Token keyword,
                                final Token className,
                                final List<Parameter> params,
                                final BlockStatement body) {
        super(keyword, params, body);
        this.keyword = keyword;
        this.className = className;
    }

    public Token keyword() {
        return this.keyword;
    }

    public Token className() {
        return className;
    }

    public Token initializerIdentifier() {
        return new Token(TokenType.CONSTRUCTOR, "<<init::%s>>".formatted(this.className().lexeme()), this.keyword().line(), this.keyword().column());
    }

    @Override
    public <R> R accept(final StatementVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
