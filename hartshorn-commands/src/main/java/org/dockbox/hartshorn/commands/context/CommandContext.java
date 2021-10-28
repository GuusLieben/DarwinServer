/*
 * Copyright (C) 2020 Guus Lieben
 *
 * This framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.hartshorn.commands.context;

import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.commands.CommandSource;
import org.dockbox.hartshorn.commands.service.CommandParameter;
import org.dockbox.hartshorn.core.context.ContextCarrier;
import org.dockbox.hartshorn.i18n.permissions.Permission;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

/**
 * The context provided to a {@link org.dockbox.hartshorn.commands.CommandExecutor} during
 * command execution. This context provides access to parsed arguments, flags and other
 * related command context.
 */
public interface CommandContext extends ParserContext, ContextCarrier {

    /**
     * Gets the argument or flag associated with the given <code>key</code>, if it exists. If
     * no argument or flag with the given <code>key</code> exists, <code>null</code> is returned
     * instead. The value of the argument is cast to type <code>T</code>.
     *
     * @param key
     *         The key of the argument or flag
     * @param <T>
     *         The expected type of the argument or flag
     *
     * @return The argument or flag, or <code>null</code>
     * @throws ClassCastException
     *         If the argument or flag is not of type <code>T</code>
     */
    <T> T get(String key);

    /**
     * Checks for the presence of an argument or flag associated with the given <code>key</code>.
     *
     * @param key
     *         The key of the argument or flag
     *
     * @return <code>true</code> if a argument or flag exists, else <code>false</code>
     */
    boolean has(String key);

    /**
     * Gets the argument or flag associated with the given <code>key</code>, if it exists. The
     * value of the argument is cast to type <code>T</code>. If the argument or flag is not of type
     * <code>T</code>, or does not exist, {@link Exceptional#empty()} is returned instead.
     *
     * @param key
     *         The key of the argument or flag
     * @param <T>
     *         The expected type of the argument or flag
     *
     * @return The argument or flag wrapped in a {@link Exceptional}, or {@link Exceptional#empty()}
     */
    <T> Exceptional<T> find(String key);

    /**
     * Gets the first {@link CommandParameter} in the form of an argument associated with the given
     * <code>key</code>, if it exists. If the argument is not of type <code>T</code>, or does not exist,
     * {@link Exceptional#empty()} is returned instead. The {@link CommandParameter} contains both the
     * defined key and value of the argument.
     *
     * @param key
     *         The key of the argument
     * @param <T>
     *         The expected type of the argument
     *
     * @return The argument wrapped in a {@link Exceptional}, or {@link Exceptional#empty()}
     */
    <T> Exceptional<CommandParameter<T>> argument(String key);

    /**
     * Gets the first {@link CommandParameter} in the form of a flag associated with the given
     * <code>key</code>, if it exists. If the flag is not of type <code>T</code>, or does not exist,
     * {@link Exceptional#empty()} is returned instead. The {@link CommandParameter} contains both the
     * defined key and value of the flag.
     *
     * @param key
     *         The key of the flag
     * @param <T>
     *         The expected type of the flag
     *
     * @return The flag wrapped in a {@link Exceptional}, or {@link Exceptional#empty()}
     */
    <T> Exceptional<CommandParameter<T>> flag(String key);

    /**
     * Gets the {@link CommandSource} responsible for executing the command. The source is capable
     * if sending and receiving messages and should be used as output for error messages. Exceptions
     * should not be returned to this source.
     *
     * @return The {@link CommandSource} responsible for executing the command.
     */
    CommandSource source();

    /**
     * Gets the permissions required to execute this command.
     *
     * @return The required permissions.
     */
    @UnmodifiableView
    List<Permission> permissions();

    /**
     * Gets the raw command as it was provided by the {@link #source()}. For example, if the command has
     * the following definition:
     * <pre><code>
     *     "command &#60;argument&#62;"
     * </code></pre>
     * The raw command may look like:
     * <pre><code>
     *     "command argumentValue"
     * </code></pre>
     *
     * @return The raw command
     */
    String command();
}
