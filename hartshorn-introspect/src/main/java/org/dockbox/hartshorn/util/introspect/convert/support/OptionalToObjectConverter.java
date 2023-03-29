package org.dockbox.hartshorn.util.introspect.convert.support;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dockbox.hartshorn.util.introspect.convert.Converter;

import java.util.Optional;

public class OptionalToObjectConverter implements Converter<Optional<?>, Object> {

    @Override
    public @Nullable Object convert(final @Nullable Optional<?> input) {
        assert input != null;
        return input.orElse(null);
    }
}
