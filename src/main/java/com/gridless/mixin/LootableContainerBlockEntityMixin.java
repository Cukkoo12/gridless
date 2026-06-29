package com.gridless.mixin;

import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LootableContainerBlockEntity.class)
public abstract class LootableContainerBlockEntityMixin implements GridlessStorage {
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

}
