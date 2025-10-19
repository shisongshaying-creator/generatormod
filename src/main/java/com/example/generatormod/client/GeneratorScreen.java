package com.example.generatormod.client;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.generator.GeneratorItems;
import com.example.generatormod.generator.GeneratorState;
import com.example.generatormod.network.CollectGeneratorPacket;
import com.example.generatormod.network.ExecuteGeneratorPacket;
import com.example.generatormod.network.GeneratorNetwork;
import com.example.generatormod.network.UpgradeGeneratorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeneratorScreen extends Screen {
    private static final int MARGIN = 16;
    private static final int LIST_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 110;

    private ItemSelectionList itemList;
    private Button executeButton;
    private Button collectButton;
    private Button speedButton;
    private Button quantityButton;
    private ResourceLocation selectedItem;

    public GeneratorScreen() {
        super(Component.translatable("screen." + GeneratorMod.MODID + ".generator"));
    }

    @Override
    protected void init() {
        ClientGeneratorState state = ClientGeneratorState.INSTANCE;
        this.selectedItem = state.getSelectedItem();

        int listTop = MARGIN + 20;
        int listBottom = this.height - MARGIN;

        this.itemList = new ItemSelectionList(this.minecraft, LIST_WIDTH, this.height, listTop, listBottom, 24);
        this.itemList.setLeftPos(MARGIN);
        addWidget(this.itemList);

        List<Item> items = GeneratorItems.getAvailableItems();
        for (Item item : items) {
            this.itemList.addItem(new ItemEntry(item)); // ← 外側からは addItem() を使う
        }
        this.itemList.selectItem(this.selectedItem);

        int buttonsLeft = MARGIN + LIST_WIDTH + MARGIN;
        int buttonsTop = this.height - MARGIN - BUTTON_HEIGHT * 2 - 8;

        this.executeButton = addRenderableWidget(
                Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".execute"), b -> execute())
                        .bounds(buttonsLeft, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        this.collectButton = addRenderableWidget(
                Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".collect"), b -> collect())
                        .bounds(buttonsLeft + BUTTON_WIDTH + 10, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        this.speedButton = addRenderableWidget(
                Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".speed_upgrade"), b -> upgrade(UpgradeGeneratorPacket.Type.SPEED))
                        .bounds(buttonsLeft, buttonsTop + BUTTON_HEIGHT + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
        this.quantityButton = addRenderableWidget(
                Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".quantity_upgrade"), b -> upgrade(UpgradeGeneratorPacket.Type.QUANTITY))
                        .bounds(buttonsLeft + BUTTON_WIDTH + 10, buttonsTop + BUTTON_HEIGHT + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        ClientGeneratorState state = ClientGeneratorState.INSTANCE;
        boolean hasSelection = this.selectedItem != null;
        boolean unlocked = hasSelection && state.isUnlocked(this.selectedItem);
        this.executeButton.active = hasSelection && !state.isRunning();
        this.collectButton.active = state.isRunning() || state.getStoredItems() > 0;
        int speedLevel = state.getSpeedLevel(this.selectedItem);
        int quantityLevel = state.getQuantityLevel(this.selectedItem);
        boolean canUpgradeSpeed = unlocked && speedLevel < GeneratorState.MAX_LEVEL;
        boolean canUpgradeQuantity = unlocked && quantityLevel < GeneratorState.MAX_LEVEL;
        this.speedButton.active = canUpgradeSpeed;
        this.quantityButton.active = canUpgradeQuantity;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawString(this.font, this.title, MARGIN, MARGIN, 0xFFFFFF, false);
        graphics.drawString(this.font,
                Component.translatable("screen." + GeneratorMod.MODID + ".select_item"),
                MARGIN, MARGIN + 10, 0xA0A0A0, false);

        // リストは自前で描画（子に追加しているので super.render でも描かれるが、ここで先に）
        this.itemList.render(graphics, mouseX, mouseY, partialTick);

        int buttonsLeft = MARGIN + LIST_WIDTH + MARGIN;
        int buttonsTop = this.height - MARGIN - BUTTON_HEIGHT * 2 - 8;

        int infoLeft = buttonsLeft;
        int infoTop = MARGIN;
        int infoRight = this.width - MARGIN;
        int infoBottom = buttonsTop - 8;
        graphics.fill(infoLeft - 4, infoTop - 4, infoRight + 4, infoBottom + 4, 0x88000000);

        drawInfo(graphics, infoLeft, infoTop);
        drawSummary(graphics, infoLeft, infoBottom, infoRight, buttonsLeft, buttonsTop);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawInfo(GuiGraphics graphics, int left, int top) {
        ClientGeneratorState state = ClientGeneratorState.INSTANCE;
        Component name = Component.translatable("screen." + GeneratorMod.MODID + ".no_selection");
        Item item = null;

        if (selectedItem != null) {
            item = BuiltInRegistries.ITEM.get(selectedItem);
            if (item != null) {
                name = item.getDescription();
            }
        }

        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".selected"), left, top, 0xFFFFFF, false);
        int lineY = top + 12;
        graphics.drawString(this.font, name, left, lineY, 0xEEEEEE, false);
        lineY += 18;

        int unlockCost = GeneratorItems.getUnlockCost(selectedItem);
        boolean locked = selectedItem != null && !state.isUnlocked(selectedItem);
        if (locked) {
            graphics.drawString(this.font,
                    Component.translatable("screen." + GeneratorMod.MODID + ".locked_detail", unlockCost),
                    left, lineY, 0xFF6666, false);
            lineY += 14;
        }

        int speedLevel = state.getSpeedLevel(this.selectedItem);
        int quantityLevel = state.getQuantityLevel(this.selectedItem);
        long interval = (selectedItem != null && item != null)
                ? GeneratorItems.computeIntervalMillis(item, speedLevel)
                : state.getIntervalMillis();
        int amount = GeneratorItems.computeAmountPerCycle(quantityLevel);
        long perItem = amount > 0 ? Math.max(1L, interval / amount) : interval;

        graphics.drawString(this.font,
                Component.translatable("screen." + GeneratorMod.MODID + ".time_per_item", formatDuration(perItem)),
                left, lineY, 0xFFFFFF, false);

        lineY += 20;
        graphics.drawString(this.font,
                Component.translatable("screen." + GeneratorMod.MODID + ".speed_level", speedLevel, getUpgradeCost(speedLevel)),
                left, lineY, 0xC0C0C0, false);
        graphics.drawString(this.font,
                Component.translatable("screen." + GeneratorMod.MODID + ".quantity_level", quantityLevel, getUpgradeCost(quantityLevel)),
                left, lineY + 12, 0xC0C0C0, false);
    }

    private void drawSummary(GuiGraphics graphics, int left, int infoBottom, int right, int buttonsLeft, int buttonsTop) {
        ClientGeneratorState state = ClientGeneratorState.INSTANCE;
        int lineHeight = 12;
        ClientGeneratorState.DisplayState displayState = state.computeDisplayState(System.currentTimeMillis());

        Component statusComponent = Component.translatable("screen." + GeneratorMod.MODID + ".status",
                state.isRunning()
                        ? Component.translatable("screen." + GeneratorMod.MODID + ".running")
                        : Component.translatable("screen." + GeneratorMod.MODID + ".stopped"));
        Component storedComponent = Component.translatable(
                "screen." + GeneratorMod.MODID + ".stored",
                NumberFormatUtil.formatCount(displayState.storedItems()));

        boolean hasNextIn = state.isRunning() && state.getIntervalMillis() > 0;
        Component nextComponent = hasNextIn
                ? Component.translatable("screen." + GeneratorMod.MODID + ".next_in",
                        formatDuration(Math.max(0L, state.getIntervalMillis() - displayState.elapsedInCycleMillis())))
                : null;

        boolean hasMessage = !state.getMessage().isEmpty();
        Component messageComponent = hasMessage
                ? createMessageComponent(state.getMessage())
                : null;

        List<Component> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int statusIndex;
        if (hasMessage) {
            lines.add(messageComponent);
            colors.add(0xFFD37F);
        }
        if (hasNextIn) {
            lines.add(nextComponent);
            colors.add(0xFFFFFF);
        }
        statusIndex = lines.size();
        lines.add(statusComponent);
        colors.add(0xFFFFFF);
        lines.add(storedComponent);
        colors.add(0xFFFFFF);

        int topLineY = buttonsTop - lineHeight * lines.size() - 4;
        int backgroundTop = Math.min(topLineY - 4, infoBottom - 4);
        int backgroundBottom = buttonsTop - 2;
        graphics.fill(left - 4, backgroundTop, right + 4, backgroundBottom, 0x88000000);

        int y = topLineY;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int color = colors.get(i);
            graphics.drawString(this.font, line, buttonsLeft, y, color, false);
            y += lineHeight;
        }

        if (state.isRunning() && state.getRunningSince() > 0) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - state.getRunningSince());
            Component elapsedComponent = Component.translatable("screen." + GeneratorMod.MODID + ".elapsed", formatDuration(elapsed));
            int elapsedX = right - this.font.width(elapsedComponent);
            int elapsedY = topLineY + statusIndex * lineHeight;
            graphics.drawString(this.font, elapsedComponent, elapsedX, elapsedY, 0xFFFFFF, false);
        }
    }

    private Component createMessageComponent(String messageKey) {
        String base = "message." + GeneratorMod.MODID + "." + messageKey;
        if ("unlock_missing_item".equals(messageKey)) {
            int unlockCost = GeneratorItems.getUnlockCost(this.selectedItem);
            return Component.translatable(base, unlockCost);
        }
        return Component.translatable(base);
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        seconds %= 60L;
        minutes %= 60L;
        if (hours > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        if (minutes > 0) return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        return seconds + "s";
    }

    private int getUpgradeCost(int level) {
        return 4 + level * 3;
    }

    private void execute() {
        if (selectedItem == null) return;
        GeneratorNetwork.CHANNEL.sendToServer(new ExecuteGeneratorPacket(selectedItem));
    }

    private void collect() {
        GeneratorNetwork.CHANNEL.sendToServer(new CollectGeneratorPacket());
    }

    private void upgrade(UpgradeGeneratorPacket.Type type) {
        if (selectedItem == null) return;
        GeneratorNetwork.CHANNEL.sendToServer(new UpgradeGeneratorPacket(type, selectedItem));
    }

    public void refreshSelection(ResourceLocation serverSelection) {
        this.selectedItem = serverSelection;
        if (this.itemList != null) {
            this.itemList.selectItem(serverSelection);
        }
        updateButtons();
    }

    private void onSelectItem(ResourceLocation id) {
        this.selectedItem = id;
        updateButtons();
    }

    //======================
    // リスト実装
    //======================
    private class ItemSelectionList extends ObjectSelectionList<ItemEntry> {
        private ItemSelectionList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int itemHeight) {
            super(minecraft, width, screenHeight, top, bottom, itemHeight);
        }

        // ← 外側から protected の addEntry に触らないためのラッパー
        public void addItem(ItemEntry entry) {
            this.addEntry(entry);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getLeft() + LIST_WIDTH - 6;
        }

        @Override
        public int getRowWidth() {
            return LIST_WIDTH;
        }

        public void selectItem(ResourceLocation id) {
            if (id == null) {
                setSelected(null);
                return;
            }
            for (ItemEntry entry : this.children()) {
                if (entry.getItemId().equals(id)) {
                    setSelected(entry);
                    break;
                }
            }
        }
    }

    private class ItemEntry extends ObjectSelectionList.Entry<ItemEntry> {
        private final Item item;
        private final ResourceLocation id;
        private final Component name;
        private final Component lockedName;

        private ItemEntry(Item item) {
            this.item = item;
            this.id = BuiltInRegistries.ITEM.getKey(item);
            this.name = item.getDescription();
            this.lockedName = Component.translatable("screen." + GeneratorMod.MODID + ".locked_entry", this.name);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean selected = GeneratorScreen.this.selectedItem != null
                    && GeneratorScreen.this.selectedItem.equals(this.id);
            boolean unlocked = ClientGeneratorState.INSTANCE.isUnlocked(this.id);
            Component displayName = unlocked ? this.name : this.lockedName;
            int textColor;
            if (unlocked) {
                textColor = (hovered || selected) ? 0xFFFFFF : 0xC0C0C0;
            } else {
                textColor = (hovered || selected) ? 0xFFAAAA : 0xFF6666;
            }
            graphics.drawString(GeneratorScreen.this.font, displayName, left + 4, top + 6, textColor, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                GeneratorScreen.this.itemList.setSelected(this);
                GeneratorScreen.this.onSelectItem(this.id);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return this.name;
        }

        public ResourceLocation getItemId() {
            return this.id;
        }
    }
}
