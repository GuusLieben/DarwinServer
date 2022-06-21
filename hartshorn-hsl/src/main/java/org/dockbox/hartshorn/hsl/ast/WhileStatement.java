package org.dockbox.hartshorn.hsl.ast;

import org.dockbox.hartshorn.hsl.visitors.StatementVisitor;

public class WhileStatement extends Statement {

    private final Expression condition;
    private final Statement loopBody;

    public WhileStatement(final Expression condition, final Statement loopBody) {
        this.condition = condition;
        this.loopBody = loopBody;
    }

    public Expression getCondition() {
        return this.condition;
    }

    public Statement getLoopBody() {
        return this.loopBody;
    }

    @Override
    public <R> R accept(final StatementVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
