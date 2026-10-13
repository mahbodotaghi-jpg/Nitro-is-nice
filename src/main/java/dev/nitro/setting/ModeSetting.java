package dev.nitro.setting;

public class ModeSetting extends Setting<String> {
    private final String[] modes;
    private int idx;

    public ModeSetting(String name, String... modes) {
        super(name, modes[0]);
        this.modes = modes;
    }

    public void cycle() {
        idx = (idx + 1) % modes.length;
        value = modes[idx];
    }
}
