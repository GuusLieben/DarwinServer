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

package org.dockbox.hartshorn.persistence;

import org.dockbox.hartshorn.core.exceptions.ApplicationException;
import org.dockbox.hartshorn.core.properties.Attribute;
import org.dockbox.hartshorn.persistence.mapping.ObjectMapper;
import org.dockbox.hartshorn.persistence.properties.ModifiersAttribute;
import org.dockbox.hartshorn.persistence.properties.PersistenceModifier;

public abstract class DefaultObjectMapper implements ObjectMapper {

    protected FileType fileType;

    protected DefaultObjectMapper(final FileType fileType) {
        this.fileType = fileType;
    }

    @Override
    public ObjectMapper fileType(final FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    @Override
    public FileType fileType() {
        return this.fileType;
    }

    @Override
    public void apply(final Attribute<?> property) throws ApplicationException {
        if (property instanceof ModifiersAttribute modifiersAttribute) {
            for (final PersistenceModifier modifier : modifiersAttribute.value()) this.modify(modifier);
        }
    }

    protected abstract void modify(PersistenceModifier modifier);
}
