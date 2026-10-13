package dev.nitro.mixin;

import dev.nitro.module.modules.NameHider;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    // require = 0: if the target signature differs on your version the mixin is skipped instead of crashing the game
    @ModifyVariable(
        method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
        at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String nitro$hideName(String text) {
        return NameHider.process(text);
    }
}
