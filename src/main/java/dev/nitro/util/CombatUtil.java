package dev.nitro.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Approximate end crystal damage (ignores enchantments/effects, so treat as an estimate). */
public class CombatUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static float exposure(Vec3d src, LivingEntity e) {
        Box b = e.getBoundingBox();
        Vec3d[] pts = {
            new Vec3d(e.getX(), b.minY + 0.1, e.getZ()),
            new Vec3d(e.getX(), (b.minY + b.maxY) / 2.0, e.getZ()),
            new Vec3d(e.getX(), b.maxY - 0.1, e.getZ())
        };
        int seen = 0;
        for (Vec3d p : pts) {
            var hit = mc.world.raycast(new RaycastContext(src, p,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() == HitResult.Type.MISS) seen++;
        }
        return seen / 3f;
    }

    public static float crystalDamage(Vec3d crystal, LivingEntity e) {
        double dist = e.getPos().distanceTo(crystal);
        if (dist > 12) return 0f;
        double impact = (1.0 - dist / 12.0) * exposure(crystal, e);
        float dmg = (float) ((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);

        switch (mc.world.getDifficulty()) {
            case PEACEFUL -> { return 0f; }
            case EASY -> dmg = Math.min(dmg / 2f + 1f, dmg);
            case HARD -> dmg *= 1.5f;
            default -> {}
        }

        float armor = e.getArmor();
        float g = MathHelper.clamp(armor - dmg / 2f, armor * 0.2f, 20f);
        dmg *= 1f - g / 25f;
        return Math.max(0f, dmg);
    }

    public static float health(LivingEntity e) {
        return e.getHealth() + e.getAbsorptionAmount();
    }
}
