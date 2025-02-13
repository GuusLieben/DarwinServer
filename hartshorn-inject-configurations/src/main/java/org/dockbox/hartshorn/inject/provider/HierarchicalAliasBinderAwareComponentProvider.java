package org.dockbox.hartshorn.inject.provider;

import org.dockbox.hartshorn.inject.binding.HierarchicalAliasCapableBinder;

public interface HierarchicalAliasBinderAwareComponentProvider extends HierarchicalBinderAwareComponentProvider {

    @Override
    HierarchicalAliasCapableBinder binder();
}
