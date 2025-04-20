package org.neo.shadesclient.mixins;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.neo.shadesclient.events.EventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(GameRenderer.class)
public class WorldRenderMixin {
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void afterRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // Use a default value for partial ticks if we can't access it
        float partialTicks = 0.0f;

        // Try to get the actual value using reflection
        try {
            java.lang.reflect.Field field = RenderTickCounter.class.getDeclaredField("tickDelta");
            field.setAccessible(true);
            partialTicks = field.getFloat(tickCounter);
        } catch (Exception e) {
            // Fall back to default if reflection fails
            // You could log this error if needed
        }

        MatrixStack matrix = new MatrixStack();
        EventHandler.onRenderWorld(matrix, partialTicks);
    }
}