package com.gridless.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class PlacedItem {
    private ItemStack stack;
    private float x;
    private float y;

    public PlacedItem(ItemStack stack, float x, float y) {
        this.stack = stack;
        this.x = x;
        this.y = y;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("Item", stack.writeNbt(new NbtCompound()));
        nbt.putFloat("X", x);
        nbt.putFloat("Y", y);
        return nbt;
    }

    public static PlacedItem readNbt(NbtCompound nbt) {
        ItemStack stack = ItemStack.fromNbt(nbt.getCompound("Item"));
        float x = nbt.getFloat("X");
        float y = nbt.getFloat("Y");
        return new PlacedItem(stack, x, y);
    }
}
