package com.gridless.network;

import com.gridless.GridlessMod;
import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GridlessNetwork {
    public static final Identifier SYNC_ITEMS = GridlessMod.id("sync_items");
    public static final Identifier PLACE_NEW_ITEM = GridlessMod.id("place_new_item");
    public static final Identifier MERGE_ITEM = GridlessMod.id("merge_item");
    public static final Identifier PICKUP_ITEM = GridlessMod.id("pickup_item");
    public static final Identifier GATHER_ITEMS = GridlessMod.id("gather_items");
    public static final Identifier SHIFT_CLICK_ITEM = GridlessMod.id("shift_click_item");
    public static final Identifier DROP_ITEM = GridlessMod.id("drop_item");
    public static final Identifier SWAP_HOTBAR = GridlessMod.id("swap_hotbar");
    public static final Identifier AUTO_SORT = GridlessMod.id("auto_sort");
    public static final Identifier PAINT_ITEM = GridlessMod.id("paint_item");

    public static void registerC2S() {
        ServerPlayNetworking.registerGlobalReceiver(PLACE_NEW_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            ItemStack stack = buf.readItemStack();
            float x = buf.readFloat();
            float y = buf.readFloat();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    ItemStack serverCursor = player.currentScreenHandler.getCursorStack();
                    if (!serverCursor.isEmpty() && ItemStack.canCombine(serverCursor, stack) && serverCursor.getCount() >= stack.getCount()) {
                        serverCursor.decrement(stack.getCount());
                        storage.gridless$addPlacedItem(new PlacedItem(stack.copy(), x, y));
                        syncToClient(player, isPlayerInventory, storage.gridless$getPlacedItems());
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(MERGE_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            int index = buf.readInt();
            int amount = buf.readInt();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    if (index >= 0 && index < items.size()) {
                        PlacedItem item = items.get(index);
                        ItemStack serverCursor = player.currentScreenHandler.getCursorStack();
                        if (!serverCursor.isEmpty() && ItemStack.canCombine(serverCursor, item.getStack()) && serverCursor.getCount() >= amount) {
                            serverCursor.decrement(amount);
                            item.getStack().increment(amount);
                            syncToClient(player, isPlayerInventory, items);
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PICKUP_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            int index = buf.readInt();
            int button = buf.readInt();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    if (index >= 0 && index < items.size()) {
                        PlacedItem item = items.get(index);
                        ItemStack serverCursor = player.currentScreenHandler.getCursorStack();
                        
                        int amountToPick = button == 1 ? (item.getStack().getCount() + 1) / 2 : item.getStack().getCount();
                        
                        if (serverCursor.isEmpty()) {
                            ItemStack copy = item.getStack().copy();
                            copy.setCount(amountToPick);
                            player.currentScreenHandler.setCursorStack(copy);
                        } else if (ItemStack.canCombine(serverCursor, item.getStack())) {
                            int space = serverCursor.getMaxCount() - serverCursor.getCount();
                            amountToPick = Math.min(amountToPick, space);
                            serverCursor.increment(amountToPick);
                        } else {
                            return; // Cannot merge
                        }

                        item.getStack().decrement(amountToPick);
                        if (item.getStack().isEmpty()) {
                            items.remove(index);
                        }
                        syncToClient(player, isPlayerInventory, items);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(GATHER_ITEMS, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    ItemStack serverCursor = player.currentScreenHandler.getCursorStack();
                    if (!serverCursor.isEmpty()) {
                        for (int i = items.size() - 1; i >= 0; i--) {
                            PlacedItem item = items.get(i);
                            if (ItemStack.canCombine(serverCursor, item.getStack())) {
                                int space = serverCursor.getMaxCount() - serverCursor.getCount();
                                int amount = Math.min(space, item.getStack().getCount());
                                if (amount > 0) {
                                    serverCursor.increment(amount);
                                    item.getStack().decrement(amount);
                                    if (item.getStack().isEmpty()) {
                                        items.remove(i);
                                    }
                                }
                                if (serverCursor.getCount() == serverCursor.getMaxCount()) break;
                            }
                        }
                        syncToClient(player, isPlayerInventory, items);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SHIFT_CLICK_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            int index = buf.readInt();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    if (index >= 0 && index < items.size()) {
                        PlacedItem item = items.get(index);
                        ItemStack stack = item.getStack();
                        boolean inserted = false;
                        
                        // First pass: try to merge into existing stacks
                        for (net.minecraft.screen.slot.Slot slot : player.currentScreenHandler.slots) {
                            if (slot.inventory != player.getInventory() || slot.getIndex() < 9) { // Chest slots or Hotbar
                                ItemStack slotStack = slot.getStack();
                                if (!slotStack.isEmpty() && ItemStack.canCombine(slotStack, stack)) {
                                    int space = slot.getMaxItemCount(stack) - slotStack.getCount();
                                    if (space > 0) {
                                        int amount = Math.min(space, stack.getCount());
                                        slotStack.increment(amount);
                                        stack.decrement(amount);
                                        slot.markDirty();
                                        inserted = true;
                                        if (stack.isEmpty()) break;
                                    }
                                }
                            }
                        }
                        
                        // Second pass: try to place in empty slots
                        if (!stack.isEmpty()) {
                            for (net.minecraft.screen.slot.Slot slot : player.currentScreenHandler.slots) {
                                if (slot.inventory != player.getInventory() || slot.getIndex() < 9) {
                                    ItemStack slotStack = slot.getStack();
                                    if (slotStack.isEmpty() && slot.canInsert(stack)) {
                                        int space = slot.getMaxItemCount(stack);
                                        int amount = Math.min(space, stack.getCount());
                                        slot.setStack(stack.split(amount));
                                        slot.markDirty();
                                        inserted = true;
                                        if (stack.isEmpty()) break;
                                    }
                                }
                            }
                        }

                        if (inserted) {
                            if (stack.isEmpty()) {
                                items.remove(index);
                            }
                            syncToClient(player, isPlayerInventory, items);
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DROP_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            int index = buf.readInt();
            boolean dropAll = buf.readBoolean();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    if (index >= 0 && index < items.size()) {
                        PlacedItem item = items.get(index);
                        ItemStack toDrop;
                        if (dropAll) {
                            toDrop = item.getStack().copy();
                            item.getStack().setCount(0);
                        } else {
                            toDrop = item.getStack().split(1);
                        }
                        
                        player.dropItem(toDrop, false, true);
                        
                        if (item.getStack().isEmpty()) {
                            items.remove(index);
                        }
                        syncToClient(player, isPlayerInventory, items);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SWAP_HOTBAR, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            int index = buf.readInt();
            int hotbarSlot = buf.readInt();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null && hotbarSlot >= 0 && hotbarSlot < 9) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    if (index >= 0 && index < items.size()) {
                        PlacedItem item = items.get(index);
                        ItemStack currentGridless = item.getStack().copy();
                        ItemStack currentHotbar = player.getInventory().getStack(hotbarSlot).copy();

                        player.getInventory().setStack(hotbarSlot, currentGridless);
                        if (currentHotbar.isEmpty()) {
                            items.remove(index);
                        } else {
                            item.setStack(currentHotbar);
                        }
                        syncToClient(player, isPlayerInventory, items);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AUTO_SORT, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    List<PlacedItem> items = storage.gridless$getPlacedItems();
                    
                    java.util.Map<String, List<PlacedItem>> grouped = new java.util.HashMap<>();
                    for (PlacedItem item : items) {
                        String name = net.minecraft.registry.Registries.ITEM.getId(item.getStack().getItem()).toString();
                        grouped.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(item);
                    }
                    
                    int currentX = 0;
                    int currentY = 0;
                    
                    for (List<PlacedItem> group : grouped.values()) {
                        for (PlacedItem item : group) {
                            item.setX((float) currentX);
                            item.setY((float) currentY);
                        }
                        
                        currentX += 18;
                        if (currentX > 162 - 18) {
                            currentX = 0;
                            currentY += 18;
                        }
                    }
                    syncToClient(player, isPlayerInventory, items);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PAINT_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean isPlayerInventory = buf.readBoolean();
            float x = buf.readFloat();
            float y = buf.readFloat();

            server.execute(() -> {
                GridlessStorage storage = isPlayerInventory ? (GridlessStorage) player.getInventory() : null;
                if (storage != null) {
                    ItemStack serverCursor = player.currentScreenHandler.getCursorStack();
                    if (!serverCursor.isEmpty()) {
                        ItemStack stackToPlace = serverCursor.copy();
                        stackToPlace.setCount(1);
                        serverCursor.decrement(1);
                        storage.gridless$addPlacedItem(new PlacedItem(stackToPlace, x, y));
                        syncToClient(player, isPlayerInventory, storage.gridless$getPlacedItems());
                    }
                }
            });
        });
    }

    public static void syncToClient(ServerPlayerEntity player, boolean isPlayerInventory, List<PlacedItem> items) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isPlayerInventory);
        buf.writeInt(items.size());
        for (PlacedItem item : items) {
            buf.writeItemStack(item.getStack());
            buf.writeFloat(item.getX());
            buf.writeFloat(item.getY());
        }
        ServerPlayNetworking.send(player, SYNC_ITEMS, buf);
    }
}
