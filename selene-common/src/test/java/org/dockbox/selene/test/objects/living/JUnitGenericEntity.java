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

package org.dockbox.selene.test.objects.living;

import org.dockbox.selene.api.entities.Entity;
import org.dockbox.selene.api.objects.location.dimensions.World;
import org.dockbox.selene.api.objects.location.position.Location;
import org.dockbox.selene.api.text.Text;
import org.dockbox.selene.test.objects.JUnitWorld;

public abstract class JUnitGenericEntity<T extends Entity<T>> implements Entity<T> {

    private Text displayName;
    private double health = 20;

    private Location location;
    private boolean invisible = false;
    private boolean invulnerable = false;
    private boolean gravity = true;

    @Override
    public Text getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(Text displayName) {
        this.displayName = displayName;
    }

    @Override
    public double getHealth() {
        return this.health;
    }

    @Override
    public void setHealth(double health) {
        this.health = health;
    }

    @Override
    public boolean isAlive() {
        return this.getHealth() > 0;
    }

    @Override
    public boolean isInvisible() {
        return this.invisible;
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    @Override
    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    @Override
    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    @Override
    public boolean hasGravity() {
        return this.gravity;
    }

    @Override
    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public void setLocation(Location location) {
        ((JUnitWorld) this.getWorld()).destroyEntity(this.getUniqueId());
        this.location = location;
        ((JUnitWorld) this.getWorld()).addEntity(this);
    }

    @Override
    public World getWorld() {
        return this.getLocation().getWorld();
    }
}
