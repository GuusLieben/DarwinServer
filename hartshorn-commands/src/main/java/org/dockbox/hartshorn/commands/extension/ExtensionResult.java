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

package org.dockbox.hartshorn.commands.extension;

import org.dockbox.hartshorn.api.i18n.common.ResourceEntry;
import org.dockbox.hartshorn.api.i18n.entry.FakeResource;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class ExtensionResult {

    private final boolean proceed;
    private final ResourceEntry reason;
    private final boolean send;

    public boolean proceed() {
        return this.proceed;
    }

    public ResourceEntry reason() {
        return this.reason;
    }

    public boolean send() {
        return this.send;
    }

    public static ExtensionResult accept() {
        return new ExtensionResult(true, new FakeResource(""), false);
    }

    public static ExtensionResult reject(ResourceEntry reason) {
        return reject(reason, true);
    }

    public static ExtensionResult reject(ResourceEntry reason, boolean send) {
        return new ExtensionResult(false, reason, send);
    }
}
