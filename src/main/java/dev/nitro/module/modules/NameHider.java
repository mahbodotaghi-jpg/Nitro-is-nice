package dev.nitro.module.modules;

import dev.nitro.NitroClient;
import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

/** Replaces your username everywhere text is drawn (chat, tab list, name tags). Hooked via TextVisitFactoryMixin. */
public class NameHider extends Module {
    private final ModeSetting name = add(new ModeSetting("Name", "Nitro", "Steve", "Player", "Anonymous"));

    public NameHider() { super("NameHider", "Replaces your username in chat, tab list and name tags.", Category.MISC); }

    public static String process(String text) {
        if (text == null || text.isEmpty() || NitroClient.modules == null) return text;
        NameHider m = NitroClient.modules.get(NameHider.class);
        if (m == null || !m.isEnabled()) return text;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSession() == null) return text;
        String me = mc.getSession().getUsername();
        if (me == null || me.isEmpty() || !text.contains(me)) return text;
        return text.replace(me, m.name.get());
    }
}
