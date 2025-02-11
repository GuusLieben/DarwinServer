/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.org.dockbox.hartshorn.proxy;

import org.dockbox.hartshorn.proxy.Proxy;
import org.dockbox.hartshorn.proxy.ProxyFactory;
import org.dockbox.hartshorn.proxy.ProxyManager;
import org.dockbox.hartshorn.proxy.ProxyOrchestrator;
import org.dockbox.hartshorn.proxy.ProxyOrchestratorLoader;
import org.dockbox.hartshorn.proxy.advice.intercept.MethodInterceptor;
import org.dockbox.hartshorn.proxy.advice.wrap.MethodWrapper;
import org.dockbox.hartshorn.proxy.advice.wrap.ProxyCallbackContext;
import org.dockbox.hartshorn.proxy.constraint.ProxyConstraintViolationException;
import org.dockbox.hartshorn.proxy.lookup.StateAwareProxyFactory;
import org.dockbox.hartshorn.util.ApplicationException;
import org.dockbox.hartshorn.util.function.CheckedSupplier;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.view.ConstructorView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import test.org.dockbox.hartshorn.proxy.support.basic.ConcreteProxyWithNonDefaultConstructor;
import test.org.dockbox.hartshorn.proxy.support.basic.DescribedProxy;
import test.org.dockbox.hartshorn.proxy.support.equals.AbstractEqualProxy;
import test.org.dockbox.hartshorn.proxy.support.equals.EqualInterfaceProxy;
import test.org.dockbox.hartshorn.proxy.support.equals.EqualProxy;
import test.org.dockbox.hartshorn.proxy.support.inheritance.multi.AgedProxy;
import test.org.dockbox.hartshorn.proxy.support.inheritance.multi.NamedAgedProxy;
import test.org.dockbox.hartshorn.proxy.support.inheritance.multi.NamedProxy;
import test.org.dockbox.hartshorn.proxy.support.inheritance.single.AbstractProxy;
import test.org.dockbox.hartshorn.proxy.support.inheritance.single.ConcreteProxy;
import test.org.dockbox.hartshorn.proxy.support.inheritance.single.InterfaceProxy;
import test.org.dockbox.hartshorn.proxy.support.standard.ConcreteProxyTarget;
import test.org.dockbox.hartshorn.proxy.support.standard.FinalClassProxyTarget;
import test.org.dockbox.hartshorn.proxy.support.standard.FinalMethodProxyTarget;
import test.org.dockbox.hartshorn.proxy.support.standard.RecordProxy;
import test.org.dockbox.hartshorn.proxy.support.standard.SealedProxy;

/**
 * Tests for the default behavior of proxies of various types.
 *
 * @since 0.7.0
 *
 * @author Guus Lieben
 */
@SuppressWarnings("unchecked")
public abstract class ProxyTests {

    protected abstract ProxyOrchestratorLoader orchestratorLoader();
    protected abstract Introspector introspector();

    @Test
    void testConcreteMethodsCanBeProxied() throws ApplicationException, NoSuchMethodException {
        Method name = ConcreteProxyTarget.class.getMethod("name");
        ProxyFactory<ConcreteProxyTarget> handler = this.orchestratorLoader().create(this.introspector()).factory(ConcreteProxyTarget.class);
        handler.advisors().method(name).intercept(context -> "Hartshorn");
        ConcreteProxyTarget proxy = handler.proxy().get();

        Assertions.assertNotNull(proxy);
        Assertions.assertNotNull(proxy.name());
        Assertions.assertEquals("Hartshorn", proxy.name());
    }

    @Test
    void testFinalMethodsCanNotBeProxied() throws NoSuchMethodException {
        Method name = FinalMethodProxyTarget.class.getMethod("name");
        ProxyFactory<FinalMethodProxyTarget> handler = this.orchestratorLoader()
                .create(this.introspector())
                .factory(FinalMethodProxyTarget.class);

        Assertions.assertThrows(IllegalArgumentException.class, () -> handler.advisors()
                .method(name)
                .intercept(context -> "Hartshorn")
        );
    }

    public static Stream<Arguments> proxyTypes() {
        return Stream.of(
                Arguments.of(InterfaceProxy.class),
                Arguments.of(AbstractProxy.class),
                Arguments.of(ConcreteProxy.class)
        );
    }

    @Test
    void testRecordProxyCannotBeCreated() {
        // Records are and cannot be proxied
        ProxyFactory<RecordProxy> handler = this.orchestratorLoader().create(this.introspector()).factory(RecordProxy.class);
        Assertions.assertThrows(ProxyConstraintViolationException.class, handler::proxy);
    }

    @Test
    void testSealedClassProxyCannotBeCreated() {
        // Sealed classes only allow for a limited number of subclasses and should not be proxied
        ProxyFactory<SealedProxy> handler = this.orchestratorLoader().create(this.introspector()).factory(SealedProxy.class);
        Assertions.assertThrows(ProxyConstraintViolationException.class, handler::proxy);
    }

    @Test
    void testFinalClassProxyCannotBeCreated() {
        // Final classes cannot be extended and should not be proxied
        ProxyFactory<FinalClassProxyTarget> handler = this.orchestratorLoader().create(this.introspector()).factory(FinalClassProxyTarget.class);
        Assertions.assertThrows(ProxyConstraintViolationException.class, handler::proxy);
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testEmptyProxyCanCreate(Class<? extends InterfaceProxy> proxyParent) throws ApplicationException {
        ProxyFactory<? extends InterfaceProxy> handler = this.orchestratorLoader().create(this.introspector()).factory(proxyParent);
        InterfaceProxy proxy = handler.proxy().get();
        Assertions.assertNotNull(proxy);
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testMethodsCanBeDelegatedToOriginalInstance(Class<InterfaceProxy> proxyType) throws ApplicationException {
        ProxyFactory<InterfaceProxy> factory = this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        factory.advisors().type().delegate(new ConcreteProxy());
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        InterfaceProxy proxyInstance = proxy.get();
        Assertions.assertEquals("concrete", proxyInstance.name());
    }

    @Test
    void testConcreteProxyWithNonDefaultConstructorUsesConstructor() {
        StateAwareProxyFactory<ConcreteProxyWithNonDefaultConstructor> factory = this.orchestratorLoader().create(this.introspector()).factory(ConcreteProxyWithNonDefaultConstructor.class);

        TypeView<ConcreteProxyWithNonDefaultConstructor> typeView = this.introspector().introspect(ConcreteProxyWithNonDefaultConstructor.class);
        ConstructorView<ConcreteProxyWithNonDefaultConstructor> constructor = typeView.constructors().all().get(0);
        Option<ConcreteProxyWithNonDefaultConstructor> proxy = Assertions.assertDoesNotThrow(() -> factory.proxy(constructor, new Object[]{"Hello world"}));
        Assertions.assertTrue(proxy.present());

        ConcreteProxyWithNonDefaultConstructor proxyInstance = proxy.get();
        Assertions.assertEquals("Hello world", proxyInstance.message());
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testMethodsCanBeIntercepted(Class<? extends InterfaceProxy> proxyType) throws ApplicationException, NoSuchMethodException {
        ProxyFactory<? extends InterfaceProxy> factory = this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        factory.advisors().method(proxyType.getMethod("name")).intercept(context -> "Hartshorn");
        Option<? extends InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        InterfaceProxy proxyInstance = proxy.get();
        Assertions.assertEquals("Hartshorn", proxyInstance.name());
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testMethodsCanBeDelegated(Class<? extends InterfaceProxy> proxyType) throws ApplicationException, NoSuchMethodException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        factory.advisors().method(proxyType.getMethod("name")).delegate(new ConcreteProxy());
        Option<? extends InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        InterfaceProxy proxyInstance = proxy.get();
        Assertions.assertEquals("concrete", proxyInstance.name());
    }

    @Test
    void testTypesCanBeDelegated() throws ApplicationException {
        // Use a custom interface for this type of delegation, as the other proxy types override methods from their parent
        ProxyFactory<NamedAgedProxy> factory = this.orchestratorLoader().create(this.introspector()).factory(NamedAgedProxy.class);
        factory.advisors().type(AgedProxy.class).delegate(() -> 12);
        factory.advisors().type(NamedProxy.class).delegate(() -> "NamedProxy");
        Option<NamedAgedProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        NamedAgedProxy proxyInstance = proxy.get();
        Assertions.assertEquals(12, proxyInstance.age());
        Assertions.assertEquals("NamedProxy", proxyInstance.name());
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testWrapperInterceptionIsCorrect(Class<? extends InterfaceProxy> proxyType) throws NoSuchMethodException, ApplicationException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        AtomicInteger count = new AtomicInteger();
        factory.advisors().method(proxyType.getMethod("name")).intercept(context -> "done");
        factory.advisors().method(proxyType.getMethod("name")).wrapAround(new MethodWrapper<>() {
            @Override
            public void acceptBefore(ProxyCallbackContext<InterfaceProxy> context) {
                Assertions.assertEquals(0, count.getAndIncrement());
            }

            @Override
            public void acceptAfter(ProxyCallbackContext<InterfaceProxy> context) {
                Assertions.assertEquals(1, count.getAndIncrement());
            }

            @Override
            public void acceptError(ProxyCallbackContext<InterfaceProxy> context) {
                // Not thrown
                Assertions.fail();
            }
        });
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        InterfaceProxy proxyInstance = proxy.get();
        Assertions.assertEquals("done", proxyInstance.name());
        Assertions.assertEquals(2, count.get());
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testErrorWrapperInterceptionIsCorrect(Class<? extends InterfaceProxy> proxyType) throws NoSuchMethodException, ApplicationException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        AtomicInteger count = new AtomicInteger();
        factory.advisors().method(proxyType.getMethod("name")).intercept(context -> {
            throw new IllegalStateException("not done");
        });
        factory.advisors().method(proxyType.getMethod("name")).wrapAround(new MethodWrapper<>() {
            @Override
            public void acceptBefore(ProxyCallbackContext<InterfaceProxy> context) {
                Assertions.assertEquals(0, count.getAndIncrement());
            }

            @Override
            public void acceptAfter(ProxyCallbackContext<InterfaceProxy> context) {
                Assertions.fail();
            }

            @Override
            public void acceptError(ProxyCallbackContext<InterfaceProxy> context) {
                Throwable error = context.error();
                Assertions.assertNotNull(error);
                Assertions.assertTrue(error instanceof IllegalStateException);
                Assertions.assertEquals("not done", error.getMessage());
                Assertions.assertEquals(1, count.getAndIncrement());
            }
        });
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        InterfaceProxy proxyInstance = proxy.get();
        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class, proxyInstance::name);
        Assertions.assertEquals("not done", error.getMessage());
        Assertions.assertEquals(2, count.get());
    }

    @Test
    void testProxyManagerTracksInterceptorsAndDelegates() throws NoSuchMethodException, ApplicationException {
        ProxyFactory<NamedAgedProxy> factory = this.orchestratorLoader().create(this.introspector()).factory(NamedAgedProxy.class);

        AgedProxy aged = () -> 12;
        factory.advisors().type(AgedProxy.class).delegate(aged);

        MethodInterceptor<NamedAgedProxy, Object> named = context -> "NamedProxy";
        factory.advisors().method(NamedProxy.class.getMethod("name")).intercept(named);
        Option<NamedAgedProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        Proxy<?> proxyInstance = (Proxy<?>) proxy.get();
        ProxyManager<?> manager = proxyInstance.manager();

        Option<?> agedDelegate = manager.advisor()
                .resolver()
                .type(AgedProxy.class)
                .delegate();
        Assertions.assertTrue(agedDelegate.present());
        Assertions.assertSame(agedDelegate.get(), aged);

        Option<?> namedInterceptor = manager.advisor()
                .resolver()
                .method(NamedProxy.class.getMethod("name"))
                .interceptor();
        Assertions.assertTrue(namedInterceptor.present());
        Assertions.assertSame(namedInterceptor.get(), named);
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testProxyCanHaveExtraInterfaces(Class<? extends InterfaceProxy> proxyType) throws ApplicationException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        factory.implement(DescribedProxy.class);
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        InterfaceProxy proxyInstance = proxy.get();
        Assertions.assertTrue(proxyInstance instanceof DescribedProxy);
    }


    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testProxiesAlwaysImplementProxyType(Class<? extends InterfaceProxy> proxyType) throws ApplicationException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());
        InterfaceProxy proxyInstance = proxy.get();
        Assertions.assertTrue(proxyInstance instanceof Proxy);
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testProxiesExposeManager(Class<? extends InterfaceProxy> proxyType) throws ApplicationException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        Proxy<?> proxyInstance = (Proxy<?>) proxy.get();
        Assertions.assertNotNull(proxyInstance.manager());
    }

    @ParameterizedTest
    @MethodSource("proxyTypes")
    void testProxyManagerExposesTargetAndProxyType(Class<? extends InterfaceProxy> proxyType) throws ApplicationException {
        ProxyFactory<InterfaceProxy> factory = (ProxyFactory<InterfaceProxy>) this.orchestratorLoader().create(this.introspector()).factory(proxyType);
        Option<InterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        ProxyManager<InterfaceProxy> manager = ((Proxy<InterfaceProxy>) proxy.get()).manager();
        Assertions.assertNotNull(manager.proxyClass());
        Assertions.assertNotNull(manager.targetClass());

        Assertions.assertNotEquals(proxyType, manager.proxyClass());
        Assertions.assertSame(proxyType, manager.targetClass());

        Assertions.assertTrue(manager.orchestrator().isProxy(manager.proxyClass()));
    }

    @Test
    void testInterfaceProxyDoesNotEqual() throws ApplicationException {
        DemoServiceA serviceA1 = this.createProxy(DemoServiceA.class);
        DemoServiceA serviceA2 = this.createProxy(DemoServiceA.class);

        Assertions.assertNotSame(serviceA1, serviceA2);
        Assertions.assertNotEquals(serviceA1, serviceA2);
    }

    @Test
    void testAbstractClassProxyDoesNotEqual() throws ApplicationException {
        DemoServiceB serviceC1 = this.createProxy(DemoServiceB.class);
        DemoServiceB serviceC2 = this.createProxy(DemoServiceB.class);

        Assertions.assertNotSame(serviceC1, serviceC2);
        Assertions.assertNotEquals(serviceC1, serviceC2);
    }

    @Test
    void testConcreteClassProxyWithoutDelegateDoesNotEqual() throws ApplicationException {
        DemoServiceC serviceB1 = this.createProxy(DemoServiceC.class);
        DemoServiceC serviceB2 = this.createProxy(DemoServiceC.class);

        Assertions.assertNotSame(serviceB1, serviceB2);
        Assertions.assertNotEquals(serviceB1, serviceB2);
    }

    @Test
    public void testConcreteClassProxyWithNonEqualsImplementedDelegateDoesNotEqual() throws ApplicationException {
        CheckedSupplier<DemoServiceC> supplier = () -> this.orchestratorLoader().create(this.introspector())
                .factory(DemoServiceC.class)
                .advisors(advisors -> advisors.type().delegate(new DemoServiceC()))
                .proxy()
                .get();

        DemoServiceC serviceC3 = supplier.get();
        DemoServiceC serviceC4 = supplier.get();

        Assertions.assertNotSame(serviceC3, serviceC4);
        Assertions.assertNotEquals(serviceC3, serviceC4);
    }

    @Test
    void testConcreteClassProxyWithDelegateDoesNotEqual() throws ApplicationException {
        CheckedSupplier<DemoServiceD> supplier = () -> this.orchestratorLoader().create(this.introspector())
                .factory(DemoServiceD.class)
                .advisors(advisors -> advisors.type().delegate(new DemoServiceD("name")))
                .proxy()
                .get();

        DemoServiceD serviceD1 = supplier.get();
        DemoServiceD serviceD2 = supplier.get();

        Assertions.assertNotSame(serviceD1, serviceD2);
        Assertions.assertEquals(serviceD1, serviceD2);
    }

    private <T> T createProxy(Class<T> type) throws ApplicationException {
        return this.orchestratorLoader().create(this.introspector()).factory(type).proxy().get();
    }

    public interface DemoServiceA { }

    public abstract static class DemoServiceB { }

    public static class DemoServiceC { }

    public static class DemoServiceD {
        private String name;

        public DemoServiceD(String name) {
            this.name = name;
        }

        @SuppressWarnings("unused")
        public DemoServiceD() {
            // Default constructor for proxying. Note that this is typically handled by providing a constructor to the
            // proxy factory, but this is a test, so we're not doing that.
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || this.getClass() != other.getClass()) {
                return false;
            }
            DemoServiceD service = (DemoServiceD) other;
            return this.name.equals(service.name);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
    }

    @Test
    void testConcreteProxySelfEquality() throws ApplicationException {
        ProxyFactory<EqualProxy> factory = this.orchestratorLoader().create(this.introspector()).factory(EqualProxy.class);
        Option<EqualProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        EqualProxy proxyInstance = proxy.get();
        Assertions.assertEquals(proxyInstance, proxyInstance);
        Assertions.assertTrue(proxyInstance.test(proxyInstance));
    }

    @Test
    void testServiceSelfEquality() throws ApplicationException {
        AbstractEqualProxy service = this.orchestratorLoader().create(this.introspector()).factory(AbstractEqualProxy.class).proxy().get();
        Assertions.assertEquals(service, service);
        Assertions.assertTrue(service.test(service));
    }

    @Test
    void testInterfaceProxySelfEquality() throws ApplicationException {
        ProxyFactory<EqualInterfaceProxy> factory = this.orchestratorLoader().create(this.introspector()).factory(EqualInterfaceProxy.class);
        Option<EqualInterfaceProxy> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());

        EqualInterfaceProxy proxyInstance = proxy.get();
        Assertions.assertEquals(proxyInstance, proxyInstance);
        Assertions.assertTrue(proxyInstance.test(proxyInstance));
    }

    @Test
    void testLambdaCanBeProxied() throws NoSuchMethodException, ApplicationException {
        Class<Supplier<String>> supplierClass = (Class<Supplier<String>>) (Class<?>) Supplier.class;
        StateAwareProxyFactory<Supplier<String>> factory = this.orchestratorLoader().create(this.introspector()).factory(supplierClass);
        factory.advisors().method(Supplier.class.getMethod("get")).intercept(context -> "foo");
        Option<Supplier<String>> proxy = factory.proxy();
        Assertions.assertTrue(proxy.present());
        Assertions.assertEquals("foo", proxy.get().get());
    }

    @Test
    void testIsProxyIsTrueIfTypeIsProxy() throws ApplicationException {
        Introspector introspector = this.introspector();
        ProxyOrchestrator orchestrator = this.orchestratorLoader().create(introspector);
        ProxyFactory<?> factory = orchestrator.factory(Object.class);
        Object proxy = factory.proxy().get();

        boolean instanceIsProxy = orchestrator.isProxy(proxy);
        Assertions.assertTrue(instanceIsProxy);

        boolean typeIsProxy = orchestrator.isProxy(proxy.getClass());
        Assertions.assertTrue(typeIsProxy);
    }

    @Test
    void testIsProxyIsFalseIfTypeIsNormal() {
        Introspector introspector = this.introspector();
        ProxyOrchestrator orchestrator = this.orchestratorLoader().create(introspector);
        TypeView<?> view = introspector.introspect(Object.class);
        boolean isProxy = orchestrator.isProxy(view);
        Assertions.assertFalse(isProxy);
    }
}
