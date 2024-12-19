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
import org.dockbox.hartshorn.util.introspect.ElementModifiersIntrospector;
import org.dockbox.hartshorn.util.introspect.IllegalIntrospectionException;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.annotations.Property;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionElementModifiersIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectionIntrospector;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectiveFieldAccess;
import org.dockbox.hartshorn.util.introspect.reflect.ReflectiveFieldWriter;
import org.dockbox.hartshorn.util.introspect.view.EnclosableView;
import org.dockbox.hartshorn.util.introspect.view.FieldView;
import org.dockbox.hartshorn.util.introspect.view.MethodView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.List;

/**
 * A view that provides access to the fields of a class. This view is backed by a {@link Field} instance.
 *
 * @see ReflectiveFieldAccess
 * @see ReflectiveFieldWriter
 *
 * @param <Parent> The type of the class that the field belongs to
 * @param <FieldType> The type of the field
 *
 * @since 0.4.13
 *
 * @author Guus Lieben
 */
public class ReflectionFieldView<Parent, FieldType> extends ReflectionAnnotatedElementView implements FieldView<Parent, FieldType> {

    private final Field field;
    private final Introspector introspector;

    private ReflectiveFieldAccess<FieldType, Parent> getter;
    private ReflectiveFieldWriter<FieldType, Parent> setter;

    private TypeView<FieldType> type;
    private TypeView<FieldType> genericType;
    private TypeView<Parent> declaredBy;

    public ReflectionFieldView(ReflectionIntrospector introspector, Field field) {
        super(introspector);
        this.field = field;
        this.introspector = introspector;
        // Acceptable if the field is not accessible. If the field cannot be accessed, it is assumed this is valid
        // and the field will only be used for introspection purposes.
        field.trySetAccessible();
    }

    @Override
    public Option<Field> field() {
        return Option.of(this.field);
    }

    @Override
    public void set(Object instance, Object value) throws Throwable {
        if (value != null && !this.type().isInstance(value)) {
            throw new IllegalIntrospectionException(this, "Cannot set field " + this.field.getName() + " to value of type " + value.getClass().getName() + ", expected " + this.type().name());
        }

        if (this.setter == null) {
            Option<Property> property = this.annotations().get(Property.class);
            if (property.present() && !"".equals(property.get().setter())) {
                String setter = property.get().setter();
                Option<MethodView<Parent, ?>> method = this.declaredBy().methods().named(setter, List.of(this.type().type()));
                MethodView<Parent, ?> methodView = method.orElseThrow(() -> new IllegalIntrospectionException(this, "Setter for field '" + this.name() + "' (" + setter + ") does not exist!"));
                this.setter = (object, propertyValue) -> {
                    methodView.invoke(this.declaredBy().cast(instance), propertyValue).cast(this.type().type());
                };
            } else {
                this.setter = (object, propertyValue) -> {
                    try {
                        this.field.set(object, propertyValue);
                    }
                    catch (IllegalAccessException e) {
                        throw new IllegalIntrospectionException(this, e.getMessage());
                    }
                };
            }
        }
        this.setter.set(this.declaredBy().cast(instance), this.type().cast(value));
    }

    @Override
    public Option<FieldType> get(Object instance) throws Throwable {
        if (this.getter == null) {
            Option<Property> property = this.annotations().get(Property.class);
            if (property.present() && !"".equals(property.get().getter())) {
                String getter = property.get().getter();
                Option<MethodView<Parent, ?>> method = this.declaredBy().methods().named(getter);
                MethodView<Parent, ?> methodContext = method.orElseThrow(() -> new IllegalIntrospectionException(this, "Getter for field '" + this.name() + "' (" + getter + ") does not exist!"));
                this.getter = object -> methodContext.invoke(instance)
                        .map(result -> this.type().cast(result));
            } else {
                this.getter = object -> {
                    try {
                        return Option.of(this.type().cast(this.field.get(object)));
                    }
                    catch (IllegalAccessException e) {
                        throw new IllegalIntrospectionException(this, e.getMessage());
                    }
                };
            }
        }
        return this.getter.get(this.declaredBy().cast(instance)).orCompute(() -> this.type().defaultOrNull());
    }

    @Override
    public Option<FieldType> getStatic() throws Throwable {
        if (!this.modifiers().isStatic()) {
            throw new IllegalIntrospectionException(this, "Cannot get static value of non-static field");
        }
        return this.get(null);
    }

    @Override
    public String name() {
        return this.field.getName();
    }

    @Override
    public String qualifiedName() {
        return "%s#%s[%s]".formatted(this.declaredBy().qualifiedName(), this.name(), this.type().qualifiedName());
    }

    @Override
    public TypeView<FieldType> type() {
        if (this.type == null) {
            this.type = (TypeView<FieldType>) this.introspector.introspect(this.field.getType());
        }
        return this.type;
    }

    @Override
    public TypeView<FieldType> genericType() {
        if (this.genericType == null) {
            this.genericType = (TypeView<FieldType>) this.introspector.introspect(this.field.getGenericType());
        }
        return this.genericType;
    }

    @Override
    public TypeView<Parent> declaredBy() {
        if (this.declaredBy == null) {
            this.declaredBy = (TypeView<Parent>) this.introspector.introspect(this.field.getDeclaringClass());
        }
        return this.declaredBy;
    }

    @Override
    public ElementModifiersIntrospector modifiers() {
        return new ReflectionElementModifiersIntrospector(this.field);
    }

    @Override
    protected AnnotatedElement annotatedElement() {
        return this.field;
    }

    @Override
    public void report(DiagnosticsPropertyCollector collector) {
        collector.property("name").writeString(this.name());
        collector.property("elementType").writeString("field");
        collector.property("type").writeDelegate(this.genericType());
        collector.property("declaredBy").writeDelegate(this.declaredBy());
    }

    @Override
    public boolean isEnclosed() {
        return true;
    }

    @Override
    public Option<EnclosableView> enclosingView() {
        return Option.of(this.declaredBy());
    }
}
