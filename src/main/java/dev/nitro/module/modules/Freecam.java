package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.NumberSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;

/**
 * Detaches the camera from your player. Your character stands still (movement, attack and use
 * keys are blocked) while you fly the camera around. Don't click on things while in freecam.
 */
public class Freecam extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));
    private OtherClientPlayerEntity cam;
    private float savedYaw, savedPitch;

    public Freecam() { super("Freecam", "Fly the camera around while your player stays put.", Category.RENDER); }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { toggle(); return; }
        cam = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        cam.copyPositionAndRotation(mc.player);
        syncPrev();
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
        mc.setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
    }

    private void syncPrev() {
        cam.prevX = cam.lastRenderX = cam.getX();
        cam.prevY = cam.lastRenderY = cam.getY();
        cam.prevZ = cam.lastRenderZ = cam.getZ();
        cam.prevYaw = cam.getYaw();
        cam.prevPitch = cam.getPitch();
    }

    @Override
    public void onPreTick() {
        // stop the real player from receiving input
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
        mc.options.pickItemKey.setPressed(false);
    }

    private boolean down(KeyBinding k) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(k);
        if (key.getCategory() != InputUtil.Type.KEYSYM) return false;
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getCode());
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (cam == null || cam.getWorld() != mc.world) { toggle(); return; }
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
        if (mc.currentScreen != null) { syncPrev(); return; }

        double s = speed.get();
        double yaw = Math.toRadians(cam.getYaw());
        int fw = (down(mc.options.forwardKey) ? 1 : 0) - (down(mc.options.backKey) ? 1 : 0);
        int side = (down(mc.options.leftKey) ? 1 : 0) - (down(mc.options.rightKey) ? 1 : 0);
        double up = (down(mc.options.jumpKey) ? 1 : 0) - (down(mc.options.sneakKey) ? 1 : 0);

        double mx = (-Math.sin(yaw) * fw + Math.cos(yaw) * side) * s;
        double mz = (Math.cos(yaw) * fw + Math.sin(yaw) * side) * s;

        syncPrev();
        cam.setPosition(cam.getX() + mx, cam.getY() + up * s, cam.getZ() + mz);
    }

    @Override
    public void onFrameStart() {
        // mouse movement rotates the real player; move that rotation over to the camera
        if (cam == null || mc.player == null) return;
        float dy = mc.player.getYaw() - savedYaw;
        float dp = mc.player.getPitch() - savedPitch;
        if (dy == 0 && dp == 0) return;

        cam.setYaw(cam.getYaw() + dy);
        cam.setPitch(MathHelper.clamp(cam.getPitch() + dp, -90f, 90f));
        cam.prevYaw = cam.getYaw();
        cam.prevPitch = cam.getPitch();

        mc.player.setYaw(savedYaw);
        mc.player.setPitch(savedPitch);
        mc.player.prevYaw = savedYaw;
        mc.player.prevPitch = savedPitch;
    }
}
