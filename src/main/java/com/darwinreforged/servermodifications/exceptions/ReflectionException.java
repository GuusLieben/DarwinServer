package com.darwinreforged.servermodifications.exceptions;

public class ReflectionException
        extends Exception {
    private static final long serialVersionUID = 5017357956938150182L;

    public ReflectionException() {
        this("Unknow");
    }

    public ReflectionException(String message) {
        super("ReflectionException caused by ModBanner: " + message);
    }
}
