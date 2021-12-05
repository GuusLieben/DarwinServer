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

package org.dockbox.hartshorn.core.boot;

import org.dockbox.hartshorn.core.annotations.context.LogExclude;
import org.dockbox.hartshorn.core.context.element.TypeContext;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The utility type to grant easy access to static components and constants.
 */
@LogExclude
public final class Hartshorn {

    /**
     * The default package prefix to use when scanning Hartshorn internals.
     */
    public static final String PACKAGE_PREFIX = "org.dockbox.hartshorn";
    /**
     * The (human-readable) display name of Hartshorn.
     */
    public static final String PROJECT_NAME = "Hartshorn";
    /**
     * The simplified identifier for Hartshorn-default identifiers.
     */
    public static final String PROJECT_ID = "hartshorn";
    /**
     * The semantic version of the current/latest release of Hartshorn
     */
    public static final String VERSION = "4.2.5";

    private static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();

    private Hartshorn() {}

    /**
     * Gets a log instance representing the calling type.
     *
     * @return The {@link Logger}
     */
    public static Logger log() {
        StackTraceElement element = null;
        for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            final boolean isJavaModule = ste.getModuleName() != null && ste.getModuleName().startsWith("java.");
            final boolean isExcluded = TypeContext.lookup(ste.getClassName().split("\\$")[0]).annotation(LogExclude.class).present();
            if (!isJavaModule && !isExcluded) {
                element = ste;
                break;
            }
        }

        if (element == null) throw new IllegalStateException("Could not determine caller from stacktrace");

        final String className = element.getClassName().split("\\$")[0];
        if (LOGGERS.containsKey(className)) return LOGGERS.get(className);

        final Logger logger = LoggerFactory.getLogger(TypeContext.lookup(className).type());
        LOGGERS.put(className, logger);
        return logger;
    }

    /**
     * Attempts to look up a resource file. If the file exists it is wrapped in a {@link Exceptional}
     * and returned. If the file does not exist or is a directory, {@link Exceptional#empty()} is
     * returned. If the requested file name is invalid, or {@code null}, a {@link Exceptional}
     * containing the appropriate exception is returned.
     *
     * @param name The name of the file to look up
     *
     * @return The resource file wrapped in a {@link Exceptional}, or an appropriate {@link Exceptional} (either none or providing the appropriate exception).
     */
    public static Exceptional<Path> resource(final String name) {
        try {
            final InputStream in = Hartshorn.class.getClassLoader().getResourceAsStream(name);
            if (in == null) {
                log().debug("Could not locate resource " + name);
                return Exceptional.empty();
            }

            final byte[] buffer = new byte[in.available()];
            final int bytes = in.read(buffer);
            if (bytes == -1) return Exceptional.of(new IOException("Requested resource contained no context"));

            final String[] parts = name.split("/");
            final String fileName = parts[parts.length - 1];
            final Path tempFile = Files.createTempFile(fileName, ".tmp");
            log().debug("Writing compressed resource " + name + " to temporary file " + tempFile.toFile().getName());
            final OutputStream outStream = new FileOutputStream(tempFile.toFile());
            outStream.write(buffer);
            outStream.flush();
            outStream.close();

            return Exceptional.of(tempFile);
        }
        catch (final NullPointerException | IOException e) {
            return Exceptional.of(e);
        }
    }
}

