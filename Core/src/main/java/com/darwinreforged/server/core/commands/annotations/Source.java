package com.darwinreforged.server.core.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 The annotation used to mark method parameters as Source types. If
 present a {@link com.darwinreforged.server.core.commands.CommandBus}
 instance will attempt to inject values to non-default parameters.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Source {
}
