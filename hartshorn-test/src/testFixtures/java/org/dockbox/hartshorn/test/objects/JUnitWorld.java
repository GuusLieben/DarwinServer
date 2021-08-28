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

package org.dockbox.hartshorn.test.objects;

import org.dockbox.hartshorn.api.domain.Exceptional;
import org.dockbox.hartshorn.api.domain.tuple.Vector3N;
import org.dockbox.hartshorn.di.context.ApplicationContext;
import org.dockbox.hartshorn.server.minecraft.dimension.Block;
import org.dockbox.hartshorn.server.minecraft.dimension.Chunk;
import org.dockbox.hartshorn.server.minecraft.dimension.position.Location;
import org.dockbox.hartshorn.server.minecraft.dimension.world.World;
import org.dockbox.hartshorn.server.minecraft.entities.Entity;
import org.dockbox.hartshorn.server.minecraft.players.Gamemode;
import org.dockbox.hartshorn.server.minecraft.players.Player;
import org.dockbox.hartshorn.util.HartshornUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import lombok.Getter;

public class JUnitWorld extends World {

    private final Map<Vector3N, Block> blocks = HartshornUtils.emptyMap();
    private final Map<UUID, Entity> entities = HartshornUtils.emptyMap();

    @Getter private final Map<String, String> gamerules = HartshornUtils.emptyMap();
    @Getter private final ApplicationContext applicationContext;
    @Getter private boolean loaded;

    public JUnitWorld(final ApplicationContext context, final UUID worldUniqueId, final String name, final boolean loadOnStartup, @NotNull final Vector3N spawnPosition, final long seed, final Gamemode defaultGamemode) {
        super(worldUniqueId, name, loadOnStartup, spawnPosition, seed, defaultGamemode);
        this.loaded = loadOnStartup;
        this.applicationContext = context;
    }

    @Override
    public Vector3N minimumPosition() {
        return Vector3N.of(-16, 0, -16);
    }

    @Override
    public Vector3N maximumPosition() {
        return Vector3N.of(16, 256, 16);
    }

    @Override
    public Vector3N floor(final Vector3N position) {
        return Vector3N.of(position.xD(), 64, position.zD());
    }

    @Override
    public boolean has(final Vector3N position) {
        return this.blocks.containsKey(position);
    }

    @Override
    public Exceptional<Block> block(final Vector3N position) {
        return Exceptional.of(this.blocks.getOrDefault(position, null));
    }

    @Override
    public boolean block(final Vector3N position, final Block block) {
        this.blocks.put(position, block);
        return true;
    }

    @Override
    public Exceptional<Chunk> chunk(final Location location) {
        return Exceptional.empty();
    }

    @Override
    public Exceptional<Chunk> chunk(final Vector3N position) {
        return Exceptional.empty();
    }

    @Override
    public Collection<Chunk> loadedChunks() {
        return HartshornUtils.emptyList();
    }

    @Override
    public int playerCount() {
        return this.entities(entity -> entity instanceof Player).size();
    }

    @Override
    public Collection<Entity> entities() {
        return this.entities.values();
    }

    @Override
    public Collection<Entity> entities(final Predicate<Entity> predicate) {
        return this.entities().stream().filter(predicate).toList();
    }

    @Override
    public boolean unload() {
        this.loaded = false;
        return true;
    }

    @Override
    public boolean load() {
        this.loaded = true;
        return true;
    }

    @Override
    public void gamerule(final String key, final String value) {
        this.gamerules.put(key, value);
    }

    public void addEntity(final Entity entity) {
        this.entities.put(entity.uniqueId(), entity);
    }

    public void destroyEntity(final UUID uuid) {
        this.entities.remove(uuid);
    }
}
