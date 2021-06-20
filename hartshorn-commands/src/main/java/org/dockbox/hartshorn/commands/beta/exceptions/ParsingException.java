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

package org.dockbox.hartshorn.commands.beta.exceptions;

import org.dockbox.hartshorn.api.i18n.common.ResourceEntry;

import lombok.Getter;

public class ParsingException extends Exception {

    @Getter
    private final ResourceEntry resource;

    public ParsingException(ResourceEntry resource) {
        super(resource.plain());
        this.resource = resource;
    }

    public ParsingException(ResourceEntry resource, Throwable cause) {
        super(resource.plain(), cause);
        this.resource = resource;
    }
}
