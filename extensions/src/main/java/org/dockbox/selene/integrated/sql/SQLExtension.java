/*
 *  Copyright (C) 2020 Guus Lieben
 *
 *  This framework is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, either version 2.1 of the
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 *  the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.selene.integrated.sql;

import org.dockbox.selene.core.annotations.extension.Extension;

/**
 Registers the SQL Extension to Selene. While no logic happens inside this class, this does indicate to server owners
 that the extension is active.
 */
@Extension(id = "selene-sql", name = "SQL Extension",
           description = "Provides the ability to communicate with SQL instances using integrated types",
           authors = "GuusLieben", uniqueId = "5640d082-a612-40a7-84e7-fc2aeea2743b")
public class SQLExtension { }
