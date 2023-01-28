package a7.tweakception.mixin;

import a7.tweakception.LagSpikeWatcher;
import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static a7.tweakception.utils.McUtils.getPlayer;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
    @Shadow
    public MovingObjectPosition objectMouseOver;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public EntityPlayerSP thePlayer;
    @Shadow
    public WorldClient theWorld;
    @Shadow
    public EntityRenderer entityRenderer;
    @Final
    @Shadow
    private static Logger logger;
    @Shadow
    private int leftClickCounter;
    
    private boolean inAutoSwap = false;
    
    @Shadow
    public abstract void rightClickMouse();
    
    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void runGameLoop(CallbackInfo ci)
    {
        LagSpikeWatcher.newTick();
    }
    
    @Inject(method = "rightClickMouse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;isPlayerRightClickingOnEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/MovingObjectPosition;)Z"),
        cancellable = true)
    private void rightClickOnEntity(CallbackInfo ci)
    {
//        sendChat("rclick entity");
        int slot = -1;
        Runnable afterInject = () ->
        {
            boolean flag = true;
            if (this.playerController.isPlayerRightClickingOnEntity(this.thePlayer, this.objectMouseOver.entityHit, this.objectMouseOver))
                flag = false;
            else if (this.playerController.interactWithEntitySendPacket(this.thePlayer, this.objectMouseOver.entityHit))
                flag = false;
            
            if (flag)
            {
                ItemStack itemstack1 = this.thePlayer.inventory.getCurrentItem();
                boolean result = !ForgeEventFactory.onPlayerInteract(this.thePlayer,
                    PlayerInteractEvent.Action.RIGHT_CLICK_AIR, this.theWorld, null, null, null).isCanceled();
                if (result && itemstack1 != null &&
                    this.playerController.sendUseItem(this.thePlayer, this.theWorld, itemstack1))
                {
                    this.entityRenderer.itemRenderer.resetEquippedProgress2();
                }
            }
        };
        
        if (Tweakception.globalTweaks.isGiftFeaturesOn() &&
            Tweakception.globalTweaks.isAutoSwitchGiftSlotOn() &&
            (slot = Utils.findInHotbarById("WHITE_GIFT", "GREEN_GIFT", "RED_GIFT")) != -1)
        {
            getPlayer().inventory.currentItem = slot;
            afterInject.run();
            ci.cancel();
            return;
        }
        
        String id = Utils.getSkyblockItemId(getPlayer().getCurrentEquippedItem());
        if (id == null)
            return;
        
        if ((id.equals("ASPECT_OF_THE_END") || id.equals("ASPECT_OF_THE_VOID")) &&
            Tweakception.dungeonTweaks.isAutoSwapSpiritSceptreAoteOn() &&
            GlobalTweaks.getCurrentIsland() == SkyblockIsland.DUNGEON && !inAutoSwap)
        {
            slot = Utils.findInHotbarById("BAT_WAND");
            if (slot != -1)
            {
                getPlayer().inventory.currentItem = slot;
                afterInject.run();
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "rightClickMouse", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/event/ForgeEventFactory;onPlayerInteract(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$Action;Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/Vec3;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;", ordinal = 0),
        cancellable = true)
    private void rightClickOnABlock(CallbackInfo ci)
    {
//        sendChat("rclick block");
        if (Tweakception.dungeonTweaks.isAutoSwapSpiritSceptreAoteOn() &&
            GlobalTweaks.getCurrentIsland() == SkyblockIsland.DUNGEON && !inAutoSwap)
        {
            String id = Utils.getSkyblockItemId(getPlayer().getCurrentEquippedItem());
            if (id != null && (id.equals("ASPECT_OF_THE_END") || id.equals("ASPECT_OF_THE_VOID")))
            {
                int slot = Utils.findInHotbarById("BAT_WAND");
                if (slot != -1)
                {
                    getPlayer().inventory.currentItem = slot;
                    
                    boolean flag = true;
                    ItemStack itemstack = this.thePlayer.inventory.getCurrentItem();
                    BlockPos blockpos = this.objectMouseOver.getBlockPos();
                    
                    int i = itemstack != null ? itemstack.stackSize : 0;
                    
                    boolean result = !ForgeEventFactory.onPlayerInteract(this.thePlayer,
                        PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, this.theWorld, blockpos,
                        this.objectMouseOver.sideHit, this.objectMouseOver.hitVec).isCanceled();
                    
                    if (result && this.playerController.onPlayerRightClick(this.thePlayer, this.theWorld,
                        this.thePlayer.getCurrentEquippedItem(), this.objectMouseOver.getBlockPos(),
                        this.objectMouseOver.sideHit, this.objectMouseOver.hitVec))
                    {
                        flag = false;
                        
                        this.thePlayer.swingItem();
                    }
                    
                    if (itemstack == null)
                    {
                        ci.cancel();
                        return;
                    }
                    
                    if (itemstack.stackSize == 0)
                        this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
                    else if (itemstack.stackSize != i || this.playerController.isInCreativeMode())
                        this.entityRenderer.itemRenderer.resetEquippedProgress();
                    
                    if (flag)
                    {
                        ItemStack itemstack1 = this.thePlayer.inventory.getCurrentItem();
                        boolean result1 = !ForgeEventFactory.onPlayerInteract(this.thePlayer,
                            PlayerInteractEvent.Action.RIGHT_CLICK_AIR, this.theWorld, null, null, null).isCanceled();
                        if (result1 && itemstack1 != null &&
                            this.playerController.sendUseItem(this.thePlayer, this.theWorld, itemstack1))
                        {
                            this.entityRenderer.itemRenderer.resetEquippedProgress2();
                        }
                    }
                    
                    ci.cancel();
                }
            }
        }
    }
    
    @Inject(method = "rightClickMouse", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/event/ForgeEventFactory;onPlayerInteract(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$Action;Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/Vec3;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;", ordinal = 1),
        cancellable = true)
    private void rightClickOnAir(CallbackInfo ci)
    {
//        sendChat("rclick air");
        if (Tweakception.dungeonTweaks.isAutoSwapSpiritSceptreAoteOn() &&
            GlobalTweaks.getCurrentIsland() == SkyblockIsland.DUNGEON && !inAutoSwap)
        {
            String id = Utils.getSkyblockItemId(getPlayer().getCurrentEquippedItem());
            if (id != null && (id.equals("ASPECT_OF_THE_END") || id.equals("ASPECT_OF_THE_VOID")))
            {
                int slot = Utils.findInHotbarById("BAT_WAND");
                if (slot != -1)
                {
                    getPlayer().inventory.currentItem = slot;
                    
                    ItemStack itemstack1 = this.thePlayer.inventory.getCurrentItem();
                    boolean result1 = !ForgeEventFactory.onPlayerInteract(this.thePlayer,
                        PlayerInteractEvent.Action.RIGHT_CLICK_AIR, this.theWorld, null, null, null).isCanceled();
                    if (result1 && itemstack1 != null &&
                        this.playerController.sendUseItem(this.thePlayer, this.theWorld, itemstack1))
                    {
                        this.entityRenderer.itemRenderer.resetEquippedProgress2();
                    }
                    
                    ci.cancel();
                }
            }
        }
    }
    
    @Inject(method = "clickMouse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingItem()V"),
        cancellable = true)
    private void clickMouse(CallbackInfo ci)
    {
//        sendChat("click mouse");
        if (Tweakception.dungeonTweaks.isAutoSwapSpiritSceptreAoteOn() &&
            GlobalTweaks.getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            String id = Utils.getSkyblockItemId(getPlayer().getCurrentEquippedItem());
            if (id != null)
            {
                if (id.equals("ASPECT_OF_THE_END") || id.equals("ASPECT_OF_THE_VOID"))
                {
                    // Do not swap click if no ss to swap to
                    int slot = Utils.findInHotbarById("BAT_WAND");
                    if (slot != -1)
                    {
                        inAutoSwap = true;
                        this.rightClickMouse();
                        inAutoSwap = false;
                        
                        ci.cancel();
                    }
                }
                else if (id.equals("BAT_WAND") && // Do not swap when attacking with both buttons
                    !Mouse.isButtonDown(1))
                {
                    int slot = Utils.findInHotbarById("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID");
                    if (slot != -1)
                    {
                        getPlayer().inventory.currentItem = slot;
                        
                        inAutoSwap = true;
                        this.rightClickMouse();
                        inAutoSwap = false;
                        
                        ci.cancel();
                    }
                }
            }
        }
    }
    
    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void getLimitFramerate(CallbackInfoReturnable<Integer> cir)
    {
        if (Tweakception.globalTweaks.isAfkModeActive())
        {
            cir.setReturnValue(Tweakception.globalTweaks.getAfkFpsLimit());
            cir.cancel();
        }
    }
}
