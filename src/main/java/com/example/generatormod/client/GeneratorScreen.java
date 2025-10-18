package com.example.generatormod.client;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.generator.GeneratorItems;
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
            this.itemList.addEntry(new ItemEntry(item));
        }
        this.itemList.selectItem(this.selectedItem);

        int buttonsLeft = MARGIN + LIST_WIDTH + MARGIN;
        int buttonsTop = this.height - MARGIN - BUTTON_HEIGHT * 2 - 8;
        this.executeButton = addRenderableWidget(Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".execute"), button -> execute())
            .bounds(buttonsLeft, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        this.collectButton = addRenderableWidget(Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".collect"), button -> collect())
            .bounds(buttonsLeft + BUTTON_WIDTH + 10, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.speedButton = addRenderableWidget(Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".speed_upgrade"), button -> upgrade(UpgradeGeneratorPacket.Type.SPEED))
            .bounds(buttonsLeft, buttonsTop + BUTTON_HEIGHT + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        this.quantityButton = addRenderableWidget(Button.builder(Component.translatable("button." + GeneratorMod.MODID + ".quantity_upgrade"), button -> upgrade(UpgradeGeneratorPacket.Type.QUANTITY))
            .bounds(buttonsLeft + BUTTON_WIDTH + 10, buttonsTop + BUTTON_HEIGHT + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

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
        this.executeButton.active = hasSelection && !state.isRunning();
        this.collectButton.active = state.isRunning() || state.getStoredItems() > 0;
        this.speedButton.active = hasSelection;
        this.quantityButton.active = hasSelection;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawString(this.font, this.title, MARGIN, MARGIN, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".select_item"), MARGIN, MARGIN + 10, 0xA0A0A0, false);
        this.itemList.render(graphics, mouseX, mouseY, partialTick);

        int infoLeft = MARGIN + LIST_WIDTH + MARGIN;
        int infoTop = MARGIN;
        int infoRight = this.width - MARGIN;
        int infoBottom = this.height - MARGIN - BUTTON_HEIGHT * 2 - 16;
        graphics.fill(infoLeft - 4, infoTop - 4, infoRight + 4, infoBottom + 4, 0x88000000);

        drawInfo(graphics, infoLeft, infoTop);
        drawLog(graphics, infoLeft, infoBottom + 8, infoRight);

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
        graphics.drawString(this.font, name, left, top + 12, 0xEEEEEE, false);

        long interval = selectedItem != null && item != null
            ? GeneratorItems.computeIntervalMillis(item, state.getSpeedLevel())
            : state.getIntervalMillis();
        int amount = state.getAmountPerCycle();
        long perItem = amount > 0 ? Math.max(1L, interval / amount) : interval;
        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".time_per_item", formatDuration(perItem)), left, top + 30, 0xFFFFFF, false);

        int lineY = top + 50;
        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".speed_level", state.getSpeedLevel(), getUpgradeCost(state.getSpeedLevel())), left, lineY, 0xC0C0C0, false);
        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".quantity_level", state.getQuantityLevel(), getUpgradeCost(state.getQuantityLevel())), left, lineY + 12, 0xC0C0C0, false);
    }

    private void drawLog(GuiGraphics graphics, int left, int top, int right) {
        ClientGeneratorState state = ClientGeneratorState.INSTANCE;
        int y = top + 4;
        int width = right - left;
        graphics.fill(left - 4, top - 4, right + 4, top + 80, 0x88000000);

        if (!state.getMessage().isEmpty()) {
            Component messageComponent = Component.translatable("message." + GeneratorMod.MODID + "." + state.getMessage());
            graphics.drawString(this.font, messageComponent, left, y, 0xFFD37F, false);
            y += 12;
        }
        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".status", state.isRunning() ? Component.translatable("screen." + GeneratorMod.MODID + ".running") : Component.translatable("screen." + GeneratorMod.MODID + ".stopped")), left, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".stored", state.getStoredItems()), left, y, 0xFFFFFF, false);
        y += 12;
        if (state.isRunning() && state.getIntervalMillis() > 0) {
            long until = Math.max(0L, state.getIntervalMillis() - state.getLeftoverMillis());
            graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".next_in", formatDuration(until)), left, y, 0xFFFFFF, false);
            y += 12;
        }
        if (state.isRunning() && state.getRunningSince() > 0) {
            long elapsed = System.currentTimeMillis() - state.getRunningSince();
            graphics.drawString(this.font, Component.translatable("screen." + GeneratorMod.MODID + ".elapsed", formatDuration(elapsed)), left, y, 0xFFFFFF, false);
        }
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        seconds %= 60L;
        minutes %= 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        }
        return seconds + "s";
    }

    private int getUpgradeCost(int level) {
        return 4 + level * 3;
    }

    private void execute() {
        if (selectedItem == null) {
            return;
        }
        GeneratorNetwork.CHANNEL.sendToServer(new ExecuteGeneratorPacket(selectedItem));
    }

    private void collect() {
        GeneratorNetwork.CHANNEL.sendToServer(new CollectGeneratorPacket());
    }

    private void upgrade(UpgradeGeneratorPacket.Type type) {
        if (selectedItem == null) {
            return;
        }
        GeneratorNetwork.CHANNEL.sendToServer(new UpgradeGeneratorPacket(type));
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

    private class ItemSelectionList extends ObjectSelectionList<ItemEntry> {
        private ItemSelectionList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int itemHeight) {
            super(minecraft, width, screenHeight, top, bottom, itemHeight);
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

        private ItemEntry(Item item) {
            this.item = item;
            this.id = BuiltInRegistries.ITEM.getKey(item);
            this.name = item.getDescription();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int textColor = hovered || GeneratorScreen.this.selectedItem != null && GeneratorScreen.this.selectedItem.equals(this.id) ? 0xFFFFFF : 0xC0C0C0;
            graphics.drawString(GeneratorScreen.this.font, this.name, left + 4, top + 6, textColor, false);
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
