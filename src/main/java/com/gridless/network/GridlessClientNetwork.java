package com.gridless.network;

import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class GridlessClientNetwork {
    public static void registerS2C() {
        ClientPlayNetworking.registerGlobalReceiver(GridlessNetwork.SYNC_ITEMS, (client, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            int count = buf.readInt();
            List<PlacedItem> items = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ItemStack stack = buf.readItemStack();
                float x = buf.readFloat();
                float y = buf.readFloat();
                items.add(new PlacedItem(stack, x, y));
            }

            client.execute(() -> {
                if (client.player == null) return;
                GridlessStorage storage;
                if (isPlayerInventory) {
                    storage = (GridlessStorage) client.player.getInventory();
                } else {
                    // For chests, we need to access the open screen's inventory.
                    // This is complex, skip chest for now or implement generic interface.
                    storage = null;
                }

                if (storage != null) {
                    storage.gridless$setPlacedItems(items);
                }
            });
        });
    }
}
