package com.gridless.mixin;

import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements GridlessStorage {
    @Shadow public PlayerEntity player;

    @Unique
    private List<PlacedItem> gridlessItems = new ArrayList<>();

    @Override
    public List<PlacedItem> gridless$getPlacedItems() {
        return this.gridlessItems;
    }

    @Override
    public void gridless$setPlacedItems(List<PlacedItem> items) {
        this.gridlessItems = new ArrayList<>(items);
    }

    @Override
    public void gridless$addPlacedItem(PlacedItem item) {
        this.gridlessItems.add(item);
    }

    @Override
    public void gridless$removePlacedItem(PlacedItem item) {
        this.gridlessItems.remove(item);
    }

    @Inject(method = "dropAll", at = @At("HEAD"))
    public void gridless$dropAll(CallbackInfo ci) {
        for (PlacedItem item : this.gridlessItems) {
            this.player.dropItem(item.getStack(), true, false);
        }
        this.gridlessItems.clear();
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    public void gridless$insertStack(net.minecraft.item.ItemStack stack, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty()) {
            for (PlacedItem item : this.gridlessItems) {
                if (net.minecraft.item.ItemStack.canCombine(item.getStack(), stack)) {
                    int space = item.getStack().getMaxCount() - item.getStack().getCount();
                    int amountToInsert = Math.min(stack.getCount(), space);
                    if (amountToInsert > 0) {
                        item.getStack().increment(amountToInsert);
                        stack.decrement(amountToInsert);
                        
                        if (this.player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                            com.gridless.network.GridlessNetwork.syncToClient(serverPlayer, true, this.gridlessItems);
                        }
                    }
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }
    @Shadow @org.spongepowered.asm.mixin.Final public net.minecraft.util.collection.DefaultedList<net.minecraft.item.ItemStack> main;

    @Inject(method = "updateItems", at = @At("HEAD"))
    public void gridless$updateItems(CallbackInfo ci) {
        if (this.player.getWorld().isClient) return; // Only process on server to avoid desync
        boolean changed = false;
        for (int i = 9; i < 36; i++) {
            net.minecraft.item.ItemStack stack = this.main.get(i);
            if (!stack.isEmpty()) {
                // Rescue item and place it on gridless area randomly
                int randomX = 8 + (int)(Math.random() * 140);
                int randomY = 84 + (int)(Math.random() * 30);
                this.gridlessItems.add(new PlacedItem(stack.copy(), randomX, randomY));
                
                // Clear the slot
                stack.setCount(0);
                changed = true;
            }
        }
        if (changed && this.player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            com.gridless.network.GridlessNetwork.syncToClient(serverPlayer, true, this.gridlessItems);
        }
    }
}
