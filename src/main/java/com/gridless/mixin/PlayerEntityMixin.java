package com.gridless.mixin;

import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow public abstract PlayerInventory getInventory();

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void gridless$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        GridlessStorage storage = (GridlessStorage) this.getInventory();
        NbtList list = new NbtList();
        for (PlacedItem item : storage.gridless$getPlacedItems()) {
            list.add(item.writeNbt(new NbtCompound()));
        }
        nbt.put("GridlessItems", list);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void gridless$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("GridlessItems", NbtElement.LIST_TYPE)) {
            GridlessStorage storage = (GridlessStorage) this.getInventory();
            storage.gridless$getPlacedItems().clear();
            NbtList list = nbt.getList("GridlessItems", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                storage.gridless$addPlacedItem(PlacedItem.readNbt(list.getCompound(i)));
            }
        }
    }
}
