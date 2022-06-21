package org.dockbox.hartshorn.hsl.ast;

import org.dockbox.hartshorn.hsl.visitors.StatementVisitor;

import java.util.List;

public class BlockStatement extends Statement {

    private final List<Statement> statementList;

    public BlockStatement(final List<Statement> statementList) {
        this.statementList = statementList;
    }

    public List<Statement> getStatementList() {
        return this.statementList;
    }

    @Override
    public <R> R accept(final StatementVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
