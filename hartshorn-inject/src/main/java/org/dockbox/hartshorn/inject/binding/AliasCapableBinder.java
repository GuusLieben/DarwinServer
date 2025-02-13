package org.dockbox.hartshorn.inject.binding;

import org.dockbox.hartshorn.inject.ComponentKey;

public interface AliasCapableBinder extends Binder {

    @Override
    default <C> AliasBindingFunction<C> bind(Class<C> type) {
        return (AliasBindingFunction<C>) Binder.super.bind(type);
    }

    @Override
    <C> AliasBindingFunction<C> bind(ComponentKey<C> key);
}
