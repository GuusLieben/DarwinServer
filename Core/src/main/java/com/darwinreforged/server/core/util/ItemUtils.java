package com.darwinreforged.server.core.util;

import com.darwinreforged.server.core.player.inventory.DarwinItem;
import com.darwinreforged.server.core.internal.Utility;

/**
 The type Item utils.

 @param <I>
 the type parameter
 */
@Utility("ItemStack manipulation utilities")
public interface ItemUtils<I> {

    /**
     Sets display name.

     @param displayName
     the display name
     @param in
     the in

     @return the display name
     */
    I setDisplayName(String displayName, DarwinItem<I> in);

    /**
     Sets lore.

     @param lore
     the lore
     @param in
     the in

     @return the lore
     */
    I setLore(String[] lore, DarwinItem<I> in);

    /**
     Gets display name.

     @param in
     the in

     @return the display name
     */
    String getDisplayName(DarwinItem<I> in);

}
