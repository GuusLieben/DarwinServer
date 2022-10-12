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

package org.dockbox.hartshorn.hsl.parser;

import org.dockbox.hartshorn.hsl.ScriptEvaluationError;
import org.dockbox.hartshorn.hsl.ast.expression.ArrayComprehensionExpression;
import org.dockbox.hartshorn.hsl.ast.expression.ArrayGetExpression;
import org.dockbox.hartshorn.hsl.ast.expression.ArrayLiteralExpression;
import org.dockbox.hartshorn.hsl.ast.expression.ArraySetExpression;
import org.dockbox.hartshorn.hsl.ast.expression.AssignExpression;
import org.dockbox.hartshorn.hsl.ast.expression.BinaryExpression;
import org.dockbox.hartshorn.hsl.ast.expression.BitwiseExpression;
import org.dockbox.hartshorn.hsl.ast.expression.ElvisExpression;
import org.dockbox.hartshorn.hsl.ast.expression.Expression;
import org.dockbox.hartshorn.hsl.ast.expression.FunctionCallExpression;
import org.dockbox.hartshorn.hsl.ast.expression.GetExpression;
import org.dockbox.hartshorn.hsl.ast.expression.GroupingExpression;
import org.dockbox.hartshorn.hsl.ast.expression.InfixExpression;
import org.dockbox.hartshorn.hsl.ast.expression.LiteralExpression;
import org.dockbox.hartshorn.hsl.ast.expression.LogicalAssignExpression;
import org.dockbox.hartshorn.hsl.ast.expression.LogicalExpression;
import org.dockbox.hartshorn.hsl.ast.expression.PostfixExpression;
import org.dockbox.hartshorn.hsl.ast.expression.PrefixExpression;
import org.dockbox.hartshorn.hsl.ast.expression.RangeExpression;
import org.dockbox.hartshorn.hsl.ast.expression.SetExpression;
import org.dockbox.hartshorn.hsl.ast.expression.SuperExpression;
import org.dockbox.hartshorn.hsl.ast.expression.TernaryExpression;
import org.dockbox.hartshorn.hsl.ast.expression.ThisExpression;
import org.dockbox.hartshorn.hsl.ast.expression.UnaryExpression;
import org.dockbox.hartshorn.hsl.ast.expression.VariableExpression;
import org.dockbox.hartshorn.hsl.ast.statement.BlockStatement;
import org.dockbox.hartshorn.hsl.ast.statement.BreakStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ClassStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ConstructorStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ContinueStatement;
import org.dockbox.hartshorn.hsl.ast.statement.DoWhileStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ExpressionStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ExtensionStatement;
import org.dockbox.hartshorn.hsl.ast.statement.FieldStatement;
import org.dockbox.hartshorn.hsl.ast.statement.FinalizableStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ForEachStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ForStatement;
import org.dockbox.hartshorn.hsl.ast.statement.Function;
import org.dockbox.hartshorn.hsl.ast.statement.FunctionStatement;
import org.dockbox.hartshorn.hsl.ast.statement.IfStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ModuleStatement;
import org.dockbox.hartshorn.hsl.ast.statement.NativeFunctionStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ParametricExecutableStatement.Parameter;
import org.dockbox.hartshorn.hsl.ast.statement.RepeatStatement;
import org.dockbox.hartshorn.hsl.ast.statement.ReturnStatement;
import org.dockbox.hartshorn.hsl.ast.statement.Statement;
import org.dockbox.hartshorn.hsl.ast.statement.SwitchCase;
import org.dockbox.hartshorn.hsl.ast.statement.SwitchStatement;
import org.dockbox.hartshorn.hsl.ast.statement.TestStatement;
import org.dockbox.hartshorn.hsl.ast.statement.VariableStatement;
import org.dockbox.hartshorn.hsl.ast.statement.WhileStatement;
import org.dockbox.hartshorn.hsl.customizer.CodeCustomizer;
import org.dockbox.hartshorn.hsl.runtime.Phase;
import org.dockbox.hartshorn.hsl.token.Token;
import org.dockbox.hartshorn.hsl.token.TokenType;
import org.dockbox.hartshorn.inject.binding.Bound;
import org.dockbox.hartshorn.util.function.TriFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The parser takes the tokens generated by the {@link org.dockbox.hartshorn.hsl.lexer.Lexer}
 * and transforms logical combinations of {@link Token}s into {@link Statement}s. This produces
 * the first syntax tree.
 *
 * @author Guus Lieben
 * @since 22.4
 */
public class Parser {

    private int current = 0;
    private List<Token> tokens;

    private static final int MAX_NUM_OF_ARGUMENTS = 8;
    private static final TokenType[] ASSIGNMENT_TOKENS = allTokensMatching(t -> t.assignsWith() != null);

    private final Set<String> prefixFunctions = new HashSet<>();
    private final Set<String> infixFunctions = new HashSet<>();

    @Bound
    public Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    private static TokenType[] allTokensMatching(final Predicate<TokenType> predicate) {
        return Arrays.stream(TokenType.values())
                .filter(predicate)
                .toArray(TokenType[]::new);
    }

    /**
     * Gets all tokens which have been configured for this parser instance. This
     * allows {@link CodeCustomizer}s to modify the collected tokens before parsing
     * is performed.
     *
     * @return The configured tokens.
     */
    public List<Token> tokens() {
        return this.tokens;
    }

    /**
     * Sets the collection of tokens to parse. This can be used by {@link CodeCustomizer}s
     * to modify the tokens before parsing is performed.
     *
     * @param tokens The tokens to parse.
     */
    public void tokens(final List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parses the configured tokens and transforms them into logical {@link Statement}s.
     *
     * @return The parsed {@link Statement}s.
     */
    public List<Statement> parse() {
        final List<Statement> statements = new ArrayList<>();
        while (!this.isAtEnd()) {
            statements.add(this.statement());
        }
        return statements;
    }

    private Statement statement() {
        if (this.match(TokenType.PREFIX, TokenType.INFIX) && this.match(TokenType.FUN))
            return this.funcDeclaration(this.tokens.get(this.current - 2));
        if (this.match(TokenType.FUN))
            return this.funcDeclaration(this.previous());
        if (this.match(TokenType.CLASS))
            return this.classDeclaration();
        if (this.match(TokenType.NATIVE))
            return this.nativeFuncDeclaration();
        if (this.match(TokenType.FINAL))
            return this.finalDeclaration();

        final TokenType type = this.peek().type();
        if (type.standaloneStatement()) {
            throw new ScriptEvaluationError("Unsupported standalone statement type: " + type, Phase.PARSING, this.peek());
        }

        return this.expressionStatement();
    }

    private ClassStatement classDeclaration() {
        final Token name = this.expect(TokenType.IDENTIFIER, "class name");

        final boolean isDynamic = this.match(TokenType.QUESTION_MARK);

        VariableExpression superClass = null;
        if (this.match(TokenType.EXTENDS)) {
            this.expect(TokenType.IDENTIFIER, "super class name");
            superClass = new VariableExpression(this.previous());
        }

        this.expectBefore(TokenType.LEFT_BRACE, "class body");

        final List<FunctionStatement> methods = new ArrayList<>();
        final List<FieldStatement> fields = new ArrayList<>();
        ConstructorStatement constructor = null;
        while (!this.check(TokenType.RIGHT_BRACE) && !this.isAtEnd()) {
            final Statement declaration = this.classBodyStatement(name);
            if (declaration instanceof ConstructorStatement constructorStatement) {
                constructor = constructorStatement;
            } else if (declaration instanceof FunctionStatement function){
                methods.add(function);
            } else if (declaration instanceof FieldStatement field) {
                fields.add(field);
            }
            else {
                throw new ScriptEvaluationError("Unsupported class body statement type: " + declaration.getClass().getSimpleName(), Phase.PARSING, this.peek());
            }
        }

        this.expectAfter(TokenType.RIGHT_BRACE, "class body");

        return new ClassStatement(name, superClass, constructor, methods, fields, isDynamic);
    }

    private Statement classBodyStatement(final Token className) {
        if (this.match(TokenType.CONSTRUCTOR)) {
            return this.constructorDeclaration(className);
        }

        if (this.check(TokenType.FUN)) {
            return this.function(TokenType.FUN, TokenType.LEFT_BRACE, (name, parameters) -> {
                final Token start = this.peek();
                final List<Statement> statements = this.consumeStatements();
                final BlockStatement body = new BlockStatement(start, statements);
                return new FunctionStatement(name, parameters, body);
            });
        } else {
            return this.fieldDeclaration();
        }
    }

    private ConstructorStatement constructorDeclaration(final Token className) {
        final Token keyword = this.peek();
        final List<Parameter> parameters = this.functionParameters("constructor", MAX_NUM_OF_ARGUMENTS, keyword);
        final BlockStatement body = this.block();
        return new ConstructorStatement(keyword, className, parameters, body);
    }

    private Function funcDeclaration(final Token token) {
        final Token name = this.expect(TokenType.IDENTIFIER, "function name");

        int expectedNumberOrArguments = MAX_NUM_OF_ARGUMENTS;
        if (token.type() == TokenType.PREFIX) {
            this.prefixFunctions.add(name.lexeme());
            expectedNumberOrArguments = 1;
        }
        else if (token.type() == TokenType.INFIX) {
            this.infixFunctions.add(name.lexeme());
            expectedNumberOrArguments = 2;
        }

        Token extensionName = null;

        if (this.peek().type() == TokenType.COLON) {
            this.expectAfter(TokenType.COLON, "class name");
            extensionName = this.expect(TokenType.IDENTIFIER, "extension name");
        }

        final List<Parameter> parameters = this.functionParameters("function name", expectedNumberOrArguments, token);
        final BlockStatement body = this.block();

        if (extensionName != null) {
            final FunctionStatement function = new FunctionStatement(extensionName, parameters, body);
            return new ExtensionStatement(name, function);
        }
        else {
            return new FunctionStatement(name, parameters, body);
        }
    }

    private List<Parameter> functionParameters(final String function_name, final int expectedNumberOrArguments, final Token token) {
        this.expectAfter(TokenType.LEFT_PAREN, function_name);
        final List<Parameter> parameters = new ArrayList<>();
        if (!this.check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= expectedNumberOrArguments) {
                    final String message = "Cannot have more than " + expectedNumberOrArguments + " parameters" + (token == null ? "" : " for " + token.type() + " functions");
                    throw new ScriptEvaluationError(message, Phase.PARSING, this.peek());
                }
                final Token parameterName = this.expect(TokenType.IDENTIFIER, "parameter name");
                parameters.add(new Parameter(parameterName));
            }
            while (this.match(TokenType.COMMA));
        }

        this.expectAfter(TokenType.RIGHT_PAREN, "parameters");
        return parameters;
    }

    private NativeFunctionStatement nativeFuncDeclaration() {
        this.expect(TokenType.FUN);
        final Token moduleName = this.expect(TokenType.IDENTIFIER, "module name");

        while (this.match(TokenType.COLON)) {
            final Token token = new Token(TokenType.DOT, ".", moduleName.line(), moduleName.column());
            moduleName.concat(token);
            final Token moduleName2 = this.expect(TokenType.IDENTIFIER, "module name");
            moduleName.concat(moduleName2);
        }

        return this.function(TokenType.DOT, TokenType.SEMICOLON, (name, parameters) ->
                new NativeFunctionStatement(name, moduleName, null, parameters)
        );
    }

    private <T extends Statement> T function(final TokenType keyword, final TokenType end, final BiFunction<Token, List<Parameter>, T> converter) {
        this.expectBefore(keyword, "method body");
        final Token funcName = this.expect(TokenType.IDENTIFIER, "function name");
        final List<Parameter> parameters = this.functionParameters("method name", MAX_NUM_OF_ARGUMENTS, null);
        this.expectAfter(end, "value");
        return converter.apply(funcName, parameters);
    }

    private FieldStatement fieldDeclaration() {
        final Token modifier = this.find(TokenType.PRIVATE, TokenType.PUBLIC);
        final boolean isFinal = this.match(TokenType.FINAL);
        final VariableStatement variable = this.varDeclaration();
        return new FieldStatement(modifier, variable.name(), variable.initializer(), isFinal);
    }

    private Statement finalDeclaration() {
        final FinalizableStatement finalizable;

        if (this.match(TokenType.PREFIX, TokenType.INFIX) && this.match(TokenType.FUN)) finalizable = this.funcDeclaration(this.tokens.get(this.current - 2));
        else if (this.match(TokenType.FUN)) finalizable = this.funcDeclaration(this.previous());
        else if (this.match(TokenType.VAR)) finalizable = this.varDeclaration();
        else if (this.match(TokenType.CLASS)) finalizable = this.classDeclaration();
        else if (this.match(TokenType.NATIVE)) finalizable = this.nativeFuncDeclaration();
        else throw new ScriptEvaluationError("Illegal use of %s. Expected valid keyword to follow, but got %s".formatted(TokenType.FINAL.representation(), this.peek().type()), Phase.PARSING, this.peek());

        finalizable.makeFinal();
        return finalizable;
    }

    private ExpressionStatement expressionStatement() {
        final Expression expr = this.expression();
        this.expectAfter(TokenType.SEMICOLON, "expression");
        return new ExpressionStatement(expr);
    }

    private List<Statement> consumeStatements() {
        final List<Statement> statements = new ArrayList<>();
        while (!this.check(TokenType.RIGHT_BRACE) && !this.isAtEnd()) {
            statements.add(this.statement());
        }
        this.expectAfter(TokenType.RIGHT_BRACE, "block");
        return statements;
    }

    private Expression expression() {
        return this.assignment();
    }

    private Expression assignment() {
        final Expression expr = this.elvisExp();

        if (this.match(TokenType.EQUAL)) {
            final Token equals = this.previous();
            final Expression value = this.assignment();

            if (expr instanceof VariableExpression) {
                final Token name = ((VariableExpression) expr).name();
                return new AssignExpression(name, value);
            }
            else if (expr instanceof final ArrayGetExpression arrayGetExpression) {
                final Token name = arrayGetExpression.name();
                return new ArraySetExpression(name, arrayGetExpression.index(), value);
            }
            else if (expr instanceof final GetExpression get) {
                return new SetExpression(get.object(), get.name(), value);
            }
            throw new ScriptEvaluationError("Invalid assignment target.", Phase.PARSING, equals);
        }
        return expr;
    }

    private Expression elvisExp() {
        final Expression expr = this.ternaryExp();
        if (this.match(TokenType.ELVIS)) {
            final Token elvis = this.previous();
            final Expression rightExp = this.ternaryExp();
            return new ElvisExpression(expr, elvis, rightExp);
        }
        return expr;
    }

    private Expression ternaryExp() {
        final Expression expr = this.bitwise();

        if (this.match(TokenType.QUESTION_MARK)) {
            final Token question = this.previous();
            final Expression firstExp = this.logical();
            final Token colon = this.peek();
            if (this.match(TokenType.COLON)) {
                final Expression secondExp = this.logical();
                return new TernaryExpression(expr, question, firstExp, colon, secondExp);
            }
            throw new ScriptEvaluationError("Expected expression after " + TokenType.COLON.representation(), Phase.PARSING, colon);
        }
        return expr;
    }

    private Expression logicalOrBitwise(final Supplier<Expression> next, final TriFunction<Expression, Token, Expression, Expression> step, final TokenType... whileMatching) {
        Expression expression = next.get();
        while(this.match(whileMatching)) {
            final Token operator = this.previous();
            final Expression right = next.get();
            expression = step.accept(expression, operator, right);
        }
        return expression;
    }

    private Expression bitwise() {
        return this.logicalOrBitwise(this::logical, BitwiseExpression::new,
                TokenType.SHIFT_LEFT,
                TokenType.SHIFT_RIGHT,
                TokenType.LOGICAL_SHIFT_RIGHT,
                TokenType.BITWISE_OR,
                TokenType.BITWISE_AND
        );
    }

    private Expression logical() {
        return this.logicalOrBitwise(this::equality, LogicalExpression::new,
                TokenType.OR,
                TokenType.XOR,
                TokenType.AND
        );
    }

    private Expression equality() {
        return this.logicalOrBitwise(this::range, BinaryExpression::new,
                TokenType.BANG_EQUAL,
                TokenType.EQUAL_EQUAL
        );
    }

    private Expression range() {
        return this.logicalOrBitwise(this::logicalAssign, RangeExpression::new, TokenType.RANGE);
    }

    private Expression logicalAssign() {
        return this.logicalOrBitwise(this::parsePrefixFunctionCall, (left, token, right) -> {
                    if (left instanceof VariableExpression variable) {
                        return new LogicalAssignExpression(variable.name(), token, right);
                    }
                    else {
                        throw new ScriptEvaluationError("Invalid assignment target.", Phase.PARSING, token);
                    }
                }, ASSIGNMENT_TOKENS);
    }

    private boolean match(final TokenType... types) {
        return this.find(types) != null;
    }

    private Token find(final TokenType... types) {
        for (final TokenType type : types) {
            if (this.check(type)) {
                final Token token = this.peek();
                this.advance();
                return token;
            }
        }
        return null;
    }

    private boolean check(final TokenType... types) {
        if (this.isAtEnd()) return false;
        for (final TokenType type : types) {
            if (this.peek().type() == type) return true;
        }
        return false;
    }

    private Token advance() {
        if (!this.isAtEnd()) this.current++;
        return this.previous();
    }

    private boolean isAtEnd() {
        return this.peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return this.tokens.get(this.current);
    }

    private Token previous() {
        return this.tokens.get(this.current - 1);
    }

    private Expression parsePrefixFunctionCall() {
        if (this.check(TokenType.IDENTIFIER) && this.prefixFunctions.contains(this.tokens.get(this.current).lexeme())) {
            this.current++;
            final Token prefixFunctionName = this.previous();
            final Expression right = this.comparison();
            return new PrefixExpression(prefixFunctionName, right);
        }
        return this.comparison();
    }

    private Expression comparison() {
        Expression expr = this.addition();

        while (this.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            final Token operator = this.previous();
            final Expression right = this.addition();
            expr = new BinaryExpression(expr, operator, right);
        }

        return expr;
    }

    private Expression addition() {
        Expression expr = this.multiplication();
        while (this.match(TokenType.MINUS, TokenType.PLUS)) {
            final Token operator = this.previous();
            final Expression right = this.multiplication();
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    private Expression multiplication() {
        Expression expr = this.parseInfixExpressions();

        while (this.match(TokenType.SLASH, TokenType.STAR, TokenType.MODULO)) {
            final Token operator = this.previous();
            final Expression right = this.parseInfixExpressions();
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    private Expression parseInfixExpressions() {
        Expression expr = this.unary();

        while (this.check(TokenType.IDENTIFIER) && this.infixFunctions.contains(this.tokens.get(this.current).lexeme())) {
            this.current++;
            final Token operator = this.previous();
            final Expression right = this.unary();
            expr = new InfixExpression(expr, operator, right);
        }
        return expr;
    }

    private Expression unary() {
        if (this.match(TokenType.BANG, TokenType.MINUS, TokenType.PLUS_PLUS, TokenType.MINUS_MINUS, TokenType.COMPLEMENT)) {
            final Token operator = this.previous();
            final Expression right = this.unary();
            return new UnaryExpression(operator, right);
        }
        return this.call();
    }

    private Expression call() {
        Expression expr = this.primary();
        while (true) {
            if (this.match(TokenType.LEFT_PAREN)) {
                expr = this.finishCall(expr);
            }
            else if (this.match(TokenType.DOT)) {
                final Token name = this.consume(TokenType.IDENTIFIER, "Expected property name after '.'.");
                expr = new GetExpression(name, expr);
            }
            else if (this.match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
                final Token operator = this.previous();
                expr = new PostfixExpression(operator, expr);
            }
            else {
                break;
            }
        }
        return expr;
    }

    private Expression finishCall(final Expression callee) {
        final List<Expression> arguments = new ArrayList<>();
        final Token parenOpen = this.previous();
        // For zero arguments
        if (!this.check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= MAX_NUM_OF_ARGUMENTS) {
                    throw new ScriptEvaluationError("Cannot have more than " + MAX_NUM_OF_ARGUMENTS + " arguments.", Phase.PARSING, this.peek());
                }
                arguments.add(this.expression());
            }
            while (this.match(TokenType.COMMA));
        }
        final Token parenClose = this.expectAfter(TokenType.RIGHT_PAREN, "arguments");
        return new FunctionCallExpression(callee, parenOpen, parenClose, arguments);
    }

    private Expression primary() {
        if (this.match(TokenType.FALSE)) return new LiteralExpression(this.peek(), false);
        if (this.match(TokenType.TRUE)) return new LiteralExpression(this.peek(), true);
        if (this.match(TokenType.NULL)) return new LiteralExpression(this.peek(), null);
        if (this.match(TokenType.THIS)) return new ThisExpression(this.previous());
        if (this.match(TokenType.NUMBER, TokenType.STRING, TokenType.CHAR)) return new LiteralExpression(this.peek(), this.previous().literal());
        if (this.match(TokenType.IDENTIFIER)) return this.identifierExpression();
        if (this.match(TokenType.LEFT_PAREN)) return this.groupingExpression();
        if (this.match(TokenType.SUPER)) return this.superExpression();
        if (this.match(TokenType.ARRAY_OPEN)) return this.complexArray();

        throw new ScriptEvaluationError("Expected expression, but found " + this.tokens.get(this.current), Phase.PARSING, this.peek());
    }

    private SuperExpression superExpression() {
        final Token keyword = this.previous();
        this.expectAfter(TokenType.DOT, TokenType.SUPER);
        final Token method = this.expect(TokenType.IDENTIFIER, "super class method name");
        return new SuperExpression(keyword, method);
    }

    private GroupingExpression groupingExpression() {
        final Expression expr = this.expression();
        this.expectAfter(TokenType.RIGHT_PAREN, "expression");
        return new GroupingExpression(expr);
    }

    private Expression identifierExpression() {
        final Token next = this.peek();
        if (next.type() == TokenType.ARRAY_OPEN) {
            final Token name = this.previous();
            this.expect(TokenType.ARRAY_OPEN);
            final Expression index = this.expression();
            this.expect(TokenType.ARRAY_CLOSE);
            return new ArrayGetExpression(name, index);
        }
        return new VariableExpression(this.previous());
    }

    private Expression complexArray() {
        final Token open = this.previous();
        final Expression expr = this.expression();

        if (this.match(TokenType.ARRAY_CLOSE)) {
            final List<Expression> elements = new ArrayList<>();
            elements.add(expr);
            return new ArrayLiteralExpression(open, this.previous(), elements);
        }
        else if (this.match(TokenType.COMMA)) return this.arrayLiteralExpression(open, expr);
        else return this.arrayComprehensionExpression(open, expr);
    }

    private ArrayLiteralExpression arrayLiteralExpression(final Token open, final Expression expr) {
        final List<Expression> elements = new ArrayList<>();
        elements.add(expr);
        do {
            elements.add(this.expression());
        }
        while (this.match(TokenType.COMMA));
        final Token close = this.expectAfter(TokenType.ARRAY_CLOSE, "array");
        return new ArrayLiteralExpression(open, close, elements);
    }

    private ArrayComprehensionExpression arrayComprehensionExpression(final Token open, final Expression expr) {
        final Token forToken = this.expectAfter(TokenType.FOR, "expression");
        final Token name = this.expect(TokenType.IDENTIFIER, "variable name");

        final Token inToken = this.expectAfter(TokenType.IN, "variable name");
        final Expression iterable = this.expression();

        Token ifToken = null;
        Expression condition = null;
        if (this.match(TokenType.IF)) {
            ifToken = this.previous();
            condition = this.expression();
        }

        Token elseToken = null;
        Expression elseExpr = null;
        if (this.match(TokenType.ELSE)) {
            elseToken = this.previous();
            elseExpr = this.expression();
        }

        final Token close = this.expectAfter(TokenType.ARRAY_CLOSE, "array");

        return new ArrayComprehensionExpression(iterable, expr, name, forToken, inToken, open, close, ifToken, condition, elseToken, elseExpr);
    }

    private Token consume(final TokenType type, final String message) {
        if (this.check(type))
            return this.advance();
        if (type != TokenType.SEMICOLON) throw new ScriptEvaluationError(message, Phase.PARSING, this.peek());
        return null;
    }

    private Token expect(final TokenType type) {
        return this.expect(type, type.representation() + (type.keyword() ? " keyword" : ""));
    }

    private Token expect(final TokenType type, final String what) {
        return this.consume(type, "Expected " + what + ".");
    }

    private Token expectBefore(final TokenType type, final String before) {
        return this.expectAround(type, before, "before");
    }

    private Token expectAfter(final TokenType type, final TokenType after) {
        return this.expectAround(type, after.representation(), "after");
    }

    private Token expectAfter(final TokenType type, final String after) {
        return this.expectAround(type, after, "after");
    }

    private Token expectAround(final TokenType type, final String where, final String position) {
        return this.consume(type, "Expected '%s' %s %s.".formatted(type.representation(), position, where));
    }
}
