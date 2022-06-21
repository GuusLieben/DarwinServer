package org.dockbox.hartshorn.hsl.ast;

import org.dockbox.hartshorn.hsl.visitors.StatementVisitor;

public class PrintStatement extends Statement {

    private final Expression expression;

    public PrintStatement(final Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return this.expression;
    }

    @Override
    public <R> R accept(final StatementVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
