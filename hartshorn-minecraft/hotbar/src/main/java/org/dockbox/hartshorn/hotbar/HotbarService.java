package org.dockbox.hartshorn.hotbar;

import org.dockbox.hartshorn.api.domain.tuple.Tuple;
import org.dockbox.hartshorn.cache.annotations.Cached;
import org.dockbox.hartshorn.cache.annotations.UpdateCache;
import org.dockbox.hartshorn.commands.RunCommandAction;
import org.dockbox.hartshorn.commands.annotations.Command;
import org.dockbox.hartshorn.commands.context.CommandContext;
import org.dockbox.hartshorn.di.properties.AttributeHolder;
import org.dockbox.hartshorn.i18n.text.Text;
import org.dockbox.hartshorn.i18n.text.actions.HoverAction;
import org.dockbox.hartshorn.server.minecraft.inventory.Element;
import org.dockbox.hartshorn.server.minecraft.inventory.InventoryLayout;
import org.dockbox.hartshorn.server.minecraft.inventory.InventoryType;
import org.dockbox.hartshorn.server.minecraft.players.Player;
import org.dockbox.hartshorn.server.minecraft.players.inventory.InventoryRow;
import org.dockbox.hartshorn.server.minecraft.players.inventory.PlayerInventory;
import org.dockbox.hartshorn.util.HartshornUtils;

import java.util.List;

@Command("hotbar")
public abstract class HotbarService implements AttributeHolder {

    @Override
    public void enable() {
        // Initialize cache
        this.cache();
    }

    @Command(value = "share", arguments = "<with{Player}>")
    public void share(final Player source, final CommandContext context, final Player with) {
        final PlayerInventory inventory = source.inventory();
        final InventoryRow hotbar = inventory.hotbar();

        final long id = System.nanoTime();
        this.save(Tuple.of(id, hotbar));

        // TODO: Resources
        final Text shared = Text.of("$1%s $2shared their hotbar ".formatted(source.displayName()));

        final Text view = Text.of("$1[$2View$1]")
                .onClick(RunCommandAction.runCommand("/hotbar view %d".formatted(id)))
                .onHover(HoverAction.showText(Text.of("$1View hotbar")));

        final Text load = Text.of("$1[$2Load$1]")
                .onClick(RunCommandAction.runCommand("/hotbar load %s".formatted(id)))
                .onHover(HoverAction.showText(Text.of("$1Load hotbar")));

        final Text message = Text.of(shared, view, load);
        with.send(message);
    }

    @Command(value = "view", arguments = "<id{Long}>")
    public void view(final Player source, final CommandContext context, final long id) {
        for (final Tuple<Long, InventoryRow> entry : this.cache()) {
            if (entry.key() == id) {
                InventoryLayout.builder(InventoryType.DROPPER)
                        .addElements(entry.value().items()
                                .stream().map(item -> Element.of(item, ctx -> ctx.player().inventory().give(item)))
                                .toList()
                        ).toStaticPaneBuilder()
                        .title(Text.of("$1" + source.name() + "$2's hotbar"))
                        .build()
                        .open(source);
            }
        }
    }

    @Cached
    public List<Tuple<Long, InventoryRow>> cache() {
        return HartshornUtils.emptyConcurrentList();
    }

    @UpdateCache
    public abstract void save(Tuple<Long, InventoryRow> entry);


}
