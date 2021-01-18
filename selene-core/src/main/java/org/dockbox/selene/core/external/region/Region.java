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

package org.dockbox.selene.core.external.region;

import org.dockbox.selene.core.objects.location.World;
import org.dockbox.selene.core.objects.tuple.Vector3N;
import org.dockbox.selene.core.util.SeleneUtils;

public class Region {

    private final World world;
    private final Vector3N from;
    private final Vector3N to;

    public Region(World world, Vector3N from, Vector3N to) {
        this.world = world;
        this.from = from;
        this.to = to;
    }

    public Vector3N getMinimumPoint() {
        return SeleneUtils.getMinimumPoint(this.from, this.to);
    }

    public Vector3N getMaximumPoint() {
        return SeleneUtils.getMaximumPoint(this.from, this.to);
    }

    public Vector3N getCenter() {
        return SeleneUtils.getCenterPoint(this.from, this.to);
    }

    public int getArea() {
        return this.getWidth() * this.getWidth() * this.getLength();
    }

    public int getWidth() {
        return this.getMaximumPoint().getXi() - this.getMinimumPoint().getXi();
    }

    public int getHeight() {
        return this.getMaximumPoint().getYi() - this.getMinimumPoint().getYi();
    }

    public int getLength() {
        return this.getMaximumPoint().getZi() - this.getMinimumPoint().getZi();
    }

    public World getWorld() {
        return this.world;
    }

}
