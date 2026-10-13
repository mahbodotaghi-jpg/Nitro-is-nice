package dev.nitro.gui;

import dev.nitro.NitroClient;
import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.ModeSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.setting.Setting;
import dev.nitro.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Style D: top category tabs, module list on the left, settings drawer on the right.
 * Click a module to select it, click its switch (or right-click) to toggle it.
 * Drag the top bar to move the window. Mouse wheel scrolls the list / drawer.
 */
public class ClickGuiScreen extends Screen {
    private enum Kind { TAB, ROW, ENABLE, SETTING, BIND, TOPBAR }
    private record Hit(Kind kind, int x, int y, int w, int h, Category cat, Module module, Setting<?> setting) {}

    private static final int WIN_W = 420, WIN_H = 250, TOP = 24, FOOT = 16, LIST_W = 250, ROW_H = 20;

    private static int winX = Integer.MIN_VALUE, winY;
    private static Category activeCat = Category.COMBAT;
    private static Module selected, lastSelected;
    private static double scroll, drawerScroll;

    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();
    private boolean draggingWin;
    private int dragDX, dragDY;
    private Hit dragSlider;
    private Module listening;
    private int bodyY1, bodyY2, setY1, setY2;

    public ClickGuiScreen() { super(Text.literal("NITRO")); }

    @Override
    protected void init() {
        if (winX == Integer.MIN_VALUE) {
            winX = (width - WIN_W) / 2;
            winY = (height - WIN_H) / 2;
        }
        clampWindow();
    }

    private void clampWindow() {
        winX = Math.max(0, Math.min(winX, Math.max(0, width - WIN_W)));
        winY = Math.max(0, Math.min(winY, Math.max(0, height - WIN_H)));
    }

    @Override public boolean shouldPause() { return false; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int rowH(Setting<?> s) { return s instanceof NumberSetting ? 26 : 20; }

    private void drawSwitch(DrawContext ctx, int x, int y, boolean on, long t) {
        ctx.fill(x, y, x + 22, y + 10, on ? Theme.accent(t, x) : 0xFF2A2A36);
        int kx = on ? x + 13 : x + 1;
        ctx.fill(kx, y + 1, kx + 8, y + 9, on ? 0xFFFFFFFF : 0xFF888888);
    }

    private static String fmt(NumberSetting n) {
        return n.step >= 1 ? String.valueOf((int) Math.round(n.get())) : String.format(Locale.ROOT, "%.1f", n.get());
    }

    private static String keyName(int key) {
        return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long t = System.currentTimeMillis();
        float p = Math.min(1f, (t - openedAt) / 180f);
        float e = 1f - (1f - p) * (1f - p);
        int wx = winX, wy = winY + (int) ((1f - e) * 10);

        ctx.fill(0, 0, width, height, ((int) (0x66 * e)) << 24);
        hits.clear();

        ctx.fill(wx - 1, wy - 1, wx + WIN_W + 1, wy + WIN_H + 1, Theme.withAlpha(Theme.accent(t, 0), 0x70));
        ctx.fill(wx, wy, wx + WIN_W, wy + WIN_H, 0xFF09090F);

        // top bar
        ctx.fill(wx, wy, wx + WIN_W, wy + TOP, 0xFF0D0D14);
        int lx = wx + 10;
        for (char c : "NITRO".toCharArray()) {
            String s = String.valueOf(c);
            ctx.drawTextWithShadow(textRenderer, s, lx, wy + 8, Theme.accent(t, lx));
            lx += textRenderer.getWidth(s) + 1;
        }
        int tx = wx + 70;
        for (Category c : Category.values()) {
            int tw = textRenderer.getWidth(c.display) + 14;
            boolean act = c == activeCat;
            boolean hov = in(mx, my, tx, wy, tw, TOP);
            if (hov && !act) ctx.fill(tx, wy, tx + tw, wy + TOP, 0x18FFFFFF);
            ctx.drawTextWithShadow(textRenderer, c.display, tx + 7, wy + 8, act ? 0xFFFFFFFF : hov ? 0xFFCCCCCC : 0xFF777777);
            if (act) ctx.fill(tx, wy + TOP - 2, tx + tw, wy + TOP, Theme.accent(t, tx));
            hits.add(new Hit(Kind.TAB, tx, wy, tw, TOP, c, null, null));
            tx += tw;
        }
        String cnt = NitroClient.modules.getEnabled().size() + " on";
        ctx.drawTextWithShadow(textRenderer, cnt, wx + WIN_W - textRenderer.getWidth(cnt) - 8, wy + 8, 0xFF666666);
        hits.add(new Hit(Kind.TOPBAR, wx, wy, WIN_W, TOP, null, null, null));

        // body
        int by = wy + TOP, bh = WIN_H - TOP - FOOT;
        bodyY1 = by; bodyY2 = by + bh;
        List<Module> mods = NitroClient.modules.getByCategory(activeCat);
        if (selected == null || selected.getCategory() != activeCat) selected = mods.isEmpty() ? null : mods.get(0);
        if (selected != lastSelected) { drawerScroll = 0; lastSelected = selected; }

        double maxScroll = Math.max(0, mods.size() * ROW_H + 8 - bh);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        // module list
        ctx.enableScissor(wx, by, wx + LIST_W, by + bh);
        int ry = by + 4 - (int) scroll;
        for (Module m : mods) {
            boolean sel = m == selected;
            boolean hov = in(mx, my, wx + 4, ry, LIST_W - 8, ROW_H - 2) && my >= by && my < by + bh;
            if (sel) ctx.fill(wx + 4, ry, wx + LIST_W - 4, ry + ROW_H - 2, Theme.withAlpha(Theme.accent(t, ry), 0x40));
            else if (hov) ctx.fill(wx + 4, ry, wx + LIST_W - 4, ry + ROW_H - 2, 0x18FFFFFF);
            if (m.isEnabled()) ctx.fill(wx + 4, ry, wx + 6, ry + ROW_H - 2, Theme.accent(t, ry));
            ctx.drawTextWithShadow(textRenderer, m.getName(), wx + 14, ry + 5, m.isEnabled() ? 0xFFFFFFFF : 0xFF999999);
            if (m.getKey() >= 0) {
                String b = "[" + keyName(m.getKey()) + "]";
                ctx.drawTextWithShadow(textRenderer, b, wx + LIST_W - 46 - textRenderer.getWidth(b), ry + 5, 0xFF666666);
            }
            int sx = wx + LIST_W - 34;
            drawSwitch(ctx, sx, ry + 4, m.isEnabled(), t);
            hits.add(new Hit(Kind.ENABLE, sx - 4, ry, 30, ROW_H - 2, null, m, null));
            hits.add(new Hit(Kind.ROW, wx + 4, ry, LIST_W - 8, ROW_H - 2, null, m, null));
            ry += ROW_H;
        }
        ctx.disableScissor();

        // drawer
        ctx.fill(wx + LIST_W, by, wx + LIST_W + 1, by + bh, 0xFF1F1F2B);
        ctx.fill(wx + LIST_W + 1, by, wx + WIN_W, by + bh, 0xFF0D0D14);
        setY1 = setY2 = by;
        if (selected != null) {
            int dx = wx + LIST_W + 10, dw = WIN_W - LIST_W - 20, dy = by + 8;
            ctx.drawTextWithShadow(textRenderer, selected.getName(), dx, dy, 0xFFFFFFFF);
            dy += 14;
            for (OrderedText line : textRenderer.wrapLines(Text.literal(selected.getDescription()), dw)) {
                ctx.drawTextWithShadow(textRenderer, line, dx, dy, 0xFF777777);
                dy += 10;
            }
            dy += 4;

            boolean en = selected.isEnabled();
            ctx.fill(dx, dy, dx + dw, dy + 16, en ? Theme.withAlpha(Theme.accent(t, dy), 0x80) : 0xFF1A1A24);
            String et = en ? "Enabled" : "Disabled";
            ctx.drawTextWithShadow(textRenderer, et, dx + (dw - textRenderer.getWidth(et)) / 2, dy + 4, en ? 0xFFFFFFFF : 0xFF888888);
            hits.add(new Hit(Kind.ENABLE, dx, dy, dw, 16, null, selected, null));
            dy += 24;

            // settings (scrollable area)
            int setTop = dy, bY = by + bh - 22, setBottom = bY - 4;
            setY1 = setTop; setY2 = setBottom;
            int content = 0;
            for (Setting<?> s : selected.getSettings()) content += rowH(s);
            double maxDS = Math.max(0, content - (setBottom - setTop));
            drawerScroll = Math.max(0, Math.min(drawerScroll, maxDS));

            ctx.enableScissor(dx - 6, setTop, wx + WIN_W, setBottom);
            int sy = setTop - (int) drawerScroll;
            for (Setting<?> s : selected.getSettings()) {
                if (s instanceof BooleanSetting b) {
                    ctx.drawTextWithShadow(textRenderer, s.name, dx, sy + 2, 0xFFBBBBBB);
                    drawSwitch(ctx, dx + dw - 22, sy, b.get(), t);
                    hits.add(new Hit(Kind.SETTING, dx - 2, sy - 2, dw + 4, 18, null, selected, s));
                } else if (s instanceof ModeSetting ms) {
                    ctx.drawTextWithShadow(textRenderer, s.name, dx, sy + 2, 0xFFBBBBBB);
                    String v = ms.get();
                    int vw = textRenderer.getWidth(v) + 10;
                    ctx.fill(dx + dw - vw, sy - 1, dx + dw, sy + 11, 0xFF1A1A24);
                    ctx.drawTextWithShadow(textRenderer, v, dx + dw - vw + 5, sy + 2, Theme.accent(t, sy));
                    hits.add(new Hit(Kind.SETTING, dx - 2, sy - 2, dw + 4, 18, null, selected, s));
                } else if (s instanceof NumberSetting n) {
                    double f = (n.get() - n.min) / (n.max - n.min);
                    ctx.drawTextWithShadow(textRenderer, s.name, dx, sy, 0xFFBBBBBB);
                    String v = fmt(n);
                    ctx.drawTextWithShadow(textRenderer, v, dx + dw - textRenderer.getWidth(v), sy, 0xFFFFFFFF);
                    int fw = (int) (dw * f);
                    ctx.fill(dx, sy + 14, dx + dw, sy + 17, 0xFF2A2A36);
                    ctx.fill(dx, sy + 14, dx + fw, sy + 17, Theme.accent(t, sy));
                    int kx = Math.max(dx, Math.min(dx + dw - 4, dx + fw - 2));
                    ctx.fill(kx, sy + 11, kx + 4, sy + 20, 0xFFFFFFFF);
                    hits.add(new Hit(Kind.SETTING, dx, sy - 2, dw, 24, null, selected, s));
                }
                sy += rowH(s);
            }
            ctx.disableScissor();

            if (maxDS > 0) {
                int trackH = setBottom - setTop;
                int barH = Math.max(12, (int) (trackH * trackH / (double) content));
                int barY = setTop + (int) ((trackH - barH) * (drawerScroll / maxDS));
                ctx.fill(wx + WIN_W - 4, barY, wx + WIN_W - 2, barY + barH, Theme.withAlpha(Theme.accent(t, 0), 0xA0));
            }

            // bind button
            boolean listen = listening == selected;
            ctx.fill(dx, bY, dx + dw, bY + 16, listen ? Theme.withAlpha(Theme.accent(t, 0), 0x60) : 0xFF1A1A24);
            String bt = listen ? "Press a key..." : selected.getKey() >= 0 ? "Bind: [" + keyName(selected.getKey()) + "]" : "Bind: none";
            ctx.drawTextWithShadow(textRenderer, bt, dx + (dw - textRenderer.getWidth(bt)) / 2, bY + 4, 0xFFCCCCCC);
            hits.add(new Hit(Kind.BIND, dx, bY, dw, 16, null, selected, null));
        }

        // footer
        ctx.fill(wx, wy + WIN_H - FOOT, wx + WIN_W, wy + WIN_H, 0xFF0D0D14);
        ctx.drawTextWithShadow(textRenderer, "RShift close   RMB toggle   Scroll list   Drag top bar", wx + 8, wy + WIN_H - 12, 0xFF555555);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (Hit h : hits) {
            if (!in(mx, my, h.x(), h.y(), h.w(), h.h())) continue;
            boolean listHit = h.kind() == Kind.ROW || (h.kind() == Kind.ENABLE && h.w() == 30);
            if (listHit && (my < bodyY1 || my >= bodyY2)) continue;
            if (h.kind() == Kind.SETTING && (my < setY1 || my >= setY2)) continue;

            switch (h.kind()) {
                case TAB -> { activeCat = h.cat(); selected = null; scroll = 0; }
                case ROW -> {
                    if (btn == 0) selected = h.module();
                    else if (btn == 1) h.module().toggle();
                }
                case ENABLE -> {
                    selected = h.module();
                    if (btn == 0 || btn == 1) h.module().toggle();
                }
                case SETTING -> {
                    if (btn != 0) break;
                    if (h.setting() instanceof BooleanSetting b) b.set(!b.get());
                    else if (h.setting() instanceof ModeSetting ms) ms.cycle();
                    else if (h.setting() instanceof NumberSetting) { dragSlider = h; slide(mx); }
                }
                case BIND -> {
                    if (btn == 0) listening = h.module();
                    else if (btn == 1) h.module().setKey(-1);
                }
                case TOPBAR -> {
                    if (btn == 0) { draggingWin = true; dragDX = (int) mx - winX; dragDY = (int) my - winY; }
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void slide(double mx) {
        if (dragSlider == null || !(dragSlider.setting() instanceof NumberSetting n)) return;
        double f = Math.max(0, Math.min(1, (mx - dragSlider.x()) / dragSlider.w()));
        n.set(n.min + f * (n.max - n.min));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingWin) {
            winX = (int) mx - dragDX;
            winY = (int) my - dragDY;
            clampWindow();
            return true;
        }
        if (dragSlider != null) { slide(mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingWin = false;
        dragSlider = null;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (in(mx, my, winX, bodyY1, WIN_W, bodyY2 - bodyY1)) {
            if (mx >= winX + LIST_W) drawerScroll -= vAmount * 12;
            else scroll -= vAmount * 14;
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            listening.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key);
            listening = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
