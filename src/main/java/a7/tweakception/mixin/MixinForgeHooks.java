package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import com.sun.jna.platform.win32.User32;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

import static a7.tweakception.utils.McUtils.*;

@Mixin(ForgeHooks.class)
public class MixinForgeHooks
{
    private static final Map<String, Integer> skyblockPicksToBreakingPower = new HashMap<>();
    private static boolean switchingSlots = false;
    
    static
    {
        skyblockPicksToBreakingPower.put("REFINED_MITHRIL_PICKAXE", 5);
        skyblockPicksToBreakingPower.put("MITHRIL_PICKAXE", 5);
        skyblockPicksToBreakingPower.put("DIAMOND_PICKAXE", 4);
        skyblockPicksToBreakingPower.put("BANDAGED_MITHRIL_PICKAXE", 5);
        skyblockPicksToBreakingPower.put("FRACTURED_MITHRIL_PICKAXE", 5);
        skyblockPicksToBreakingPower.put("PICKONIMBUS", 7);
        skyblockPicksToBreakingPower.put("STONK", 6);
        skyblockPicksToBreakingPower.put("JUNGLE_PICKAXE", 5);
        skyblockPicksToBreakingPower.put("TITANIUM_PICKAXE", 6);
        skyblockPicksToBreakingPower.put("REFINED_TITANIUM_PICKAXE", 6);
        skyblockPicksToBreakingPower.put("GEMSTONE_GAUNTLET", 9);
        skyblockPicksToBreakingPower.put("MITHRIL_DRILL_1", 5);
        skyblockPicksToBreakingPower.put("MITHRIL_DRILL_2", 6);
        skyblockPicksToBreakingPower.put("GEMSTONE_DRILL_1", 7);
        skyblockPicksToBreakingPower.put("GEMSTONE_DRILL_2", 8);
        skyblockPicksToBreakingPower.put("GEMSTONE_DRILL_3", 9);
        skyblockPicksToBreakingPower.put("GEMSTONE_DRILL_4", 9);
        skyblockPicksToBreakingPower.put("TITANIUM_DRILL_1", 7);
        skyblockPicksToBreakingPower.put("TITANIUM_DRILL_2", 8);
        skyblockPicksToBreakingPower.put("TITANIUM_DRILL_3", 9);
        skyblockPicksToBreakingPower.put("TITANIUM_DRILL_4", 9);
        skyblockPicksToBreakingPower.put("DIVAN_DRILL", 10);
    }
    
    // Gets the speed at which player can break a block as opposite to a hardness number
    @Inject(method = "blockStrength", at = @At("HEAD"), remap = false, cancellable = true)
    private static void blockStrength(IBlockState state, EntityPlayer player, World world, BlockPos pos, CallbackInfoReturnable<Float> cir)
    {
        if (Tweakception.gardenTweaks.isSimulateCactusKnifeInstaBreakOn() &&
            state.getBlock() == Blocks.cactus &&
            "CACTUS_KNIFE".equals(Utils.getSkyblockItemId(player.getCurrentEquippedItem())))
        {
            cir.setReturnValue(100.0f);
            cir.cancel();
            return;
        }
        
        if (Tweakception.miningTweaks.isSimulateBlockHardnessOn() &&
            (GlobalTweaks.getCurrentIsland() == SkyblockIsland.DWARVEN_MINES ||
                GlobalTweaks.getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS))
        {
            ItemStack stack = player.inventory.getCurrentItem();
            String id = Utils.getSkyblockItemId(stack);
            float pickaxeBreakSpeed = 0.0f;
            float blockHardness = 0.0f;
            
            // Can harvest & break speed
            if (skyblockPicksToBreakingPower.containsKey(id))
            {
                pickaxeBreakSpeed = Tweakception.miningTweaks.getHeldToolMiningSpeed();
                pickaxeBreakSpeed += Tweakception.miningTweaks.getCachedMiningSpeed();
                pickaxeBreakSpeed *= Tweakception.miningTweaks.getMiningSpeedBoostScale();
            }
            else
            {
                return;
            }
            
            if (player.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(player))
                pickaxeBreakSpeed /= 5.0F;
            
            if (!player.onGround)
                pickaxeBreakSpeed /= 5.0F;
            
            // Hardness
            blockHardness = Tweakception.miningTweaks.getSpecialBlockHardness(world, pos);
            if (blockHardness == 0.0f)
            {
                return;
            }
            
            cir.setReturnValue(pickaxeBreakSpeed / blockHardness / 30.0f);
            cir.cancel();
        }
    }
    
    @Inject(method = "onPickBlock", at = @At("HEAD"), remap = false, cancellable = true)
    private static void onPickBlock(MovingObjectPosition target, EntityPlayer player, World world, CallbackInfoReturnable<Boolean> cir)
    {
        if ((Tweakception.dungeonTweaks.isPickaxeMiddleClickRemoveBlockOn() ||
            Tweakception.dungeonTweaks.isPickaxeMiddleClickRemoveLineOn()) &&
            GlobalTweaks.getCurrentIsland() == SkyblockIsland.DUNGEON &&
            player.getCurrentEquippedItem() != null &&
            player.getCurrentEquippedItem().getItem() instanceof ItemPickaxe)
        {
            if (target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
            {
                if (Tweakception.dungeonTweaks.isPickaxeMiddleClickRemoveLineOn())
                {
                    final float step = 0.1f;
                    final float dist = 5.0f;
                    final int count = (int) (dist / step);
                    Vector3f pos = new Vector3f((float) player.posX, (float) player.posY + player.getEyeHeight(), (float) player.posZ);
                    Vec3 lookVec3 = player.getLook(McUtils.getPartialTicks());
                    Vector3f look = new Vector3f((float) lookVec3.xCoord, (float) lookVec3.yCoord, (float) lookVec3.zCoord);
                    look.scale(step / look.length());
                    for (int i = 0; i <= count; i++)
                    {
                        Vector3f.add(pos, look, pos);
                        BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
                        IBlockState state = world.getBlockState(blockPos);
                        Block block = state.getBlock();
                        if (block == Blocks.chest ||
                            block == Blocks.skull ||
                            block == Blocks.lever)
                        {
                            break;
                        }
                        else if (block != Blocks.air)
                        {
                            world.setBlockToAir(blockPos);
                        }
                    }
                }
                else if (Tweakception.dungeonTweaks.isPickaxeMiddleClickRemoveBlockOn())
                {
                    BlockPos pos = target.getBlockPos();
                    Block block = world.getBlockState(pos).getBlock();
                    if (block == Blocks.chest ||
                        block == Blocks.skull ||
                        block == Blocks.lever)
                    {
                        return;
                    }
                    world.setBlockToAir(pos);
                }
            }
            cir.setReturnValue(false);
            cir.cancel();
        }
        else if (Tweakception.foragingTweaks.isAxeMidClickSwapRodBreakOn() &&
            !switchingSlots &&
            player.getCurrentEquippedItem() != null &&
            player.getCurrentEquippedItem().getItem() instanceof ItemAxe)
        {
            int rod = Utils.findFishingRodInHotbar();
            if (rod == -1)
                return;
            switchingSlots = true;
            int lastSlot = getPlayer().inventory.currentItem;
            getPlayer().inventory.currentItem = rod;
            Tweakception.scheduler
                .addDelayed(() -> ((AccessorMinecraft) getMc()).rightClickMouse(), 1)
                .thenDelayed(() ->
                {
                    getPlayer().inventory.currentItem = lastSlot;
                    ((AccessorMinecraft) getMc()).clickMouse();
                    KeyBinding.setKeyBindState(getMc().gameSettings.keyBindAttack.getKeyCode(), true);
                }, 2)
                .thenDelayed(() ->
                {
                    getPlayer().inventory.currentItem = lastSlot;
                    KeyBinding.setKeyBindState(getMc().gameSettings.keyBindAttack.getKeyCode(), false);
                    switchingSlots = false;
                }, 3);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
