package com.example.generatormod.generator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GeneratorState {
    public static final int UNLOCK_COST = 4;
    private static final String TAG_SELECTED = "Selected";
    private static final String TAG_RUNNING = "Running";
    private static final String TAG_STORED = "Stored";
    private static final String TAG_SPEED = "Speed";
    private static final String TAG_QUANTITY = "Quantity";
    private static final String TAG_LAST_REAL = "LastReal";
    private static final String TAG_LEFTOVER = "Leftover";
    private static final String TAG_RUNNING_SINCE = "RunningSince";
    private static final String TAG_UNLOCKED = "Unlocked";

    private ResourceLocation selectedItem;
    private boolean running;
    private long storedItems;
    private int speedLevel;
    private int quantityLevel;
    private long lastRealTime;
    private long leftoverMillis;
    private long runningSince;
    private boolean dirty;
    private String transientMessage = "";
    private final Set<ResourceLocation> unlockedItems = new HashSet<>();

    public void load(CompoundTag tag) {
        selectedItem = tag.contains(TAG_SELECTED) ? new ResourceLocation(tag.getString(TAG_SELECTED)) : null;
        running = tag.getBoolean(TAG_RUNNING);
        storedItems = tag.getLong(TAG_STORED);
        speedLevel = tag.getInt(TAG_SPEED);
        quantityLevel = tag.getInt(TAG_QUANTITY);
        lastRealTime = tag.getLong(TAG_LAST_REAL);
        leftoverMillis = tag.getLong(TAG_LEFTOVER);
        runningSince = tag.getLong(TAG_RUNNING_SINCE);
        unlockedItems.clear();
        if (tag.contains(TAG_UNLOCKED)) {
            ListTag listTag = tag.getList(TAG_UNLOCKED, Tag.TAG_STRING);
            for (int i = 0; i < listTag.size(); i++) {
                String value = listTag.getString(i);
                if (!value.isEmpty()) {
                    unlockedItems.add(new ResourceLocation(value));
                }
            }
        }
        dirty = false;
    }

    public void save(CompoundTag tag) {
        if (selectedItem != null) {
            tag.putString(TAG_SELECTED, selectedItem.toString());
        } else {
            tag.remove(TAG_SELECTED);
        }
        tag.putBoolean(TAG_RUNNING, running);
        tag.putLong(TAG_STORED, storedItems);
        tag.putInt(TAG_SPEED, speedLevel);
        tag.putInt(TAG_QUANTITY, quantityLevel);
        tag.putLong(TAG_LAST_REAL, lastRealTime);
        tag.putLong(TAG_LEFTOVER, leftoverMillis);
        tag.putLong(TAG_RUNNING_SINCE, runningSince);
        ListTag listTag = new ListTag();
        for (ResourceLocation id : unlockedItems) {
            listTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put(TAG_UNLOCKED, listTag);
        dirty = false;
    }

    public boolean tick(long now) {
        if (!running || selectedItem == null) {
            lastRealTime = now;
            return false;
        }
        long elapsed = now - lastRealTime + leftoverMillis;
        long interval = getIntervalMillis();
        if (interval <= 0) {
            interval = 1000L;
        }
        long cycles = elapsed / interval;
        leftoverMillis = elapsed % interval;
        lastRealTime = now;
        if (cycles > 0) {
            storedItems += cycles * getAmountPerCycle();
            dirty = true;
            return true;
        }
        return false;
    }

    public void setSelectedItem(ResourceLocation id) {
        if (id != null && !id.equals(this.selectedItem)) {
            this.selectedItem = id;
            this.dirty = true;
        }
    }

    public boolean start(ResourceLocation id, ServerPlayer player, long now) {
        if (running) {
            transientMessage = "already_running";
            return false;
        }
        Optional<Item> optionalItem = GeneratorItems.resolve(id);
        if (optionalItem.isEmpty()) {
            transientMessage = "invalid_item";
            return false;
        }
        Item item = optionalItem.get();
        boolean wasUnlocked = unlockedItems.contains(id);
        if (!wasUnlocked) {
            if (!consumeItems(player, item, UNLOCK_COST)) {
                transientMessage = "unlock_missing_item";
                return false;
            }
            unlockedItems.add(id);
            transientMessage = "unlocked";
        } else {
            transientMessage = "started";
        }
        this.selectedItem = id;
        this.running = true;
        this.lastRealTime = now;
        this.leftoverMillis = 0L;
        this.runningSince = now;
        this.dirty = true;
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public ResourceLocation getSelectedItem() {
        return selectedItem;
    }

    public long getStoredItems() {
        return storedItems;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public int getQuantityLevel() {
        return quantityLevel;
    }

    public long getIntervalMillis() {
        if (selectedItem == null) {
            return 0L;
        }
        Optional<Item> optionalItem = GeneratorItems.resolve(selectedItem);
        return optionalItem.map(item -> GeneratorItems.computeIntervalMillis(item, speedLevel)).orElse(0L);
    }

    public int getAmountPerCycle() {
        return GeneratorItems.computeAmountPerCycle(quantityLevel);
    }

    public long getRunningSince() {
        return runningSince;
    }

    public long getLastRealTime() {
        return lastRealTime;
    }

    public void setLastRealTime(long time) {
        this.lastRealTime = time;
    }

    public long getLeftoverMillis() {
        return leftoverMillis;
    }

    public void stop() {
        if (running) {
            running = false;
            leftoverMillis = 0L;
            runningSince = 0L;
            dirty = true;
        }
    }

    public long collect() {
        long result = storedItems;
        if (storedItems > 0) {
            storedItems = 0L;
            dirty = true;
        }
        stop();
        transientMessage = result > 0 ? "collected" : "collected_empty";
        return result;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markSaved() {
        this.dirty = false;
    }

    public String getTransientMessage() {
        return transientMessage;
    }

    public void clearTransientMessage() {
        this.transientMessage = "";
    }

    public boolean upgradeSpeed(Player player) {
        if (selectedItem == null) {
            transientMessage = "no_selection";
            return false;
        }
        int cost = getUpgradeCost(speedLevel);
        Optional<Item> optionalItem = GeneratorItems.resolve(selectedItem);
        if (optionalItem.isEmpty()) {
            transientMessage = "invalid_item";
            return false;
        }
        if (!consumeItems(player, optionalItem.get(), cost)) {
            transientMessage = "upgrade_missing_items";
            return false;
        }
        speedLevel = Mth.clamp(speedLevel + 1, 0, 10);
        dirty = true;
        transientMessage = "speed_upgraded";
        return true;
    }

    public boolean upgradeQuantity(Player player) {
        if (selectedItem == null) {
            transientMessage = "no_selection";
            return false;
        }
        int cost = getUpgradeCost(quantityLevel);
        Optional<Item> optionalItem = GeneratorItems.resolve(selectedItem);
        if (optionalItem.isEmpty()) {
            transientMessage = "invalid_item";
            return false;
        }
        if (!consumeItems(player, optionalItem.get(), cost)) {
            transientMessage = "upgrade_missing_items";
            return false;
        }
        quantityLevel = Mth.clamp(quantityLevel + 1, 0, 10);
        dirty = true;
        transientMessage = "quantity_upgraded";
        return true;
    }

    private boolean consumeItems(Player player, Item item, int cost) {
        int available = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                available += stack.getCount();
            }
        }
        if (available < cost) {
            return false;
        }
        int remaining = cost;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
            if (remaining <= 0) {
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    public int getSpeedUpgradeCost() {
        return getUpgradeCost(speedLevel);
    }

    public int getQuantityUpgradeCost() {
        return getUpgradeCost(quantityLevel);
    }

    private int getUpgradeCost(int level) {
        return 4 + level * 3;
    }

    public List<ItemStack> createItemStacks(long amount) {
        Optional<Item> optionalItem = GeneratorItems.resolve(selectedItem);
        if (optionalItem.isEmpty() || amount <= 0) {
            return List.of();
        }
        Item item = optionalItem.get();
        List<ItemStack> stacks = new ArrayList<>();
        long remaining = amount;
        while (remaining > 0) {
            int count = (int) Math.min(item.getMaxStackSize(), remaining);
            stacks.add(new ItemStack(item, count));
            remaining -= count;
        }
        return stacks;
    }

    public Set<ResourceLocation> getUnlockedItems() {
        return Collections.unmodifiableSet(unlockedItems);
    }

    public boolean isUnlocked(ResourceLocation id) {
        return unlockedItems.contains(id);
    }
}
