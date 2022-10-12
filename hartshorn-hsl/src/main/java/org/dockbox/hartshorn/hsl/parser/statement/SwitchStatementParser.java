package org.dockbox.hartshorn.hsl.parser.statement;

import org.dockbox.hartshorn.hsl.ScriptEvaluationError;
import org.dockbox.hartshorn.hsl.ast.expression.Expression;
import org.dockbox.hartshorn.hsl.ast.expression.LiteralExpression;
import org.dockbox.hartshorn.hsl.ast.statement.Statement;
import org.dockbox.hartshorn.hsl.ast.statement.SwitchCase;
import org.dockbox.hartshorn.hsl.ast.statement.SwitchStatement;
import org.dockbox.hartshorn.hsl.parser.ASTNodeParser;
import org.dockbox.hartshorn.hsl.parser.TokenParser;
import org.dockbox.hartshorn.hsl.parser.TokenStepValidator;
import org.dockbox.hartshorn.hsl.runtime.Phase;
import org.dockbox.hartshorn.hsl.token.Token;
import org.dockbox.hartshorn.hsl.token.TokenType;
import org.dockbox.hartshorn.inject.binding.Bound;
import org.dockbox.hartshorn.util.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

public class SwitchStatementParser implements ASTNodeParser<SwitchStatement> {

    private final CaseBodyStatementParser caseBodyStatementParser;

    @Inject
    @Bound
    public SwitchStatementParser(final CaseBodyStatementParser caseBodyStatementParser) {
        this.caseBodyStatementParser = caseBodyStatementParser;
    }

    @Override
    public Result<SwitchStatement> parse(final TokenParser parser, final TokenStepValidator validator) {
        if (parser.match(TokenType.SWITCH)) {
            final Token switchToken = parser.previous();
            validator.expectAfter(TokenType.LEFT_PAREN, "switch");
            final Expression expr = parser.expression();
            validator.expectAfter(TokenType.RIGHT_PAREN, "expression");

            validator.expectAfter(TokenType.LEFT_BRACE, "switch");

            SwitchCase defaultBody = null;
            final List<SwitchCase> cases = new ArrayList<>();
            final Set<Object> matchedLiterals = new HashSet<>();

            while (parser.match(TokenType.CASE, TokenType.DEFAULT)) {
                final Token caseToken = parser.previous();

                if (caseToken.type() == TokenType.CASE) {
                    final Expression caseExpr = null; // TODO: Reimplement once #primary() is resolved
                    if (!(caseExpr instanceof final LiteralExpression literal)) {
                        throw new ScriptEvaluationError("Case expression must be a literal.", Phase.PARSING, caseToken);
                    }

                    if (matchedLiterals.contains(literal.value())) {
                        throw new ScriptEvaluationError("Duplicate case expression '" + literal.value() + "'.", Phase.PARSING, caseToken);
                    }
                    matchedLiterals.add(literal.value());

                    final Result<Statement> body = this.caseBodyStatementParser.parse(parser, validator);
                    if (body.caught()) {
                        return Result.of(body.error());
                    }
                    cases.add(new SwitchCase(caseToken, body.get(), literal, false));
                }
                else {
                    final Result<Statement> body = this.caseBodyStatementParser.parse(parser, validator);
                    if (body.caught()) {
                        return Result.of(body.error());
                    }
                    defaultBody = new SwitchCase(caseToken, body.get(), null, true);
                }
            }

            validator.expectAfter(TokenType.RIGHT_BRACE, "switch");

            return Result.of(new SwitchStatement(switchToken, expr, cases, defaultBody));
        }
        return Result.empty();
    }

    @Override
    public Set<Class<? extends SwitchStatement>> types() {
        return Set.of(SwitchStatement.class);
    }
}
