package com.webselenium.base;

import org.openqa.selenium.By;

public final class DynamicLocator {

    public enum Type { XPATH, CSS }

    private final Type type;
    private final String template;

    private DynamicLocator(Type type, String template) {
        this.type = type;
        this.template = template;
    }

    public static DynamicLocator xpath(String template) {
        return new DynamicLocator(Type.XPATH, template);
    }

    public static DynamicLocator css(String template) {
        return new DynamicLocator(Type.CSS, template);
    }

    public By format(Object... args) {
        String value = (args == null || args.length == 0)
                ? template
                : String.format(template, args);
        return type == Type.XPATH ? By.xpath(value) : By.cssSelector(value);
    }

    @Override
    public String toString() {
        return "DynamicLocator{" + type + ", " + template + "}";
    }
}