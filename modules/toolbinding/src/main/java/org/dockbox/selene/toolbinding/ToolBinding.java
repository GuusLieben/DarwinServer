/*
 * Copyright (C) 2020 Guus Lieben
 *
 * This framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.selene.toolbinding;

import org.dockbox.selene.api.annotations.event.Listener;
import org.dockbox.selene.api.annotations.i18n.Resources;
import org.dockbox.selene.api.annotations.module.Module;
import org.dockbox.selene.api.events.player.interact.PlayerInteractEvent;
import org.dockbox.selene.api.i18n.common.ResourceEntry;
import org.dockbox.selene.api.i18n.entry.Resource;
import org.dockbox.selene.api.objects.Exceptional;
import org.dockbox.selene.api.objects.item.Item;
import org.dockbox.selene.api.objects.keys.Keys;
import org.dockbox.selene.api.objects.keys.PersistentDataKey;
import org.dockbox.selene.api.objects.keys.RemovableKey;
import org.dockbox.selene.api.objects.keys.TransactionResult;
import org.dockbox.selene.api.objects.player.Sneaking;
import org.dockbox.selene.api.server.Selene;
import org.dockbox.selene.api.util.SeleneUtils;

import java.util.Map;
import java.util.UUID;

@Module(
        id = "toolbinding",
        name = "Tool Binding",
        description = "Adds the ability to bind commands to tools and items",
        authors = "GuusLieben")
@Resources(module = ToolBinding.class)
public class ToolBinding {

    // TODO: Continue

    private static final PersistentDataKey<String> PERSISTENT_TOOL = Keys.persistent(String.class, "Tool Binding", ToolBinding.class);
    private static final ResourceEntry TOOL_ERROR_BLOCK = new Resource("Tool cannot be bound to blocks", "toolbinding.caught.block");
    private static final ResourceEntry TOOL_ERROR_HAND = new Resource("Tool cannot be bound to hand", "toolbinding.caught.hand");
    private static final ResourceEntry TOOL_ERROR_DUPLICATE = new Resource("There is already a tool bound to this item", "toolbinding.caught.duplicate");

    private static ToolBinding instance;

    public static final RemovableKey<Item, ItemTool> TOOL = Keys.removable(
            // Not possible to use method references here due to instance being initialized later
            (item, tool) -> instance.setTool(item, tool),
            item -> instance.getTool(item),
            item -> instance.removeTool(item));
    private final Map<String, ItemTool> registry = SeleneUtils.emptyConcurrentMap();

    public ToolBinding() {
        instance = this;
    }

    private TransactionResult setTool(Item item, ItemTool tool) {
        if (item.isBlock()) return TransactionResult.fail(TOOL_ERROR_BLOCK);
        if (item == Selene.getItems().getAir()) return TransactionResult.fail(TOOL_ERROR_HAND);
        if (item.get(PERSISTENT_TOOL).present()) return TransactionResult.fail(TOOL_ERROR_DUPLICATE);

        String bindingId = UUID.randomUUID().toString();

        TransactionResult result = item.set(PERSISTENT_TOOL, bindingId);
        if (result.isSuccessfull()) {
            this.registry.put(bindingId, tool);
            tool.prepare(item);
        }

        return result;
    }

    private void removeTool(Item item) {
        Exceptional<String> identifier = item.get(PERSISTENT_TOOL);
        if (identifier.absent()) return;

        Exceptional<ItemTool> tool = this.getTool(item);
        if (tool.absent()) return;

        item.remove(PERSISTENT_TOOL);
        this.registry.remove(identifier.get());
        ItemTool.reset(item);
    }

    private Exceptional<ItemTool> getTool(Item item) {
        Exceptional<String> identifier = item.get(PERSISTENT_TOOL);
        if (identifier.absent()) return Exceptional.none();

        String registryIdentifier = identifier.get();
        if (!this.registry.containsKey(registryIdentifier)) return Exceptional.none();
        ItemTool itemTool = this.registry.get(registryIdentifier);

        return Exceptional.of(itemTool);
    }

    @Listener
    public void onPlayerInteracted(PlayerInteractEvent event) {
        Item itemInHand = event.getTarget().getItemInHand(event.getHand());
        if (itemInHand.equals(Selene.getItems().getAir()) || itemInHand.isBlock()) return;

        Exceptional<String> identifier = itemInHand.get(PERSISTENT_TOOL);
        if (identifier.absent()) return;
        if (!this.registry.containsKey(identifier.get())) return;
        ItemTool tool = this.registry.get(identifier.get());

        ToolInteractionEvent toolInteractionEvent = new ToolInteractionEvent(
                event.getTarget(),
                itemInHand,
                tool,
                event.getHand(),
                event.getClientClickType(),
                event.getTarget().isSneaking() ? Sneaking.SNEAKING : Sneaking.STANDING);

        if (tool.accepts(toolInteractionEvent)) {
            toolInteractionEvent.post();
            if (toolInteractionEvent.isCancelled()) return;
            tool.perform(event.getTarget(), itemInHand);
            event.setCancelled(true); // To prevent block/entity damage
        }
    }
}
