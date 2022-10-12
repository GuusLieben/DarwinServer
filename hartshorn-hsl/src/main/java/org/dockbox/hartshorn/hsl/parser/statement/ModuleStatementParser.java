package org.dockbox.hartshorn.hsl.parser.statement;

import org.dockbox.hartshorn.hsl.ast.statement.ModuleStatement;
import org.dockbox.hartshorn.hsl.parser.ASTNodeParser;
import org.dockbox.hartshorn.hsl.parser.TokenParser;
import org.dockbox.hartshorn.hsl.parser.TokenStepValidator;
import org.dockbox.hartshorn.hsl.token.Token;
import org.dockbox.hartshorn.hsl.token.TokenType;
import org.dockbox.hartshorn.util.Result;

import java.util.Set;

public class ModuleStatementParser implements ASTNodeParser<ModuleStatement> {

    @Override
    public Result<ModuleStatement> parse(final TokenParser parser, final TokenStepValidator validator) {
        if (parser.match(TokenType.USING)) {
            final Token name = validator.expect(TokenType.IDENTIFIER, "module name");
            validator.expectAfter(TokenType.SEMICOLON, TokenType.USING);
            return Result.of(new ModuleStatement(name));
        }
        return Result.empty();
    }

    @Override
    public Set<Class<? extends ModuleStatement>> types() {
        return Set.of(ModuleStatement.class);
    }
}
