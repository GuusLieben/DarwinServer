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
import org.dockbox.hartshorn.util.ObjectDescriber;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.view.EnclosableView;
import org.dockbox.hartshorn.util.introspect.view.TypeParameterView;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.introspect.view.wildcard.WildcardTypeView;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A view that provides access to a type parameter. This view is backed by a {@link Type}, which may be a {@link
 * TypeVariable}, {@link ParameterizedType}, {@link WildcardType} or {@link Class}. The view provides information about
 * the type parameter, such as its bounds, the type that it represents, and the type that it is declared by.
 *
 * @since 0.5.0
 *
 * @author Guus Lieben
 */
public class ReflectionTypeParameterView extends ReflectionAnnotatedElementView implements TypeParameterView {

    private final Type type;
    private final TypeView<?> consumedBy;
    private final int index;

    private Set<TypeParameterView> represents;
    private Set<TypeView<?>> upperBounds;
    private Option<TypeView<?>> resolvedType;
    private TypeView<?> declaredBy;

    public ReflectionTypeParameterView(Type type, TypeView<?> consumedBy, int index, Introspector introspector) {
        super(introspector);
        this.type = type;
        this.consumedBy = consumedBy;
        this.index = index;
    }

    @Override
    public int index() {
        return this.index;
    }

    @Override
    public boolean isInputParameter() {
        return this.declaredBy().is(this.consumedBy().type());
    }

    @Override
    public boolean isOutputParameter() {
        return !this.isInputParameter();
    }

    @Override
    public TypeView<?> declaredBy() {
        if (this.declaredBy == null) {
            if (this.type instanceof TypeVariable<?> typeVariable) {
                GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                if (genericDeclaration instanceof Class<?> clazz) {
                    this.declaredBy = this.introspector().introspect(clazz);
                }
                else {
                    throw new IllegalStateException("Generic declaration is not a class, cannot resolve declaring type");
                }
            }
            else {
                this.declaredBy = this.consumedBy();
            }
        }
        return this.declaredBy;
    }

    @Override
    public TypeView<?> consumedBy() {
        return this.consumedBy.rawType();
    }

    @Override
    public Option<TypeParameterView> definition() {
        if (this.isOutputParameter() && this.isVariable()) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) this.type;
            TypeVariable<? extends Class<?>>[] typeParameters = this.declaredBy.type().getTypeParameters();
            int index = -1;
            for (int i = 0; i < typeParameters.length; i++) {
                TypeVariable<? extends Class<?>> typeParameter = typeParameters[i];
                if (typeParameter == typeVariable) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                throw new IllegalStateException("Could not find type parameter " + typeVariable.getName() + " in " + this.declaredBy.name());
            }
            TypeParameterView view = new ReflectionTypeParameterView(typeVariable, this.declaredBy, index, this.introspector());
            return Option.of(view);
        }
        else {
            return Option.empty();
        }
    }

    @Override
    public Set<TypeParameterView> represents() {
        if (this.represents == null) {
            if (this.type instanceof TypeVariable<?> typeVariable) {
                TypeView<?> superClass = this.consumedBy.genericSuperClass();
                List<TypeView<?>> genericInterfaces = this.consumedBy.genericInterfaces();

                Set<TypeView<?>> representationCandidates = new HashSet<>();
                representationCandidates.add(superClass);
                representationCandidates.addAll(genericInterfaces);

                this.represents = representationCandidates.stream()
                        .flatMap(candidate -> this.getRepresentedParameters(typeVariable, candidate).stream())
                        .collect(Collectors.toSet());
            }
            else if (this.type instanceof Class<?>) {
                // A concrete type represents itself, as it is not provided by an input parameter. However, a
                // self-representing type is not a type parameter, so it represents nothing in this context.
                this.represents = Set.of();
            }
        }
        return this.represents;
    }

    private Set<TypeParameterView> getRepresentedParameters(TypeVariable<?> typeVariable, TypeView<?> typeView) {
        Set<TypeParameterView> representedParameters = new HashSet<>();
        for (TypeParameterView parameterView : typeView.typeParameters().allInput()) {
            if (parameterView instanceof ReflectionTypeParameterView reflectionTypeParameterView) {
                if (reflectionTypeParameterView.type == typeVariable) {
                    representedParameters.add(reflectionTypeParameterView);
                }
            }
        }
        return representedParameters;
    }

    @Override
    public Set<TypeView<?>> upperBounds() {
        if (this.upperBounds == null) {
            this.upperBounds = switch (this.type) {
                case TypeVariable<?> typeVariable -> Arrays.stream(typeVariable.getBounds())
                        .map(this.introspector()::introspect)
                        .collect(Collectors.toSet());
                case WildcardType wildcardType -> Arrays.stream(wildcardType.getUpperBounds())
                        .map(this.introspector()::introspect)
                        .collect(Collectors.toSet());
                case null, default -> Set.of();
            };
        }
        return this.upperBounds;
    }

    @Override
    public Option<TypeView<?>> resolvedType() {
        if (this.resolvedType == null) {
            this.resolvedType = switch (this.type) {
                case Class<?> clazz -> Option.of(this.introspector().introspect(clazz));
                case ParameterizedType parameterizedType -> Option.of(this.introspector().introspect(parameterizedType));
                // Note that upper bounds may be present, but the resolved type itself is still a wildcard,
                // so we return a wildcard type view here. The upper bounds can be resolved separately if
                // needed.
                case WildcardType ignored -> Option.of(new WildcardTypeView());
                case null, default -> Option.empty();
            };
        }
        return this.resolvedType;
    }

    @Override
    public boolean isBounded() {
        return switch (this.type) {
            case TypeVariable<?> typeVariable -> typeVariable.getBounds().length > 0;
            case WildcardType wildcardType -> wildcardType.getUpperBounds().length > 0;
            case null, default -> false;
        };
    }

    @Override
    public boolean isUnbounded() {
        return !this.isBounded();
    }

    @Override
    public boolean isClass() {
        return this.resolvedType().filter(TypeView::isWildcard).present();
    }

    @Override
    public boolean isInterface() {
        return this.resolvedType().map(TypeView::isInterface).orElse(false);
    }

    @Override
    public boolean isEnum() {
        return this.resolvedType().map(TypeView::isEnum).orElse(false);
    }

    @Override
    public boolean isAnnotation() {
        return this.resolvedType().map(TypeView::isAnnotation).orElse(false);
    }

    @Override
    public boolean isRecord() {
        return this.resolvedType().map(TypeView::isRecord).orElse(false);
    }

    @Override
    public boolean isVariable() {
        return this.type instanceof TypeVariable<?>;
    }

    @Override
    public boolean isWildcard() {
        return this.type instanceof WildcardType;
    }

    @Override
    public TypeParameterView asInputParameter() {
        if (this.isInputParameter()) {
            return this;
        }
        else {
            return this.consumedBy().typeParameters().atIndex(this.index).orNull();
        }
    }

    @Override
    public String qualifiedName() {
        return this.type.getTypeName();
    }

    @Override
    public void report(DiagnosticsPropertyCollector collector) {
        collector.property("name").writeString(this.name());
        collector.property("index").writeInt(this.index());
        collector.property("declaredBy").writeString(this.declaredBy().name());
        collector.property("consumedBy").writeString(this.consumedBy().name());
        collector.property("type").writeString(this.isInputParameter() ? "input" : "output");
    }

    @Override
    public String name() {
        return this.type.getTypeName();
    }

    @Override
    public String toString() {
        return ObjectDescriber.of(this)
                .field("name", this.name())
                .field("declaredBy", this.declaredBy())
                .field("consumedBy", this.consumedBy())
                .describe();
    }

    @Override
    protected AnnotatedElement annotatedElement() {
        return this.type instanceof AnnotatedElement annotatedElement ? annotatedElement : null;
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
