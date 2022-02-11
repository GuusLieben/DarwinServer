package org.dockbox.hartshorn.core.proxy;

import org.dockbox.hartshorn.core.MultiMap;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.core.domain.TypeMap;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class LazyProxyManager<T> implements ProxyManager<T> {

    private Class<T> proxyClass;
    private final Class<T> targetClass;
    private T proxy;

    private final Map<Method, ?> delegates;
    private final TypeMap<Object> typeDelegates;
    private final Map<Method, MethodInterceptor<T>> interceptors;
    private final MultiMap<Method, MethodWrapper<T>> wrappers;
    private final T delegate;

    public LazyProxyManager(final DefaultProxyFactory<T> proxyFactory) {
        this(null, proxyFactory.type(), proxyFactory.typeDelegate(), proxyFactory.delegates(), proxyFactory.typeDelegates(), proxyFactory.interceptors(), proxyFactory.wrappers());
    }

    public LazyProxyManager(final Class<T> proxyClass, final Class<T> targetClass, final T delegate, final Map<Method, ?> delegates, final TypeMap<Object> typeDelegates,
                            final Map<Method, MethodInterceptor<T>> interceptors, final MultiMap<Method, MethodWrapper<T>> wrappers) {
        // TODO: ApplicationContext to validate incoming values
        this.proxyClass = proxyClass;
        this.targetClass = targetClass;
        this.delegate = delegate;
        this.delegates = delegates;
        this.typeDelegates = typeDelegates;
        this.interceptors = interceptors;
        this.wrappers = wrappers;
    }

    public void proxy(final T proxy) {
        if (this.proxy != null) {
            throw new IllegalStateException("Proxy already set");
        }
        this.proxy = proxy;
    }

    @Override
    public Class<T> targetClass() {
        return this.targetClass;
    }

    @Override
    public Class<T> proxyClass() {
        if (this.proxyClass == null) {
            this.proxyClass = (Class<T>) this.proxy().getClass();
        }
        return this.proxyClass;
    }

    @Override
    public T proxy() {
        if (this.proxy == null) {
            throw new IllegalStateException("Proxy instance has not been set");
        }
        return this.proxy;
    }

    @Override
    public Exceptional<T> delegate() {
        return Exceptional.of(this.delegate);
    }

    @Override
    public Exceptional<T> delegate(final Method method) {
        return Exceptional.of(this.delegates).map(map -> map.get(method)).map(delegate -> (T) delegate);
    }

    @Override
    public <S> Exceptional<S> delegate(final Class<S> type) {
        return Exceptional.of(this.typeDelegates).map(map -> map.get(type)).map(type::cast);
    }

    @Override
    public Exceptional<MethodInterceptor<T>> interceptor(final Method method) {
        return Exceptional.of(this.interceptors).map(map -> map.get(method));
    }

    @Override
    public Set<MethodWrapper<T>> wrappers(final Method method) {
        return Set.copyOf(this.wrappers.get(method));
    }
}
