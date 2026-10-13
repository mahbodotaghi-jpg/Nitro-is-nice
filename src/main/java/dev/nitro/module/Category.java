package dev.nitro.module;

public enum Category {
    COMBAT("Combat"), RENDER("Render"), MISC("Misc");

    public final String display;
    Category(String display) { this.display = display; }
}
