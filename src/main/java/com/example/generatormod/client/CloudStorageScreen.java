package com.example.generatormod.client;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.network.GeneratorNetwork;
import com.example.generatormod.network.WithdrawCloudStoragePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CloudStorageScreen extends Screen {
    private static final int MARGIN = 16;
    private static final int LIST_WIDTH = 240;
    private static final int ENTRY_HEIGHT = 26;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 18;

    private CloudStorageList storageList;
    private final Map<ResourceLocation, Long> visibleItems = new HashMap<>();

    public CloudStorageScreen() {
        super(Component.translatable("screen." + GeneratorMod.MODID + ".cloud_storage"));
    }

    @Override
    protected void init() {
        super.init();
        int listTop = MARGIN + 20;
        int listBottom = this.height - MARGIN;
        this.storageList = new CloudStorageList(this.minecraft, LIST_WIDTH, this.height, listTop, listBottom, ENTRY_HEIGHT);
        this.storageList.setLeftPos(MARGIN);
        addWidget(this.storageList);
        refreshEntries();
    }

    @Override
    public void tick() {
        super.tick();
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getInventory().getFreeSlot() == -1) {
            onClose();
            return;
        }
        refreshEntries();
    }

    private void refreshEntries() {
        if (this.storageList == null) {
            return;
        }
        Map<ResourceLocation, Long> current = ClientCloudStorageState.INSTANCE.getStoredItems();
        if (!visibleItems.equals(current)) {
            visibleItems.clear();
            visibleItems.putAll(current);
            this.storageList.updateEntries(current);
        } else {
            this.storageList.updateAmounts(current);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawString(this.font, this.title, MARGIN, MARGIN, 0xFFFFFF, false);
        if (this.storageList != null) {
            this.storageList.render(graphics, mouseX, mouseY, partialTick);
            if (this.storageList.isEmpty()) {
                Component empty = Component.translatable("screen." + GeneratorMod.MODID + ".cloud_storage_empty");
                graphics.drawCenteredString(this.font, empty, this.width / 2, this.height / 2, 0xA0A0A0);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void refreshFromNetwork() {
        refreshEntries();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void withdraw(ResourceLocation id) {
        if (id == null) {
            return;
        }
        GeneratorNetwork.CHANNEL.sendToServer(new WithdrawCloudStoragePacket(id, Long.MAX_VALUE));
    }

    private Component getAmountComponent(long amount) {
        return Component.translatable(
                "screen." + GeneratorMod.MODID + ".cloud_storage_amount",
                NumberFormatUtil.formatCount(amount));
    }

    private Component getItemName(ResourceLocation id) {
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            Item item = BuiltInRegistries.ITEM.get(id);
            return item.getDescription();
        }
        return Component.literal(id.toString());
    }

    private class CloudStorageList extends ObjectSelectionList<CloudStorageEntry> {
        private CloudStorageList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int itemHeight) {
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

        public void updateEntries(Map<ResourceLocation, Long> entries) {
            this.children().clear();
            this.setSelected(null);
            this.setScrollAmount(0);
            entries.entrySet().stream()
                    .sorted(Comparator.comparing(e -> getItemName(e.getKey()).getString(), String.CASE_INSENSITIVE_ORDER))
                    .map(e -> new CloudStorageEntry(e.getKey(), e.getValue()))
                    .forEach(this::addEntry);
        }

        public void updateAmounts(Map<ResourceLocation, Long> entries) {
            for (CloudStorageEntry entry : this.children()) {
                entry.updateAmount(entries.getOrDefault(entry.getId(), 0L));
            }
        }

        public boolean isEmpty() {
            return this.children().isEmpty();
        }
    }

    private class CloudStorageEntry extends ObjectSelectionList.Entry<CloudStorageEntry> {
        private final ResourceLocation id;
        private final Component name;
        private long amount;

        private CloudStorageEntry(ResourceLocation id, long amount) {
            this.id = id;
            this.name = getItemName(id);
            this.amount = amount;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawString(CloudStorageScreen.this.font, this.name, left + 4, top + 6, 0xFFFFFF, false);
            Component amountComponent = getAmountComponent(this.amount);
            graphics.drawString(CloudStorageScreen.this.font, amountComponent, left + 4, top + 16, 0xC0C0C0, false);

            int buttonLeft = left + width - BUTTON_WIDTH - 6;
            int buttonTop = top + (height - BUTTON_HEIGHT) / 2;
            boolean buttonHovered = mouseX >= buttonLeft && mouseX <= buttonLeft + BUTTON_WIDTH
                    && mouseY >= buttonTop && mouseY <= buttonTop + BUTTON_HEIGHT;
            int backgroundColor = buttonHovered ? 0xFF5A5AFF : 0xFF3A3AFF;
            graphics.fill(buttonLeft, buttonTop, buttonLeft + BUTTON_WIDTH, buttonTop + BUTTON_HEIGHT, backgroundColor);
            Component label = Component.translatable("button." + GeneratorMod.MODID + ".withdraw");
            int labelWidth = CloudStorageScreen.this.font.width(label);
            int labelX = buttonLeft + (BUTTON_WIDTH - labelWidth) / 2;
            int labelY = buttonTop + (BUTTON_HEIGHT - 8) / 2;
            graphics.drawString(CloudStorageScreen.this.font, label, labelX, labelY, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int left = CloudStorageScreen.this.storageList.getLeft();
                int topIndex = CloudStorageScreen.this.storageList.children().indexOf(this);
                if (topIndex < 0) {
                    return false;
                }
                int top = CloudStorageScreen.this.storageList.getRowTop(topIndex);
                int rowBottom = CloudStorageScreen.this.storageList.getRowBottom(topIndex);
                int rowHeight = rowBottom - top;
                int buttonLeft = left + LIST_WIDTH - BUTTON_WIDTH - 6;
                int buttonTop = top + Math.max(0, (rowHeight - BUTTON_HEIGHT) / 2);
                if (mouseX >= buttonLeft && mouseX <= buttonLeft + BUTTON_WIDTH
                        && mouseY >= buttonTop && mouseY <= buttonTop + BUTTON_HEIGHT) {
                    withdraw(this.id);
                    return true;
                }
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return this.name;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        public void updateAmount(long amount) {
            this.amount = amount;
        }
    }
}
