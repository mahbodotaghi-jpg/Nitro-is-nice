package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.CombatUtil;
import dev.nitro.util.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CrystalAura extends Module {
    private final BooleanSetting place = add(new BooleanSetting("Place", true));
    private final BooleanSetting breakC = add(new BooleanSetting("Break", true));
    private final BooleanSetting autoSwitch = add(new BooleanSetting("Auto switch", true));
    private final NumberSetting placeRange = add(new NumberSetting("Place range", 4.5, 2, 6, 0.1));
    private final NumberSetting breakRange = add(new NumberSetting("Break range", 4.5, 2, 6, 0.1));
    private final NumberSetting minDmg = add(new NumberSetting("Min damage", 5, 1, 20, 0.5));
    private final NumberSetting maxSelf = add(new NumberSetting("Max self damage", 6, 1, 20, 0.5));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private int placeTimer, breakTimer;

    public CrystalAura() { super("CrystalAura", "Places and breaks end crystals near the closest player.", Category.COMBAT); }

    @Override
    public void onEnable() { placeTimer = breakTimer = 0; }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        PlayerEntity target = findTarget();
        if (target == null) return;

        placeTimer++; breakTimer++;
        if (breakC.get() && breakTimer >= delay.get() && doBreak(target)) breakTimer = 0;
        if (place.get() && placeTimer >= delay.get() && doPlace(target)) placeTimer = 0;
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bd = 12;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            double d = mc.player.distanceTo(p);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    private boolean safe(float selfDmg) {
        return selfDmg <= maxSelf.get() && selfDmg < CombatUtil.health(mc.player) - 1f;
    }

    private boolean doBreak(PlayerEntity target) {
        Vec3d eye = mc.player.getEyePos();
        Box area = mc.player.getBoundingBox().expand(breakRange.get() + 1);
        EndCrystalEntity best = null;
        float bestScore = -1000f;

        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class, area, e -> e.isAlive())) {
            if (eye.distanceTo(c.getPos()) > breakRange.get()) continue;
            float self = CombatUtil.crystalDamage(c.getPos(), mc.player);
            if (!safe(self)) continue;
            float dmg = CombatUtil.crystalDamage(c.getPos(), target);
            if (dmg < minDmg.get()) continue;
            if (dmg - self > bestScore) { bestScore = dmg - self; best = c; }
        }
        if (best == null) return false;

        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean doPlace(PlayerEntity target) {
        Vec3d eye = mc.player.getEyePos();
        BlockPos base = mc.player.getBlockPos();
        int r = (int) Math.ceil(placeRange.get());
        BlockPos best = null;
        float bestScore = -1000f;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = base.add(x, y, z);
                    BlockState st = mc.world.getBlockState(p);
                    if (!st.isOf(Blocks.OBSIDIAN) && !st.isOf(Blocks.BEDROCK)) continue;
                    if (!mc.world.getBlockState(p.up()).isAir() || !mc.world.getBlockState(p.up(2)).isAir()) continue;
                    if (eye.distanceTo(new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) > placeRange.get()) continue;

                    Box space = new Box(p.getX(), p.getY() + 1, p.getZ(), p.getX() + 1, p.getY() + 3, p.getZ() + 1);
                    if (!mc.world.getOtherEntities(null, space, e -> !(e instanceof ItemEntity)).isEmpty()) continue;

                    Vec3d center = new Vec3d(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5);
                    float dmg = CombatUtil.crystalDamage(center, target);
                    if (dmg < minDmg.get()) continue;
                    float self = CombatUtil.crystalDamage(center, mc.player);
                    if (!safe(self)) continue;
                    if (dmg - self > bestScore) { bestScore = dmg - self; best = p; }
                }
            }
        }
        if (best == null) return false;

        Hand hand;
        if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.OFF_HAND;
        else if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.MAIN_HAND;
        else {
            if (!autoSwitch.get()) return false;
            int slot = InvUtil.findHotbar(Items.END_CRYSTAL);
            if (slot < 0) return false;
            InvUtil.selectSlot(slot);
            hand = Hand.MAIN_HAND;
        }

        BlockHitResult hit = new BlockHitResult(
                new Vec3d(best.getX() + 0.5, best.getY() + 1.0, best.getZ() + 0.5), Direction.UP, best, false);
        mc.interactionManager.interactBlock(mc.player, hand, hit);
        mc.player.swingHand(hand);
        return true;
    }
}
