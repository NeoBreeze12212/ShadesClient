package org.neo.shadesclient.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.neo.shadesclient.modules.InventoryLockModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class InventoryLockMixin {

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot == null) return;

        InventoryLockModule module = ModuleManager.getModule(InventoryLockModule.class);
        if (module != null && module.isEnabled()) {
            // Check if this is a hotbar slot (0-8) and if it's locked
            int inventorySlot = slot.getIndex();
            if (inventorySlot >= 0 && inventorySlot < 9 && module.isSlotLocked(inventorySlot)) {
                // Cancel the click event
                ci.cancel();

                // Notify player
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(
                            net.minecraft.text.Text.literal("§cThis slot is locked!"),
                            true
                    );
                }
            }
        }
    }
}