package org.dockbox.hartshorn.inject.binding;

import org.dockbox.hartshorn.inject.ComponentKey;
import org.dockbox.hartshorn.inject.IllegalScopeException;
import org.dockbox.hartshorn.inject.QualifierKey;
import org.dockbox.hartshorn.inject.scope.ScopeKey;

public interface AliasBindingFunction<T> extends BindingFunction<T> {

    AliasBindingFunction<T> alias(Class<? super T> aliasType);

    AliasBindingFunction<T> alias(ComponentKey<? super T> aliasKey);

    AliasBindingFunction<T> alias(QualifierKey<T> aliasQualifier);

    @Override
    AliasBindingFunction<T> installTo(ScopeKey scope) throws IllegalScopeException;

    @Override
    AliasBindingFunction<T> processAfterInitialization(boolean processAfterInitialization);

    @Override
    AliasBindingFunction<T> priority(int priority);
}
