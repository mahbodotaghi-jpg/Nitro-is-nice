package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 10, 1));
    private int timer;

    public AutoTotem() { super("AutoTotem", "Keeps a totem in your offhand.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { timer = 0; return; }
        if (timer++ < delay.get()) return;

        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? i + 36 : i;
                mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
                timer = 0;
                return;
            }
        }
    }
}
