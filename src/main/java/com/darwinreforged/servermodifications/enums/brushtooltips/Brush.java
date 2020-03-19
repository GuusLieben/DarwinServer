package com.darwinreforged.servermodifications.enums.brushtooltips;

import java.util.*;

public class Brush {

  private String displayName;
  private List<Flag> flags;
  private List<Argument> arguments;

  public Brush(String displayName, Prototype... prototypes) {
    flags = new ArrayList<>();
    arguments = new ArrayList<>();

    Arrays.stream(prototypes)
        .forEach(
            (Prototype prototype) -> {
              if (prototype instanceof Argument) arguments.add((Argument) prototype);
              else if (prototype instanceof Flag) flags.add((Flag) prototype);
            });
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public List<Flag> getFlags() {
    return flags;
  }

  public List<Argument> getArguments() {
    return arguments;
  }
}
