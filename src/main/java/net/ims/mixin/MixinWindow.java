package net.ims.mixin;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {
    @Unique
    private boolean supportsAdaptiveSync;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setMode()V"))
    private void setSupportsAdaptiveSync(WindowEventHandler $$0, ScreenManager $$1, DisplayData $$2, String $$3, String $$4, CallbackInfo ci) {
       supportsAdaptiveSync = (GLFW.glfwExtensionSupported("GLX_EXT_swap_control_tear") || GLFW.glfwExtensionSupported("WGL_EXT_swap_control_tear"));
       System.out.println(supportsAdaptiveSync ? "This context supports Adaptive VSync!" : "This context does not support adaptive VSync.");
    }

    @Redirect(method = "updateVsync", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V"))
    private void setSwapInterval(int interval) {
        if (supportsAdaptiveSync) {
            GLFW.glfwSwapInterval(interval == 1 ? -1 : 0);
        } else {
            GLFW.glfwSwapInterval(interval);
        }
    }
}
