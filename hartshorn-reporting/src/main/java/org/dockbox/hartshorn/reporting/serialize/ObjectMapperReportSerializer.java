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

package org.dockbox.hartshorn.reporting.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.dockbox.hartshorn.reporting.collect.ArrayNode;
import org.dockbox.hartshorn.reporting.collect.DiagnosticsReport;
import org.dockbox.hartshorn.reporting.collect.GroupNode;
import org.dockbox.hartshorn.reporting.collect.Node;
import org.dockbox.hartshorn.reporting.collect.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class ObjectMapperReportSerializer implements ReportSerializer<String> {

    @Override
    public String serialize(final DiagnosticsReport report) {
        try {
            final JsonNode node = new JsonNodeVisitor().visit(report.root());
            return this.objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (final JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected abstract ObjectMapper objectMapper();

    private static class JsonNodeVisitor implements NodeVisitor<JsonNode> {

        @Override
        public JsonNode visit(final Node<?> node) {
            final JsonNodeFactory factory = JsonNodeFactory.instance;
            final Object value = node.value();
            if (value instanceof String string) return factory.textNode(string);
            else if (value instanceof Integer integer) return factory.numberNode(integer);
            else if (value instanceof Double dbl) return factory.numberNode(dbl);
            else if (value instanceof Long lng) return factory.numberNode(lng);
            else if (value instanceof Short shrt) return factory.numberNode(shrt);
            else if (value instanceof Boolean bool) return factory.booleanNode(bool);
            else if (value instanceof Node<?> n) return n.accept(this);
            else throw new IllegalArgumentException("Unsupported type " + value.getClass().getName());
        }

        @Override
        public JsonNode visit(final GroupNode node) {
            final JsonNodeFactory factory = JsonNodeFactory.instance;
            final ObjectNode object = factory.objectNode();
            for (final Node<?> value : node.value()) {
                object.set(value.name(), value.accept(this));
            }
            return object;
        }

        @Override
        public JsonNode visit(final ArrayNode<?> node) {
            final JsonNodeFactory factory = JsonNodeFactory.instance;
            final List<JsonNode> nodes = new ArrayList<>();
            for (final Object value : node.value()) {
                if (value instanceof String string) nodes.add(factory.textNode(string));
                else if (value instanceof Integer integer) nodes.add(factory.numberNode(integer));
                else if (value instanceof Double dbl) nodes.add(factory.numberNode(dbl));
                else if (value instanceof Long lng) nodes.add(factory.numberNode(lng));
                else if (value instanceof Short shrt) nodes.add(factory.numberNode(shrt));
                else if (value instanceof Boolean bool) nodes.add(factory.booleanNode(bool));
                else if (value instanceof Node<?> n) nodes.add(n.accept(this));
                else throw new IllegalArgumentException("Unsupported type " + value.getClass().getName());
            }
            return factory.arrayNode().addAll(nodes);
        }
    }

    public static class XMLReportSerializer extends ObjectMapperReportSerializer {

        @Override
        protected ObjectMapper objectMapper() {
            return new XmlMapper();
        }
    }

    public static class JsonReportSerializer extends ObjectMapperReportSerializer {

        @Override
        protected ObjectMapper objectMapper() {
            return new JsonMapper();
        }
    }

}
