package dev.tr7zw.exordium.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.tr7zw.exordium.ExordiumModBase;
import dev.tr7zw.exordium.util.BufferedComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow
    private Minecraft minecraft;
    @Shadow
    private List<GuiMessage.Line> trimmedMessages;
    @Shadow
    private int chatScrollbarPos;
    
    private int lastScrollbarPos = 0;
    private int messageCount = 0;
    private boolean wasFocused = false;
    
    boolean outdated = false;
    
    private BufferedComponent bufferedComponent = new BufferedComponent(ExordiumModBase.instance.config.chatSettings) {
        
        @Override
        public boolean needsRender() {
            return outdated;
        }

        @Override
        public void captureState() {
            lastScrollbarPos = chatScrollbarPos;
            messageCount = trimmedMessages.size();
            wasFocused = isChatFocused();
        }
    };
    
    public boolean hasChanged(int i) {
        boolean changed = chatScrollbarPos != lastScrollbarPos || messageCount != trimmedMessages.size() || isChatFocused() != wasFocused;
        if(changed) {
            return true;
        }
        int j = getLinesPerPage();
        for (int o = 0; o + this.chatScrollbarPos < this.trimmedMessages.size() && o < j; o++) {
            GuiMessage.Line guiMessage = this.trimmedMessages.get(o + this.chatScrollbarPos);
            if (guiMessage != null) {
                int p = i - guiMessage.addedTime();
                if (p > 170 && p < 200) { // 180 is correct, add a tiny buffer for the frame to catch up
                        return true;
                }
            }
        }
        return false;
    }
    
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack poseStack, int tickCount, int j, int k, CallbackInfo ci) {
        outdated = hasChanged(tickCount);
        if(bufferedComponent.render()) {
            ci.cancel();
            return;
        }
    }
    
    @Inject(method = "render", at = @At("RETURN"))
    public void renderEnd(PoseStack poseStack, int tickCount, int j, int k, CallbackInfo ci) {
        bufferedComponent.renderEnd();
    }

    @Shadow
    public abstract boolean isChatFocused();
    
    @Shadow
    public abstract boolean isChatHidden();
    
    @Shadow
    public abstract int getLinesPerPage();
    
}
