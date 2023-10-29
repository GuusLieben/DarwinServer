package org.dockbox.hartshorn.application.context.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.application.context.DependencyGraph;
import org.dockbox.hartshorn.component.ComponentKey;
import org.dockbox.hartshorn.inject.ComponentDiscoveryList;
import org.dockbox.hartshorn.inject.CyclicComponentException;
import org.dockbox.hartshorn.inject.DependencyContext;
import org.dockbox.hartshorn.inject.TypePathNode;
import org.dockbox.hartshorn.util.ApplicationException;
import org.dockbox.hartshorn.util.graph.ContainableGraphNode;
import org.dockbox.hartshorn.util.graph.GraphNode;
import org.dockbox.hartshorn.util.introspect.Introspector;
import org.dockbox.hartshorn.util.introspect.view.TypeView;
import org.dockbox.hartshorn.util.introspect.view.View;

public class CyclicDependencyGraphValidator implements DependencyGraphValidator {

    @Override
    public void validateBeforeConfiguration(DependencyGraph dependencyGraph, ApplicationContext applicationContext) throws ApplicationException {
        Set<GraphNode<DependencyContext<?>>> nodes = dependencyGraph.nodes();
        for (GraphNode<DependencyContext<?>> node : nodes) {
            if (node.isLeaf()) {
                continue;
            }
            if (node instanceof ContainableGraphNode<DependencyContext<?>> contextContainableGraphNode
                && contextContainableGraphNode.isRoot()) {
                continue;
            }
            List<GraphNode<DependencyContext<?>>> graphNodes = this.checkNodeNotCyclicRecursive(node, new ArrayList<>());
            if (!graphNodes.isEmpty()) {
                ComponentDiscoveryList discoveryList = this.createDiscoveryList(graphNodes, applicationContext);
                throw new CyclicComponentException(discoveryList, node.value().origin());
            }
        }
    }

    public List<GraphNode<DependencyContext<?>>> checkNodeNotCyclicRecursive(GraphNode<DependencyContext<?>> node, List<GraphNode<DependencyContext<?>>> knownNodes) {
        if (knownNodes.contains(node)) {
            return List.of(node);
        }

        // Defaults to true, as we should assume that the node needs immediate resolution unless proven otherwise.
        boolean needsImmediateResolution = true;
        if (node instanceof ContainableGraphNode<DependencyContext<?>> containableGraphNode) {
            ComponentKey<?> dependencyCandidate = node.value().componentKey();
            // If none of the parents need immediate resolution, then we can cut potential cyclic graphs short.
            needsImmediateResolution = containableGraphNode.children().stream()
                    .anyMatch(parent -> parent.value().needsImmediateResolution(dependencyCandidate));
        }

        // If the node doesn't need immediate resolution, then we can skip it. Note that this does not affect potential grandchild
        // dependencies, as this validator goes over all nodes in the graph, and not just the roots.
        if (!needsImmediateResolution) {
            return List.of();
        }

        knownNodes.add(node);

        for(GraphNode<DependencyContext<?>> child : node.children()) {
            List<GraphNode<DependencyContext<?>>> graphNodes = this.checkNodeNotCyclicRecursive(child, knownNodes);
            if(!graphNodes.isEmpty()) {
                List<GraphNode<DependencyContext<?>>> path = new ArrayList<>();
                path.add(node);
                path.addAll(graphNodes);
                return path;
            }
        }
        return List.of();
    }

    public ComponentDiscoveryList createDiscoveryList(List<GraphNode<DependencyContext<?>>> path, ApplicationContext applicationContext) {
        ComponentDiscoveryList discoveryList = new ComponentDiscoveryList();
        Introspector introspector = applicationContext.environment().introspector();
        for (GraphNode<DependencyContext<?>> node : path) {
            TypePathNode<?> pathNode = this.createTypePathNode(node.value().componentKey(), node.value().origin(), introspector);
            discoveryList.add(pathNode);
        }
        return discoveryList;
    }

    private <T> TypePathNode<T> createTypePathNode(ComponentKey<T> componentKey, View origin, Introspector introspector) {
        TypeView<T> view = introspector.introspect(componentKey.type());
        return new TypePathNode<>(view, componentKey, origin);
    }
}
