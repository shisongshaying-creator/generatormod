package com.example.generatormod.client;

import com.example.generatormod.GeneratorMod;
import com.example.generatormod.network.DepositCloudStoragePacket;
import com.example.generatormod.network.GeneratorNetwork;
import com.example.generatormod.network.WithdrawCloudStoragePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CloudStorageScreen extends Screen {
    private static final int MARGIN = 16;
    private static final int LIST_WIDTH = 240;
    private static final int ENTRY_HEIGHT = 36;

    private CloudStorageList storageList;
    private final Map<ResourceLocation, EntryData> visibleItems = new HashMap<>();
    private EditBox amountField;
    private Button depositButton;
    private Button withdrawButton;
    private ResourceLocation selectedId;

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

        int controlsLeft = getControlsLeft();
        int amountTop = listTop;
        int amountFieldHeight = 20;
        int buttonHeight = 20;
        int buttonSpacing = 6;
        int buttonOffset = 10;
        this.amountField = new EditBox(this.font, controlsLeft, amountTop, 110, amountFieldHeight,
                Component.translatable("screen." + GeneratorMod.MODID + ".cloud_storage_amount_label"));
        this.amountField.setFilter(value -> value == null || value.isEmpty() || value.chars().allMatch(Character::isDigit));
        this.amountField.setResponder(value -> updateControls());
        addRenderableWidget(this.amountField);

        this.depositButton = addRenderableWidget(Button.builder(
                        Component.translatable("button." + GeneratorMod.MODID + ".deposit"),
                        b -> depositSelected())
                .bounds(controlsLeft, amountTop + amountFieldHeight + buttonOffset, 110, buttonHeight)
                .build());

        this.withdrawButton = addRenderableWidget(Button.builder(
                        Component.translatable("button." + GeneratorMod.MODID + ".withdraw"),
                        b -> withdrawSelected())
                .bounds(controlsLeft, this.depositButton.getY() + buttonHeight + buttonSpacing, 110, buttonHeight)
                .build());

        refreshEntries();
        updateControls();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.amountField != null) {
            this.amountField.tick();
        }
        refreshEntries();
        updateControls();
    }

    private void refreshEntries() {
        if (this.storageList == null) {
            return;
        }
        Map<ResourceLocation, Long> stored = ClientCloudStorageState.INSTANCE.getStoredItems();
        Map<ResourceLocation, Long> inventory = getInventorySnapshot();
        Map<ResourceLocation, EntryData> combined = combineEntries(stored, inventory);
        if (!visibleItems.equals(combined)) {
            visibleItems.clear();
            visibleItems.putAll(combined);
            this.storageList.updateEntries(combined);
        } else {
            this.storageList.updateAmounts(combined);
        }
        this.storageList.selectEntry(this.selectedId);
    }

    private Map<ResourceLocation, Long> getInventorySnapshot() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return Map.of();
        }
        Map<ResourceLocation, Long> counts = new HashMap<>();
        player.getInventory().items.forEach(stack -> accumulate(counts, stack));
        player.getInventory().offhand.forEach(stack -> accumulate(counts, stack));
        return counts;
    }

    private void accumulate(Map<ResourceLocation, Long> counts, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        counts.merge(id, (long) stack.getCount(), Long::sum);
    }

    private Map<ResourceLocation, EntryData> combineEntries(Map<ResourceLocation, Long> stored,
                                                            Map<ResourceLocation, Long> inventory) {
        Map<ResourceLocation, EntryData> combined = new HashMap<>();
        Set<ResourceLocation> allKeys = new HashSet<>();
        allKeys.addAll(stored.keySet());
        allKeys.addAll(inventory.keySet());
        for (ResourceLocation id : allKeys) {
            long storedAmount = stored.getOrDefault(id, 0L);
            long inventoryAmount = inventory.getOrDefault(id, 0L);
            combined.put(id, new EntryData(storedAmount, inventoryAmount));
        }
        return combined;
    }

    private int getControlsLeft() {
        return MARGIN + LIST_WIDTH + 20;
    }

    private void updateControls() {
        CloudStorageEntry selected = this.storageList != null ? this.storageList.getSelected() : null;
        long storedAmount = selected != null ? selected.getStoredAmount() : 0L;
        long inventoryAmount = selected != null ? selected.getInventoryAmount() : 0L;
        boolean hasSelection = selected != null;
        if (this.depositButton != null) {
            this.depositButton.active = hasSelection && inventoryAmount > 0L;
        }
        if (this.withdrawButton != null) {
            this.withdrawButton.active = hasSelection && storedAmount > 0L;
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

        if (this.amountField != null) {
            int controlsLeft = getControlsLeft();
            Component amountLabel = Component.translatable("screen." + GeneratorMod.MODID + ".cloud_storage_amount_label");
            graphics.drawString(this.font, amountLabel, controlsLeft, this.amountField.getY() - 10, 0xFFFFFF, false);
            int hintTop = this.withdrawButton != null
                    ? this.withdrawButton.getY() + this.withdrawButton.getHeight() + 6
                    : this.amountField.getY() + this.amountField.getHeight() + 6;
            Component hint = Component.translatable("screen." + GeneratorMod.MODID + ".cloud_storage_hint");
            graphics.drawString(this.font, hint, controlsLeft, hintTop, 0xA0A0A0, false);

            CloudStorageEntry selected = this.storageList != null ? this.storageList.getSelected() : null;
            int infoTop = hintTop + 18;
            if (selected != null) {
                graphics.drawString(this.font, selected.getName(), controlsLeft, infoTop, 0xFFFFFF, false);
                infoTop += 12;
                graphics.drawString(this.font, getStoredAmountComponent(selected.getStoredAmount()), controlsLeft, infoTop, 0xC0C0C0, false);
                infoTop += 12;
                graphics.drawString(this.font, getInventoryAmountComponent(selected.getInventoryAmount()), controlsLeft, infoTop, 0xC0C0C0, false);
            } else {
                Component prompt = Component.translatable("screen." + GeneratorMod.MODID + ".cloud_storage_select");
                graphics.drawString(this.font, prompt, controlsLeft, infoTop, 0xA0A0A0, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void refreshFromNetwork() {
        refreshEntries();
        updateControls();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void depositSelected() {
        CloudStorageEntry entry = this.storageList != null ? this.storageList.getSelected() : null;
        if (entry == null) {
            return;
        }
        long available = entry.getInventoryAmount();
        long amount = resolveRequestedAmount(available);
        if (amount <= 0L) {
            return;
        }
        GeneratorNetwork.CHANNEL.sendToServer(new DepositCloudStoragePacket(entry.getId(), amount));
    }

    private void withdrawSelected() {
        CloudStorageEntry entry = this.storageList != null ? this.storageList.getSelected() : null;
        if (entry == null) {
            return;
        }
        long available = entry.getStoredAmount();
        long amount = resolveRequestedAmount(available);
        if (amount <= 0L) {
            return;
        }
        GeneratorNetwork.CHANNEL.sendToServer(new WithdrawCloudStoragePacket(entry.getId(), amount));
    }

    private long resolveRequestedAmount(long available) {
        if (available <= 0L) {
            return 0L;
        }
        long parsed = parseAmountField();
        if (parsed <= 0L) {
            return available;
        }
        return Math.min(parsed, available);
    }

    private long parseAmountField() {
        if (this.amountField == null) {
            return 0L;
        }
        String value = this.amountField.getValue();
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Component getStoredAmountComponent(long amount) {
        return Component.translatable(
                "screen." + GeneratorMod.MODID + ".cloud_storage_amount",
                NumberFormatUtil.formatCount(amount));
    }

    private Component getInventoryAmountComponent(long amount) {
        return Component.translatable(
                "screen." + GeneratorMod.MODID + ".cloud_storage_inventory",
                NumberFormatUtil.formatCount(amount));
    }

    private Component getItemName(ResourceLocation id) {
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            Item item = BuiltInRegistries.ITEM.get(id);
            return item.getDescription();
        }
        return Component.literal(id.toString());
    }

    private void onEntrySelected(CloudStorageEntry entry) {
        this.selectedId = entry != null ? entry.getId() : null;
        updateControls();
    }

    private record EntryData(long storedAmount, long inventoryAmount) {
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

        public void updateEntries(Map<ResourceLocation, EntryData> entries) {
            this.children().clear();
            super.setSelected(null);
            this.setScrollAmount(0);
            entries.entrySet().stream()
                    .sorted(Comparator.comparing(e -> getItemName(e.getKey()).getString(), String.CASE_INSENSITIVE_ORDER))
                    .map(e -> new CloudStorageEntry(e.getKey(), e.getValue()))
                    .forEach(this::addEntry);
        }

        public void updateAmounts(Map<ResourceLocation, EntryData> entries) {
            for (CloudStorageEntry entry : this.children()) {
                EntryData data = entries.get(entry.getId());
                if (data != null) {
                    entry.updateData(data);
                }
            }
        }

        public boolean isEmpty() {
            return this.children().isEmpty();
        }

        public void selectEntry(ResourceLocation id) {
            if (id == null) {
                super.setSelected(null);
                CloudStorageScreen.this.onEntrySelected(null);
                return;
            }
            for (CloudStorageEntry entry : this.children()) {
                if (entry.getId().equals(id)) {
                    super.setSelected(entry);
                    CloudStorageScreen.this.onEntrySelected(entry);
                    return;
                }
            }
            super.setSelected(null);
            CloudStorageScreen.this.onEntrySelected(null);
        }

        public void selectEntry(CloudStorageEntry entry) {
            super.setSelected(entry);
            CloudStorageScreen.this.onEntrySelected(entry);
        }
    }

    private class CloudStorageEntry extends ObjectSelectionList.Entry<CloudStorageEntry> {
        private final ResourceLocation id;
        private final Component name;
        private long storedAmount;
        private long inventoryAmount;

        private CloudStorageEntry(ResourceLocation id, EntryData data) {
            this.id = id;
            this.name = getItemName(id);
            this.storedAmount = data.storedAmount();
            this.inventoryAmount = data.inventoryAmount();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean selected = CloudStorageScreen.this.storageList.getSelected() == this;
            if (selected) {
                graphics.fill(left, top, left + width, top + height, 0x604848FF);
            } else if (hovered) {
                graphics.fill(left, top, left + width, top + height, 0x402020FF);
            }
            graphics.drawString(CloudStorageScreen.this.font, this.name, left + 4, top + 6, 0xFFFFFF, false);
            graphics.drawString(CloudStorageScreen.this.font, getStoredAmountComponent(this.storedAmount), left + 4, top + 18, 0xC0C0C0, false);
            graphics.drawString(CloudStorageScreen.this.font, getInventoryAmountComponent(this.inventoryAmount), left + 4, top + 28, 0xC0C0C0, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                CloudStorageScreen.this.storageList.selectEntry(this);
                return true;
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

        public Component getName() {
            return this.name;
        }

        public long getStoredAmount() {
            return this.storedAmount;
        }

        public long getInventoryAmount() {
            return this.inventoryAmount;
        }

        public void updateData(EntryData data) {
            this.storedAmount = data.storedAmount();
            this.inventoryAmount = data.inventoryAmount();
        }
    }
}
