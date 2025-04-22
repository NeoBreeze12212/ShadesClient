package org.neo.shadesclient.mixins;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.neo.shadesclient.modules.ItemRenameModule;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemDisplayNameMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void onGetName(CallbackInfoReturnable<Text> cir) {
        // Get instance of our module
        ItemRenameModule module = ModuleManager.getModule(ItemRenameModule.class);

        if (module != null && module.isEnabled()) {
            ItemStack self = (ItemStack)(Object)this;
            Text customName = module.getCustomDisplayName(self);

            if (customName != null) {
                cir.setReturnValue(customName);
            }
        }
    }
}