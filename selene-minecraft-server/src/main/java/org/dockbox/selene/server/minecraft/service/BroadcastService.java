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

package org.dockbox.selene.server.minecraft.service;

import org.dockbox.selene.api.i18n.permissions.Permission;
import org.dockbox.selene.api.i18n.text.Text;
import org.dockbox.selene.server.minecraft.players.Player;

import java.util.function.Predicate;

public interface BroadcastService {

    void broadcastPublic(Text message);

    void broadcastWithFilter(Text message, Predicate<Player> filter);

    void broadcastForPermission(Text message, Permission permission);

    void broadcastForPermission(Text message, String permission);

    void broadcastForPermissionWithFilter(Text message, Permission permission, Predicate<Player> filter);

    void broadcastForPermissionWithFilter(Text message, String permission, Predicate<Player> filter);
}
