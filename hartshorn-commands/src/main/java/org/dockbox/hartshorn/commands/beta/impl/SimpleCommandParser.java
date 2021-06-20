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

package org.dockbox.hartshorn.commands.beta.impl;

import org.dockbox.hartshorn.api.domain.Exceptional;
import org.dockbox.hartshorn.api.i18n.common.ResourceEntry;
import org.dockbox.hartshorn.api.i18n.entry.FakeResource;
import org.dockbox.hartshorn.commands.beta.api.CommandContainerContext;
import org.dockbox.hartshorn.commands.beta.api.CommandElement;
import org.dockbox.hartshorn.commands.beta.api.CommandExecutorContext;
import org.dockbox.hartshorn.commands.beta.api.CommandFlag;
import org.dockbox.hartshorn.commands.beta.api.CommandParser;
import org.dockbox.hartshorn.commands.beta.api.ParsedContext;
import org.dockbox.hartshorn.commands.beta.exceptions.ParsingException;
import org.dockbox.hartshorn.commands.context.CommandParameter;
import org.dockbox.hartshorn.commands.source.CommandSource;
import org.dockbox.hartshorn.di.annotations.Binds;
import org.dockbox.hartshorn.util.HartshornUtils;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Binds(CommandParser.class)
public class SimpleCommandParser implements CommandParser {

    private static final Pattern FLAG = Pattern.compile("-(-?\\w+)(?: ([^ -]+))?");

    @Override
    public Exceptional<ParsedContext> parse(String command, CommandSource source, CommandExecutorContext context) throws ParsingException {
        final Exceptional<CommandContainerContext> container = context.first(CommandContainerContext.class);
        if (container.absent()) return Exceptional.empty();

        final CommandContainerContext containerContext = container.get();
        List<CommandElement<?>> elements = containerContext.elements();

        final List<CommandParameter<?>> parsedElements = HartshornUtils.emptyList();
        final List<CommandParameter<?>> parsedFlags = HartshornUtils.emptyList();

        // Ensure no aliases are left so they are not accidentally parsed
        command = context.strip(command);
        // Strip all flags beforehand so elements can be parsed safely without flag interference
        command = this.stripFlags(command, parsedFlags, source, containerContext);

        for (CommandElement<?> element : elements) {
            // TODO, actually parse elements
            element.parse(source, "");
        }

        return Exceptional.of(new SimpleParsedContext(command,
                parsedElements,
                parsedFlags,
                source, HartshornUtils.singletonList(containerContext.permission())));
    }

    private String stripFlags(String command, Collection<CommandParameter<?>> flags, CommandSource source, CommandContainerContext context) throws ParsingException {
        final Matcher matcher = FLAG.matcher(command);
        while (matcher.find()) {
            final String flag = matcher.group().substring(1); // Discard - prefix
            String name = flag.split(" ")[0];
            final CommandFlag commandFlag = context.flag(name);
            if (commandFlag.value()) {
                if (commandFlag instanceof CommandFlagElement) {
                    final Exceptional<?> value = ((CommandFlagElement<?>) commandFlag).parse(source, flag.split(" ")[1]);
                    if (value.absent()) {
                        ResourceEntry resource = new FakeResource("Could not parse flag '" + name + "'");
                        throw value.caught() ? new ParsingException(resource, value.error()) : new ParsingException(resource);
                    } else {
                        flags.add(new CommandParameter<>(value.get(), name));
                    }
                }
                command = command.replace('-' + flag, "");
            } else {
                flags.add(new CommandParameter<>(null, name));
                command = command.replace('-' + name, "");
            }
        }
        return command.trim();
    }

}
