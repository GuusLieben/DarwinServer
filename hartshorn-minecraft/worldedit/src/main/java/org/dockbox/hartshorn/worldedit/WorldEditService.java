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

package org.dockbox.hartshorn.worldedit;

import org.dockbox.hartshorn.api.domain.Exceptional;
import org.dockbox.hartshorn.di.annotations.inject.Required;
import org.dockbox.hartshorn.server.minecraft.item.Item;
import org.dockbox.hartshorn.server.minecraft.players.Player;
import org.dockbox.hartshorn.worldedit.region.Clipboard;
import org.dockbox.hartshorn.worldedit.region.Mask;
import org.dockbox.hartshorn.worldedit.region.Pattern;
import org.dockbox.hartshorn.worldedit.region.Region;

import java.util.Collection;

@Required
public interface WorldEditService {

    Exceptional<Region> selection(Player player);

    void selection(Player player, Region region);

    Exceptional<Clipboard> clipboard(Player player);

    void clipboard(Player player, Clipboard clipboard);

    void replace(Region region, Mask mask, Pattern pattern, Player cause);

    void set(Region region, Pattern pattern, Player cause);

    Exceptional<Pattern> parsePattern(String pattern, Player cause);

    Exceptional<Mask> parseMask(String mask, Player cause);

    void replace(Region region, Collection<Item> mask, Collection<Item> pattern, Player cause);

    void set(Region region, Collection<Item> pattern, Player cause);

    boolean hasGlobalMask(Player player);

    Exceptional<Mask> globalMask(Player player);

    void globalMask(Player player, Mask mask);
}
