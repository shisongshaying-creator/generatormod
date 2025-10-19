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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GeneratorState {
    private static final String TAG_SELECTED = "Selected";
    private static final String TAG_RUNNING = "Running";
    private static final String TAG_STORED = "Stored";
    private static final String TAG_SPEED = "Speed";
    private static final String TAG_QUANTITY = "Quantity";
    private static final String TAG_SPEED_LEVELS = "SpeedLevels";
    private static final String TAG_QUANTITY_LEVELS = "QuantityLevels";
    private static final String TAG_LEVEL_ITEM = "Item";
    private static final String TAG_LEVEL_VALUE = "Level";
    private static final String TAG_CLOUD_STORAGE = "CloudStorage";
    private static final String TAG_CLOUD_AMOUNT = "Amount";
    private static final String TAG_LAST_REAL = "LastReal";
    private static final String TAG_LEFTOVER = "Leftover";
    private static final String TAG_RUNNING_SINCE = "RunningSince";
    private static final String TAG_UNLOCKED = "Unlocked";

    public static final int MAX_LEVEL = 10;

    private ResourceLocation selectedItem;
    private boolean running;
    private long storedItems;
    private final Map<ResourceLocation, Integer> speedLevels = new HashMap<>();
    private final Map<ResourceLocation, Integer> quantityLevels = new HashMap<>();
    private final Map<ResourceLocation, Long> cloudStorage = new HashMap<>();
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
        speedLevels.clear();
        quantityLevels.clear();
        cloudStorage.clear();
        if (tag.contains(TAG_SPEED_LEVELS, Tag.TAG_LIST)) {
            ListTag speedList = tag.getList(TAG_SPEED_LEVELS, Tag.TAG_COMPOUND);
            for (int i = 0; i < speedList.size(); i++) {
                CompoundTag levelTag = speedList.getCompound(i);
                String idValue = levelTag.getString(TAG_LEVEL_ITEM);
                if (idValue.isEmpty()) {
                    continue;
                }
                int level = Mth.clamp(levelTag.getInt(TAG_LEVEL_VALUE), 0, MAX_LEVEL);
                if (level > 0) {
                    speedLevels.put(new ResourceLocation(idValue), level);
                }
            }
        }
        if (tag.contains(TAG_QUANTITY_LEVELS, Tag.TAG_LIST)) {
            ListTag quantityList = tag.getList(TAG_QUANTITY_LEVELS, Tag.TAG_COMPOUND);
            for (int i = 0; i < quantityList.size(); i++) {
                CompoundTag levelTag = quantityList.getCompound(i);
                String idValue = levelTag.getString(TAG_LEVEL_ITEM);
                if (idValue.isEmpty()) {
                    continue;
                }
                int level = Mth.clamp(levelTag.getInt(TAG_LEVEL_VALUE), 0, MAX_LEVEL);
                if (level > 0) {
                    quantityLevels.put(new ResourceLocation(idValue), level);
                }
            }
        }
        if (tag.contains(TAG_CLOUD_STORAGE, Tag.TAG_LIST)) {
            ListTag cloudList = tag.getList(TAG_CLOUD_STORAGE, Tag.TAG_COMPOUND);
            for (int i = 0; i < cloudList.size(); i++) {
                CompoundTag entryTag = cloudList.getCompound(i);
                String idValue = entryTag.getString(TAG_LEVEL_ITEM);
                if (idValue.isEmpty()) {
                    continue;
                }
                long amount = Math.max(0L, entryTag.getLong(TAG_CLOUD_AMOUNT));
                if (amount > 0L) {
                    cloudStorage.put(new ResourceLocation(idValue), amount);
                }
            }
        }
        if (selectedItem != null) {
            int legacySpeed = Mth.clamp(tag.getInt(TAG_SPEED), 0, MAX_LEVEL);
            if (legacySpeed > 0 && !speedLevels.containsKey(selectedItem)) {
                speedLevels.put(selectedItem, legacySpeed);
            }
            int legacyQuantity = Mth.clamp(tag.getInt(TAG_QUANTITY), 0, MAX_LEVEL);
            if (legacyQuantity > 0 && !quantityLevels.containsKey(selectedItem)) {
                quantityLevels.put(selectedItem, legacyQuantity);
            }
        }
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
        tag.remove(TAG_SPEED);
        tag.remove(TAG_QUANTITY);
        ListTag speedList = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : speedLevels.entrySet()) {
            int level = Mth.clamp(entry.getValue(), 0, MAX_LEVEL);
            if (level <= 0) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_LEVEL_ITEM, entry.getKey().toString());
            entryTag.putInt(TAG_LEVEL_VALUE, level);
            speedList.add(entryTag);
        }
        tag.put(TAG_SPEED_LEVELS, speedList);
        ListTag quantityList = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : quantityLevels.entrySet()) {
            int level = Mth.clamp(entry.getValue(), 0, MAX_LEVEL);
            if (level <= 0) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_LEVEL_ITEM, entry.getKey().toString());
            entryTag.putInt(TAG_LEVEL_VALUE, level);
            quantityList.add(entryTag);
        }
        tag.put(TAG_QUANTITY_LEVELS, quantityList);
        tag.putLong(TAG_LAST_REAL, lastRealTime);
        tag.putLong(TAG_LEFTOVER, leftoverMillis);
        tag.putLong(TAG_RUNNING_SINCE, runningSince);
        ListTag listTag = new ListTag();
        for (ResourceLocation id : unlockedItems) {
            listTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put(TAG_UNLOCKED, listTag);
        ListTag cloudList = new ListTag();
        for (Map.Entry<ResourceLocation, Long> entry : cloudStorage.entrySet()) {
            long amount = entry.getValue();
            if (amount <= 0L) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_LEVEL_ITEM, entry.getKey().toString());
            entryTag.putLong(TAG_CLOUD_AMOUNT, amount);
            cloudList.add(entryTag);
        }
        tag.put(TAG_CLOUD_STORAGE, cloudList);
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
            int unlockCost = GeneratorItems.getUnlockCost(item);
            if (!consumeItems(player, item, unlockCost)) {
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
        return getSpeedLevel(selectedItem);
    }

    public int getQuantityLevel() {
        return getQuantityLevel(selectedItem);
    }

    public Map<ResourceLocation, Integer> getSpeedLevels() {
        return Collections.unmodifiableMap(speedLevels);
    }

    public Map<ResourceLocation, Integer> getQuantityLevels() {
        return Collections.unmodifiableMap(quantityLevels);
    }

    public long getIntervalMillis() {
        if (selectedItem == null) {
            return 0L;
        }
        Optional<Item> optionalItem = GeneratorItems.resolve(selectedItem);
        int level = getSpeedLevel(selectedItem);
        return optionalItem.map(item -> GeneratorItems.computeIntervalMillis(item, level)).orElse(0L);
    }

    public int getAmountPerCycle() {
        return GeneratorItems.computeAmountPerCycle(getQuantityLevel(selectedItem));
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
        return upgradeSpeed(player, selectedItem);
    }

    public boolean upgradeSpeed(Player player, ResourceLocation id) {
        if (id == null) {
            transientMessage = "no_selection";
            return false;
        }
        int currentLevel = getSpeedLevel(id);
        if (currentLevel >= MAX_LEVEL) {
            transientMessage = "speed_max_level";
            return false;
        }

        int cost = getUpgradeCost(currentLevel);
        Optional<Item> optionalItem = GeneratorItems.resolve(id);
        if (optionalItem.isEmpty()) {
            transientMessage = "invalid_item";
            return false;
        }
        if (!consumeItems(player, optionalItem.get(), cost)) {
            transientMessage = "upgrade_missing_items";
            return false;
        }
        int newLevel = Mth.clamp(currentLevel + 1, 0, MAX_LEVEL);
        setSpeedLevel(id, newLevel);
        dirty = true;
        transientMessage = "speed_upgraded";
        return true;
    }

    public boolean upgradeQuantity(Player player) {
        return upgradeQuantity(player, selectedItem);
    }

    public boolean upgradeQuantity(Player player, ResourceLocation id) {
        if (id == null) {
            transientMessage = "no_selection";
            return false;
        }
        int currentLevel = getQuantityLevel(id);
        if (currentLevel >= MAX_LEVEL) {
            transientMessage = "quantity_max_level";
            return false;
        }

        int cost = getUpgradeCost(currentLevel);
        Optional<Item> optionalItem = GeneratorItems.resolve(id);
        if (optionalItem.isEmpty()) {
            transientMessage = "invalid_item";
            return false;
        }
        if (!consumeItems(player, optionalItem.get(), cost)) {
            transientMessage = "upgrade_missing_items";
            return false;
        }
        int newLevel = Mth.clamp(currentLevel + 1, 0, MAX_LEVEL);
        setQuantityLevel(id, newLevel);
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
        return getUpgradeCost(getSpeedLevel());
    }

    public int getQuantityUpgradeCost() {
        return getUpgradeCost(getQuantityLevel());
    }

    private int getUpgradeCost(int level) {
        return 4 + level * 3;
    }

    public long depositToCloud(ResourceLocation id, long amount) {
        if (id == null || amount <= 0L) {
            return 0L;
        }
        Optional<Item> optionalItem = GeneratorItems.resolve(id);
        if (optionalItem.isEmpty()) {
            return 0L;
        }
        long current = cloudStorage.getOrDefault(id, 0L);
        long deposited;
        try {
            deposited = Math.addExact(current, amount);
        } catch (ArithmeticException e) {
            deposited = Long.MAX_VALUE;
        }
        cloudStorage.put(id, deposited);
        dirty = true;
        return deposited - current;
    }

    public List<ItemStack> withdrawFromCloud(ResourceLocation id, long requestedAmount) {
        if (id == null || requestedAmount <= 0L) {
            return List.of();
        }
        Optional<Item> optionalItem = GeneratorItems.resolve(id);
        if (optionalItem.isEmpty()) {
            return List.of();
        }
        long available = cloudStorage.getOrDefault(id, 0L);
        long toWithdraw = Math.min(available, requestedAmount);
        if (toWithdraw <= 0L) {
            return List.of();
        }
        long remaining = available - toWithdraw;
        if (remaining > 0L) {
            cloudStorage.put(id, remaining);
        } else {
            cloudStorage.remove(id);
        }
        dirty = true;
        return createItemStacks(optionalItem.get(), toWithdraw);
    }

    public Map<ResourceLocation, Long> getCloudStorageSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(cloudStorage));
    }

    public long getCloudStored(ResourceLocation id) {
        if (id == null) {
            return 0L;
        }
        return cloudStorage.getOrDefault(id, 0L);
    }

    private List<ItemStack> createItemStacks(Item item, long amount) {
        if (amount <= 0L) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        long remaining = amount;
        while (remaining > 0L) {
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

    public int getSpeedLevel(ResourceLocation id) {
        if (id == null) {
            return 0;
        }
        return speedLevels.getOrDefault(id, 0);
    }

    private void setSpeedLevel(ResourceLocation id, int level) {
        if (id == null) {
            return;
        }
        if (level <= 0) {
            speedLevels.remove(id);
        } else {
            speedLevels.put(id, level);
        }
    }

    public int getQuantityLevel(ResourceLocation id) {
        if (id == null) {
            return 0;
        }
        return quantityLevels.getOrDefault(id, 0);
    }

    private void setQuantityLevel(ResourceLocation id, int level) {
        if (id == null) {
            return;
        }
        if (level <= 0) {
            quantityLevels.remove(id);
        } else {
            quantityLevels.put(id, level);
        }
    }
}
