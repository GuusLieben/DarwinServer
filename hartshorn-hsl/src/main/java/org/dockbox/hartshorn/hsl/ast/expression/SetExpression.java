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

package org.dockbox.hartshorn.hsl.ast.expression;

import org.dockbox.hartshorn.hsl.ast.NamedNode;
import org.dockbox.hartshorn.hsl.token.Token;
import org.dockbox.hartshorn.hsl.visitors.ExpressionVisitor;

/**
 * TODO: #1061 Add documentation
 *
 * @since 0.4.12
 *
 * @author Guus Lieben
 */
public class SetExpression extends Expression implements NamedNode {

    private final Expression object;
    private final Token name;
    private final Expression value;

    public SetExpression(Expression object, Token name, Expression value) {
        super(name);
        this.object = object;
        this.name = name;
        this.value = value;
    }

    public Expression object() {
        return this.object;
    }

    @Override
    public Token name() {
        return this.name;
    }

    public Expression value() {
        return this.value;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
