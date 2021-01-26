package org.dockbox.selene.palswap;

import com.google.inject.Inject;

import org.dockbox.selene.core.annotations.command.Command;
import org.dockbox.selene.core.annotations.extension.Extension;
import org.dockbox.selene.core.command.context.CommandContext;
import org.dockbox.selene.core.command.context.CommandValue.Argument;
import org.dockbox.selene.core.i18n.common.Language;
import org.dockbox.selene.core.objects.Exceptional;
import org.dockbox.selene.core.objects.item.Item;
import org.dockbox.selene.core.objects.player.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Extension(id = "blockregistry", name = "Block Registry",
           description = "Block Registry Testing",
           authors = "pumbas600")
public class BlockRegistryTestExtension {

    private static BlockRegistryTestExtension instance;

    @Inject
    public Logger logger;

    public BlockRegistryTestExtension() {
        instance = this;
    }

    @Command(aliases = "blockid", usage = "blockid <id>")
    public void blockID(@NotNull Player player, CommandContext context) {
        Exceptional<Argument<String>> eID = context.getArgument("id");

        if (eID.isAbsent()) return;

        String id = eID.get().getValue();

        Item item = Item.of(id);
        this.logger.info(item.getDisplayName().toStringValue());

        player.getInventory().give(Item.of(id));
    }

    @Command(aliases = "blockmeta", usage = "blockmeta <id>{String} <meta>{Integer}")
    public void blockMeta(@NotNull Player player, CommandContext context) {

        String id = context.getArgument("id").get().getValue();
        int meta = context.getArgument("meta", Integer.class).get().getValue();

        Item item = Item.of(id, meta);
        this.logger.info("Is block: " + item.isBlock());
        this.logger.info(item.getDisplayName(Language.EN_US).toStringValue());
        this.logger.info(item.getId());

        player.getInventory().give(Item.of(id));
    }
}