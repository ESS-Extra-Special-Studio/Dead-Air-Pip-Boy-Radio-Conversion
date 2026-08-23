package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import uk.co.extraspecialstudio.dead_air.client.WalkieTalkieTuningScreen;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoyGui;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoyRaise;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoySilhouetteRenderer;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoyTab;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscFalloutDraw;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscFonts;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscPanel;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscRect;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscTabBar;

/**
 * Dead Air-backed Pip-Boy: embeds walkie tuning on the RADIO tab.
 * Only loaded when Dead Air is present.
 */
public class PipBoyDeadAirScreen extends WalkieTalkieTuningScreen implements PipBoyGui {
    private static final int CHROME_PAD = 10;
    private static final int TAB_GAP = 6;
    private static final int FOOTER_H = 44;

    private final EscTabBar tabBar;

    public PipBoyDeadAirScreen(InteractionHand hand) {
        super(hand);
        PipBoyRaise.onOpen();

        tabBar = new EscTabBar.Builder()
            .style(EscTabBar.Style.falloutPip())
            .add(EscTabBar.Tab.of("stat", "STAT"))
            .add(EscTabBar.Tab.disabled("inv", "INV"))
            .add(EscTabBar.Tab.disabled("map", "MAP"))
            .add(EscTabBar.Tab.of("radio", "RADIO"))
            .add(EscTabBar.Tab.disabled("special", "SPECIAL"))
            .build();
        tabBar.selectId("stat");
        tabBar.layoutAt(CHROME_PAD, CHROME_PAD);
    }

    public void setTab(PipBoyTab tab) {
        if (tab != null) {
            tabBar.selectId(tab.name().toLowerCase());
        }
    }

    public PipBoyTab getTab() {
        return PipBoyTab.fromId(tabBar.selected().id());
    }

    @Override
    protected int getBackdropColor() {
        return 0xE0000000;
    }

    @Override
    protected boolean shouldRenderRadioContent() {
        return "radio".equals(tabBar.selected().id());
    }

    @Override
    protected EscRect radioBodyRect() {
        EscRect full = chromeInner();
        tabBar.layoutFit(full.x(), full.y(), full.width());
        return tabBar.bodyBelow(full, TAB_GAP);
    }

    private EscRect chromeInner() {
        int margin = CHROME_PAD + 8;
        return new EscRect(
            margin,
            CHROME_PAD,
            Math.max(160, this.width - margin * 2),
            Math.max(140, this.height - CHROME_PAD * 2)
        );
    }

    @Override
    protected void renderOuterChrome(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        EscRect full = chromeInner();
        tabBar.layoutFit(full.x(), full.y(), full.width());

        int frameX = full.x() - 4;
        int frameY = full.y() - 4;
        int frameW = full.width() + 8;
        int frameH = full.height() + 8;

        g.fill(frameX - 4, frameY - 4, frameX + frameW + 4, frameY + frameH + 4, 0xFF0A1A0A);
        g.fill(frameX - 2, frameY - 2, frameX + frameW + 2, frameY + frameH + 2, 0xFF143014);
        EscPanel.fill(g, new EscRect(frameX, frameY, frameW, frameH), EscFalloutDraw.CRT_BLACK);
        EscPanel.border(g, new EscRect(frameX, frameY, frameW, frameH), EscFalloutDraw.PHOSPHOR, 1);
        EscFalloutDraw.scanlines(g, new EscRect(frameX, frameY, frameW, frameH), 3);

        tabBar.render(g, font, mouseX, mouseY);
    }

    @Override
    protected void renderNonRadioTab(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PipBoyTab tab = getTab();
        EscRect full = chromeInner();
        tabBar.layoutFit(full.x(), full.y(), full.width());
        EscRect body = tabBar.bodyBelow(full, TAB_GAP);
        switch (tab) {
            case STAT -> renderStatPage(g, body, partialTick);
            case INV, MAP, SPECIAL -> renderComingSoon(g, body, tab);
            default -> renderComingSoon(g, body, tab);
        }
    }

    private void renderStatPage(GuiGraphics g, EscRect body, float partialTick) {
        EscRect content = new EscRect(
            body.x() + 8,
            body.y() + 4,
            Math.max(32, body.width() - 16),
            Math.max(48, body.height() - 4 - FOOTER_H - 6)
        );
        PipBoySilhouetteRenderer.renderInGui(g, content, partialTick);
        renderStatFooter(g, body);
    }

    private void renderStatFooter(GuiGraphics g, EscRect body) {
        LocalPlayer player = Minecraft.getInstance().player;
        int footerY = body.bottom() - FOOTER_H;
        EscFalloutDraw.greenRule(g, body.x() + 4, footerY, body.right() - 4, EscFalloutDraw.PHOSPHOR_DIM);

        float hp = player != null ? player.getHealth() : 0f;
        float maxHp = player != null ? player.getMaxHealth() : 20f;
        int level = player != null ? player.experienceLevel : 0;
        float xp = player != null ? player.experienceProgress : 0f;

        int barX = body.x() + 8;
        int barW = body.width() - 16;
        EscFalloutDraw.labeledBar(
            g, font,
            "HP",
            String.format("%d/%d", Math.round(hp), Math.round(maxHp)),
            barX, footerY + 6, barW, 10,
            maxHp <= 0f ? 0f : hp / maxHp,
            EscFalloutDraw.PHOSPHOR,
            0xFF102010,
            0xFF00CC33
        );
        EscFalloutDraw.labeledBar(
            g, font,
            "LEVEL " + level,
            null,
            barX, footerY + 24, barW, 10,
            xp,
            EscFalloutDraw.PHOSPHOR,
            0xFF102010,
            EscFalloutDraw.PHOSPHOR
        );
    }

    private void renderComingSoon(GuiGraphics g, EscRect body, PipBoyTab tab) {
        int cx = body.x() + body.width() / 2;
        int cy = body.y() + body.height() / 2;
        Component title = Component.literal(tab.label).withStyle(s -> s.withFont(EscFonts.VT323));
        Component soon = Component.literal("COMING SOON").withStyle(s -> s.withFont(EscFonts.PRESS_START_2P));
        g.drawCenteredString(font, title, cx, cy - 18, EscFalloutDraw.PHOSPHOR);
        g.drawCenteredString(font, soon, cx, cy, EscFalloutDraw.PHOSPHOR_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && tabBar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        PipBoyRaise.onClose();
        super.onClose();
    }
}
