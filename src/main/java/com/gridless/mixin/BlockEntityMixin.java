package com.gridless.mixin;

import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "writeNbt", at = @At("RETURN"))
    protected void gridless$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if ((Object) this instanceof GridlessStorage) {
            GridlessStorage storage = (GridlessStorage) (Object) this;
            if (storage.gridless$getPlacedItems() != null) {
                NbtList list = new NbtList();
                for (PlacedItem item : storage.gridless$getPlacedItems()) {
                    list.add(item.writeNbt(new NbtCompound()));
                }
                nbt.put("GridlessItems", list);
            }
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    protected void gridless$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if ((Object) this instanceof GridlessStorage) {
            if (nbt.contains("GridlessItems", NbtElement.LIST_TYPE)) {
                GridlessStorage storage = (GridlessStorage) (Object) this;
                if (storage.gridless$getPlacedItems() != null) {
                    storage.gridless$getPlacedItems().clear();
                    NbtList list = nbt.getList("GridlessItems", NbtElement.COMPOUND_TYPE);
                    for (int i = 0; i < list.size(); i++) {
                        storage.gridless$addPlacedItem(PlacedItem.readNbt(list.getCompound(i)));
                    }
                }
            }
        }
    }
}
