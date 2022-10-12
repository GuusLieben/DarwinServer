package org.dockbox.hartshorn.hsl.parser.statement;

import org.dockbox.hartshorn.hsl.ScriptEvaluationError;
import org.dockbox.hartshorn.hsl.ast.statement.BlockStatement;
import org.dockbox.hartshorn.hsl.ast.statement.Statement;
import org.dockbox.hartshorn.hsl.parser.ASTNodeParser;
import org.dockbox.hartshorn.hsl.parser.TokenParser;
import org.dockbox.hartshorn.hsl.parser.TokenStepValidator;
import org.dockbox.hartshorn.hsl.runtime.Phase;
import org.dockbox.hartshorn.hsl.token.Token;
import org.dockbox.hartshorn.hsl.token.TokenType;
import org.dockbox.hartshorn.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CaseBodyStatementParser implements ASTNodeParser<Statement> {

    @Override
    public Result<Statement> parse(final TokenParser parser, final TokenStepValidator validator) {
        if (parser.match(TokenType.COLON)) {
            final Token colon = parser.previous();
            final List<Statement> statements = new ArrayList<>();
            while (!parser.check(TokenType.CASE, TokenType.DEFAULT, TokenType.RIGHT_BRACE)) {
                statements.add(parser.statement());
            }
            return Result.of(new BlockStatement(colon, statements));
        }
        else if (parser.match(TokenType.ARROW)) {
            return Result.of(parser.expressionStatement());
        }
        else {
            throw new ScriptEvaluationError("Expected ':' or '->'", Phase.PARSING, parser.peek());
        }
    }

    @Override
    public Set<Class<? extends Statement>> types() {
        // Only for direct used, should not be used for dynamic parsing
        return Set.of();
    }
}
