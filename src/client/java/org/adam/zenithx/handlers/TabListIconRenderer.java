package org.adam.zenithx.handlers;

public class TabListIconRenderer {
    private static Object currentContext;

    public static void setContext(Object ctx) {
        currentContext = ctx;
    }

    public static Object getContext() {
        return currentContext;
    }
}