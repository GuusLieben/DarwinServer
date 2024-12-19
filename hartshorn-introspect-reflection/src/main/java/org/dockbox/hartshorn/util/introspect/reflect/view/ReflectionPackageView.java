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

package org.dockbox.hartshorn.util.introspect.reflect.view;

import org.dockbox.hartshorn.reporting.DiagnosticsPropertyCollector;
import org.dockbox.hartshorn.util.StringUtilities;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.view.EnclosableView;
import org.dockbox.hartshorn.util.introspect.view.PackageView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.AnnotatedElement;

/**
 * A view that provides access to the package of a class. This view is backed by a {@link Package} instance.
 *
 * @since 0.5.0
 *
 * @author Guus Lieben
 */
public class ReflectionPackageView extends ReflectionAnnotatedElementView implements PackageView {

    private final Package pkg;

    public ReflectionPackageView(Introspector introspector, Package pkg) {
        super(introspector);
        this.pkg = pkg;
    }

    @Override
    public String name() {
        return this.pkg.getName();
    }

    @Override
    public String specificationTitle() {
        return StringUtilities.emptyIfNull(this.pkg.getSpecificationTitle());
    }

    @Override
    public String specificationVendor() {
        return StringUtilities.emptyIfNull(this.pkg.getSpecificationVendor());
    }

    @Override
    public String specificationVersion() {
        return StringUtilities.emptyIfNull(this.pkg.getSpecificationVersion());
    }

    @Override
    public String implementationTitle() {
        return StringUtilities.emptyIfNull(this.pkg.getImplementationTitle());
    }

    @Override
    public String implementationVendor() {
        return StringUtilities.emptyIfNull(this.pkg.getImplementationVendor());
    }

    @Override
    public String implementationVersion() {
        return StringUtilities.emptyIfNull(this.pkg.getImplementationVersion());
    }

    @Override
    public boolean isSealed() {
        return this.pkg.isSealed();
    }

    @Override
    public String qualifiedName() {
        return this.name();
    }

    @Override
    protected AnnotatedElement annotatedElement() {
        return this.pkg;
    }

    @Override
    public void report(DiagnosticsPropertyCollector collector) {
        collector.property("name").writeString(this.name());
        collector.property("specificationTitle").writeString(this.specificationTitle());
        collector.property("specificationVendor").writeString(this.specificationVendor());
        collector.property("specificationVersion").writeString(this.specificationVersion());
    }

    @Override
    public boolean isEnclosed() {
        return false;
    }

    @Override
    public Option<EnclosableView> enclosingView() {
        return Option.empty();
    }
}
