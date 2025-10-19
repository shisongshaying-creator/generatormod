package com.example.generatormod.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ClientCloudStorageState {
    public static final ClientCloudStorageState INSTANCE = new ClientCloudStorageState();

    private final Map<ResourceLocation, Long> storedItems = new HashMap<>();

    private ClientCloudStorageState() {
    }

    public void apply(Map<ResourceLocation, Long> storage) {
        this.storedItems.clear();
        this.storedItems.putAll(storage);
    }

    public Map<ResourceLocation, Long> getStoredItems() {
        return Collections.unmodifiableMap(storedItems);
    }

    public long get(ResourceLocation id) {
        if (id == null) {
            return 0L;
        }
        return storedItems.getOrDefault(id, 0L);
    }

    public boolean isEmpty() {
        return storedItems.isEmpty();
    }
}
