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

package org.dockbox.selene.sponge.objects.bossbar;

import org.dockbox.selene.core.objects.Exceptional;
import org.dockbox.selene.core.objects.FieldReferenceHolder;
import org.dockbox.selene.core.objects.bossbar.Bossbar;
import org.dockbox.selene.core.objects.bossbar.BossbarColor;
import org.dockbox.selene.core.objects.bossbar.BossbarStyle;
import org.dockbox.selene.core.objects.player.Player;
import org.dockbox.selene.core.server.Selene;
import org.dockbox.selene.core.tasks.TaskRunner;
import org.dockbox.selene.core.text.Text;
import org.dockbox.selene.core.util.SeleneUtils;
import org.dockbox.selene.sponge.util.SpongeConversionUtil;
import org.spongepowered.api.boss.ServerBossBar;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SpongeBossbar extends Bossbar {

    // Keeps bossbars in memory as long as they are active.
    // If they are no longer visible to at least one player the bossbar can be disposed of.
    public static transient final Map<String, SpongeBossbar> activeBossbars = SeleneUtils.COLLECTION.emptyConcurrentMap();

    private final FieldReferenceHolder<ServerBossBar> reference;

    public SpongeBossbar(String id, float percent, Text text, BossbarColor color, BossbarStyle overlay) {
        super(id, percent, text, color, overlay);
        this.reference = new FieldReferenceHolder<>(Exceptional.of(this.constructReference()), bar -> {
            bar.setName(SpongeConversionUtil.toSponge(this.getText()));
            bar.setPercent(this.getPercent());
            bar.setColor(SpongeConversionUtil.toSponge(color));
            bar.setOverlay(SpongeConversionUtil.toSponge(overlay));
            return Exceptional.of(bar);
        }, ServerBossBar.class);
    }

    @Override
    public void tick() {
        this.reference.updateReference();
    }

    @Override
    public void showTo(Player player) {
        this.reference.getReference().ifPresent(serverBossBar -> {
            SpongeConversionUtil.toSponge(player).ifPresent(serverBossBar::addPlayer);
            if (activeBossbars.containsKey(this.getId())) Selene.log().warn("Adding a bossbar with duplicate ID '" + this.getId() + "' to " + player.getName() + ". This may cause unexpected behavior!");
            else if (activeBossbars.containsValue(this)) Selene.log().warn("Adding identical bossbar with different ID '" + this.getId()  + "'. This may cause unexpected behavior!");
            activeBossbars.put(this.getId(), this);
        });
    }

    @Override
    public void showTo(Player player, Duration duration) {
        this.reference.getReference().ifPresent(serverBossBar -> {
            SpongeConversionUtil.toSponge(player).ifPresent(sp -> {
                serverBossBar.addPlayer(sp);
                TaskRunner.create().acceptDelayed(
                        () -> this.hideFrom(player),
                        duration.getSeconds(),
                        TimeUnit.SECONDS
                );
            });
            if (activeBossbars.containsKey(this.getId())) Selene.log().warn("Adding a bossbar with duplicate ID '" + this.getId() + "' to " + player.getName() + ". This may cause unexpected behavior!");
            else if (activeBossbars.containsValue(this)) Selene.log().warn("Adding identical bossbar with different ID '" + this.getId()  + "'. This may cause unexpected behavior!");
            activeBossbars.put(this.getId(), this);
        });
    }

    @Override
    public void hideFrom(Player player) {
        this.reference.getReference().ifPresent(serverBossBar -> {
            SpongeConversionUtil.toSponge(player).ifPresent(serverBossBar::removePlayer);
            if (serverBossBar.getPlayers().isEmpty()) activeBossbars.remove(this.getId());
        });
    }

    private ServerBossBar constructReference() {
        return ServerBossBar.builder()
                .name(SpongeConversionUtil.toSponge(this.getText()))
                .percent(this.getPercent())
                .color(SpongeConversionUtil.toSponge(this.getColor()))
                .overlay(SpongeConversionUtil.toSponge(this.getStyle()))
                .build();
    }
}
