package com.gridless.mixin.client;

import com.gridless.storage.GridlessStorage;
import com.gridless.storage.PlacedItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected T handler;
    @Shadow protected Slot focusedSlot;

    protected HandledScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }

    @Unique
    private Slot gridless$getReferenceSlot() {
        if (this.handler == null || this.handler.slots == null) return null;
        for (Slot slot : this.handler.slots) {
            if (slot.inventory instanceof PlayerInventory && slot.getIndex() == 9) {
                return slot;
            }
        }
        return null;
    }

    @Unique
    private boolean isGridlessSlot(Slot slot) {
        return slot != null && slot.inventory instanceof PlayerInventory && slot.getIndex() >= 9 && slot.getIndex() < 36;
    }

    @Unique
    private long gridless$lastClickTime = 0;
    @Unique
    private double gridless$lastClickX = 0;
    @Unique
    private double gridless$lastClickY = 0;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void gridless$hideSlots(DrawContext context, Slot slot, CallbackInfo ci) {
        if (isGridlessSlot(slot)) {
            ci.cancel(); // Hide gridless slots
        }
    }

    @Inject(method = "isPointOverSlot", at = @At("HEAD"), cancellable = true)
    private void gridless$preventSlotInteraction(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (isGridlessSlot(slot)) {
            cir.setReturnValue(false); // Completely disable hovering, clicking, and tooltips for hidden vanilla slots
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V"))
    private void gridless$renderPlacedItems(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client == null || this.client.player == null) return;
        Slot refSlot = gridless$getReferenceSlot();
        if (refSlot == null) return;

        // Draw giant slot background
        int minX = refSlot.x - 1;
        int minY = refSlot.y - 1;
        int maxX = minX + 162;
        int maxY = minY + 54;
        
        context.fill(minX, minY, maxX, maxY, 0xFF8B8B8B); // Inner dark gray
        context.fill(minX, minY, maxX - 1, minY + 1, 0xFF373737); // Top border
        context.fill(minX, minY, minX + 1, maxY - 1, 0xFF373737); // Left border
        context.fill(minX, maxY - 1, maxX, maxY, 0xFFFFFFFF); // Bottom border
        context.fill(maxX - 1, minY, maxX, maxY, 0xFFFFFFFF); // Right border

        GridlessStorage storage = (GridlessStorage) this.client.player.getInventory();
        int offsetX = refSlot.x - 8;
        int offsetY = refSlot.y - 84;

        List<PlacedItem> items = storage.gridless$getPlacedItems();
        for (PlacedItem item : items) {
            int localX = (int) item.getX() + offsetX;
            int localY = (int) item.getY() + offsetY;
            int drawX = this.x + localX;
            int drawY = this.y + localY;
            boolean hovered = mouseX >= drawX && mouseX < drawX + 16 && mouseY >= drawY && mouseY < drawY + 16;
            
            if (hovered) {
                context.getMatrices().push();
                context.getMatrices().translate(localX + 8, localY + 8, 0);
                context.getMatrices().scale(1.15f, 1.15f, 1.0f);
                context.getMatrices().translate(-(localX + 8), -(localY + 8), 0);
                context.drawItem(item.getStack(), localX, localY);
                context.drawItemInSlot(this.client.textRenderer, item.getStack(), localX, localY);
                context.getMatrices().pop();
            } else {
                context.drawItem(item.getStack(), localX, localY);
                context.drawItemInSlot(this.client.textRenderer, item.getStack(), localX, localY);
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void gridless$renderTooltips(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client == null || this.client.player == null) return;
        if (!this.handler.getCursorStack().isEmpty()) return;

        Slot refSlot = gridless$getReferenceSlot();
        if (refSlot == null) return;

        GridlessStorage storage = (GridlessStorage) this.client.player.getInventory();
        int offsetX = refSlot.x - 8;
        int offsetY = refSlot.y - 84;

        List<PlacedItem> items = storage.gridless$getPlacedItems();
        for (int i = items.size() - 1; i >= 0; i--) {
            PlacedItem item = items.get(i);
            int localX = (int) item.getX() + offsetX;
            int localY = (int) item.getY() + offsetY;
            int drawX = this.x + localX;
            int drawY = this.y + localY;
            if (mouseX >= drawX && mouseX < drawX + 16 && mouseY >= drawY && mouseY < drawY + 16) {
                context.drawItemTooltip(this.client.textRenderer, item.getStack(), mouseX, mouseY);
                break;
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void gridless$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.client == null || this.client.player == null) return;
        Slot refSlot = gridless$getReferenceSlot();
        if (refSlot == null) return;

        GridlessStorage storage = (GridlessStorage) this.client.player.getInventory();
        int offsetX = refSlot.x - 8;
        int offsetY = refSlot.y - 84;

        ItemStack cursorStack = this.handler.getCursorStack();
        List<PlacedItem> items = storage.gridless$getPlacedItems();

        long currentTime = net.minecraft.util.Util.getMeasuringTimeMs();
        boolean isDoubleClick = currentTime - gridless$lastClickTime < 250L && Math.abs(mouseX - gridless$lastClickX) < 5 && Math.abs(mouseY - gridless$lastClickY) < 5 && button == 0;
        gridless$lastClickTime = currentTime;
        gridless$lastClickX = mouseX;
        gridless$lastClickY = mouseY;

        if (isDoubleClick && !cursorStack.isEmpty()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(true); // Always PlayerInventory now
            ClientPlayNetworking.send(com.gridless.network.GridlessNetwork.GATHER_ITEMS, buf);
            
            for (int j = items.size() - 1; j >= 0; j--) {
                PlacedItem gatherItem = items.get(j);
                if (ItemStack.canCombine(cursorStack, gatherItem.getStack())) {
                    int space = cursorStack.getMaxCount() - cursorStack.getCount();
                    int amount = Math.min(space, gatherItem.getStack().getCount());
                    if (amount > 0) {
                        cursorStack.increment(amount);
                        gatherItem.getStack().decrement(amount);
                        if (gatherItem.getStack().isEmpty()) {
                            items.remove(j);
                        }
                    }
                    if (cursorStack.getCount() == cursorStack.getMaxCount()) break;
                }
            }
            this.client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
            cir.setReturnValue(true);
            return;
        }

        for (int i = items.size() - 1; i >= 0; i--) {
            PlacedItem item = items.get(i);
            int localX = (int) item.getX() + offsetX;
            int localY = (int) item.getY() + offsetY;
            int drawX = this.x + localX;
            int drawY = this.y + localY;
            if (mouseX >= drawX && mouseX < drawX + 16 && mouseY >= drawY && mouseY < drawY + 16) {
                if (cursorStack.isEmpty() || (ItemStack.canCombine(cursorStack, item.getStack()) && cursorStack.getCount() < cursorStack.getMaxCount())) {
                    int amountToPick = button == 1 ? (item.getStack().getCount() + 1) / 2 : item.getStack().getCount();
                    if (!cursorStack.isEmpty()) {
                        int space = cursorStack.getMaxCount() - cursorStack.getCount();
                        amountToPick = Math.min(amountToPick, space);
                    }
                    
                    if (amountToPick > 0) {
                        if (cursorStack.isEmpty()) {
                            ItemStack newCursor = item.getStack().copy();
                            newCursor.setCount(amountToPick);
                            this.handler.setCursorStack(newCursor);
                        } else {
                            cursorStack.increment(amountToPick);
                        }
                        
                        item.getStack().decrement(amountToPick);
                        
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(true);
                        buf.writeInt(i);
                        buf.writeInt(button);
                        ClientPlayNetworking.send(com.gridless.network.GridlessNetwork.PICKUP_ITEM, buf);
                        
                        if (item.getStack().isEmpty()) {
                            items.remove(i);
                        }
                        this.client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                    }
                    cir.setReturnValue(true);
                    return;
                } else if (!cursorStack.isEmpty() && ItemStack.canCombine(cursorStack, item.getStack()) && item.getStack().getCount() < item.getStack().getMaxCount()) {
                    int amountToPlace = button == 1 ? 1 : cursorStack.getCount();
                    int space = item.getStack().getMaxCount() - item.getStack().getCount();
                    amountToPlace = Math.min(amountToPlace, space);
                    
                    if (amountToPlace > 0) {
                        cursorStack.decrement(amountToPlace);
                        item.getStack().increment(amountToPlace);
                        
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(true);
                        buf.writeInt(i);
                        buf.writeInt(amountToPlace);
                        ClientPlayNetworking.send(com.gridless.network.GridlessNetwork.MERGE_ITEM, buf);
                        this.client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                    }
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        double localX = mouseX - this.x;
        double localY = mouseY - this.y;

        int minX = refSlot.x - 1;
        int maxX = refSlot.x + 161;
        int minY = refSlot.y - 1;
        int maxY = refSlot.y + 53;

        if (localX >= minX && localX <= maxX && localY >= minY && localY <= maxY) {
            if (cursorStack.isEmpty()) {
                cir.setReturnValue(true);
            } else {
                int amountToPlace = button == 1 ? 1 : cursorStack.getCount();
                ItemStack stackToPlace = cursorStack.copy();
                stackToPlace.setCount(amountToPlace);
                
                // Subtract offset so item is saved relative to standard 8,84
                float savedX = (float) (localX - 8 - offsetX);
                float savedY = (float) (localY - 8 - offsetY);
                
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(true);
                buf.writeItemStack(stackToPlace);
                buf.writeFloat(savedX);
                buf.writeFloat(savedY);
                ClientPlayNetworking.send(com.gridless.network.GridlessNetwork.PLACE_NEW_ITEM, buf);
                
                items.add(new PlacedItem(stackToPlace, savedX, savedY));
                cursorStack.decrement(amountToPlace);
                this.client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                cir.setReturnValue(true);
            }
        }
    }
}
