/*
 * Copyright 2019-2022 the original author or authors.
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

package org.dockbox.hartshorn.data.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.dockbox.hartshorn.util.Property;
import org.dockbox.hartshorn.util.introspect.ElementAnnotationsIntrospector;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.option.Option;

import java.lang.reflect.AnnotatedElement;
import java.util.function.Function;

public class JacksonPropertyAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private final Introspector introspector;

    public JacksonPropertyAnnotationIntrospector(Introspector introspector) {
        this.introspector = introspector;
    }

    @Override
    public JavaType refineSerializationType(final MapperConfig<?> config, final Annotated a, final JavaType baseType) throws JsonMappingException {
        if (a.hasAnnotation(Property.class)) {
            final Property property = a.getAnnotation(Property.class);
            final TypeFactory typeFactory = config.getTypeFactory();
            JavaType type = baseType;

            // Property type
            final Class<?> valueClass = this._classIfExplicit(property.type());
            if (valueClass != null) {
                if (type.hasRawClass(valueClass)) type = type.withStaticTyping();
                else type = this.refineAssignableType(typeFactory, type, valueClass);
            }

            // Key type - for container-like types
            if (type.isMapLikeType()) {
                JavaType keyType = type.getKeyType();
                final Class<?> keyClass = this._classIfExplicit(property.key());
                if (keyClass != null) {
                    if (keyType.hasRawClass(keyClass)) type = type.withStaticTyping();
                    else keyType = this.refineAssignableType(typeFactory, keyType, keyClass);
                    type = ((MapLikeType) type).withKeyType(keyType);
                }
            }

            // Content type - for container-like types
            JavaType contentType = type.getContentType();
            if (contentType != null) {
                final Class<?> contentClass = this._classIfExplicit(property.content());
                if (contentClass != null) {
                    if (contentType.hasRawClass(contentClass)) contentType = contentType.withStaticTyping();
                    else contentType = this.refineAssignableType(typeFactory, contentType, contentClass);
                    type = type.withContentType(contentType);
                }
            }

            return type;
        }
        return super.refineSerializationType(config, a, baseType);
    }

    private JavaType refineAssignableType(final TypeFactory typeFactory, final JavaType javaType, final Class<?> type) throws JacksonIntrospectionException {
        final Class<?> rawClass = javaType.getRawClass();
        if (type.isAssignableFrom(rawClass)) return typeFactory.constructGeneralizedType(javaType, type);
        else if (rawClass.isAssignableFrom(type)) return typeFactory.constructSpecializedType(javaType, type);
        else throw new JacksonIntrospectionException(null, "Cannot refine property content type from " + rawClass.getName() + " to " + type.getName());
    }

    @Override
    public JavaType refineDeserializationType(final MapperConfig<?> config, final Annotated a, final JavaType baseType) throws JsonMappingException {
        if (a.hasAnnotation(Property.class)) {
            final Property property = a.getAnnotation(Property.class);
            final TypeFactory typeFactory = config.getTypeFactory();
            JavaType type = baseType;

            // Property type
            final Class<?> valueClass = this._classIfExplicit(property.type());
            if (valueClass != null && !type.hasRawClass(valueClass)) {
                type = typeFactory.constructSpecializedType(type, valueClass);
            }

            // Key type - for container-like types
            if (type.isMapLikeType()) {
                final JavaType keyType = type.getKeyType();
                // Filters void types, including default used in RefineProperty
                final Class<?> keyClass = this._classIfExplicit(property.key());
                if (keyClass != null) {
                    final JavaType specializedType = typeFactory.constructSpecializedType(keyType, keyClass);
                    type = ((MapLikeType) type).withKeyType(specializedType);
                }
            }

            // Content type - for container-like types
            final JavaType contentType = type.getContentType();
            if (contentType != null) {
                // Filters void types, including default used in RefineProperty
                final Class<?> contentClass = this._classIfExplicit(property.content());
                if (contentClass != null) {
                    final JavaType specializedType = typeFactory.constructSpecializedType(contentType, contentClass);
                    type = type.withContentType(specializedType);
                }
            }

            return type;
        }
        return super.refineDeserializationType(config, a, baseType);
    }

    @Override
    public PropertyName findNameForSerialization(final Annotated a) {
        return this.findName(a, super::findNameForSerialization);
    }

    @Override
    public PropertyName findNameForDeserialization(final Annotated a) {
        return this.findName(a, super::findNameForDeserialization);
    }

    private PropertyName findName(final Annotated a, final Function<Annotated, PropertyName> defaultValue) {
        final AnnotatedElement annotated = a.getAnnotated();
        if (annotated != null) {
            final ElementAnnotationsIntrospector introspector = this.introspector.introspect(annotated);
            final Option<Property> annotation = introspector.get(Property.class);
            if (annotation.present()) {
                return new PropertyName(annotation.get().name());
            }
        }
        return defaultValue.apply(a);
    }
}
