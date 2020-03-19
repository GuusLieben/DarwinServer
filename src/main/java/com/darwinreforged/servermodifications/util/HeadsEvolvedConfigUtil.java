package com.darwinreforged.servermodifications.util;

import com.google.gson.Gson;
import org.spongepowered.api.item.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class HeadsEvolvedConfigUtil {

  private ItemStack[] storedJson;

  public HeadsEvolvedConfigUtil(File file) {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      FileReader reader = new FileReader(file);
      this.storedJson = new Gson().fromJson(reader, ItemStack[].class);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ItemStack[] getCustomHeads() {
    return this.storedJson;
  }
}
