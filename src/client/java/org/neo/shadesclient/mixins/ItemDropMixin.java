package org.neo.shadesclient.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.neo.shadesclient.modules.InventoryLockModule;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ItemDropMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onItemDrop(boolean entireStack, CallbackInfoReturnable<Boolean> ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        InventoryLockModule module = ModuleManager.getModule(InventoryLockModule.class);
        if (module != null && module.isEnabled() && module.isPreventDropping()) {
            int currentSlot = client.player.getInventory().selectedSlot;

            if (module.isSlotLocked(currentSlot)) {
                // Cancel the drop action
                ci.cancel();

                // Notify player
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("§cCannot drop items from locked slots!"),
                        true
                );
            }
        }
    }
}