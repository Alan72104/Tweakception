package a7.tweakception.mixin;

import a7.tweakception.DevSettings;
import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.MiningTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Stopwatch;
import a7.tweakception.utils.Utils;
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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

import static a7.tweakception.utils.McUtils.*;

@Mixin(ForgeHooks.class)
public class MixinForgeHooks
{
//    @Unique
//    private static final Map<String, Integer> tc$picksToBreakingPower = new HashMap<>();
    @Unique
    private static boolean tc$switchingSlots = false;
    @Unique
    private static final Stopwatch tc$printSpeedTimer = new Stopwatch(1000);
    
    static
    {
//        tc$picksToBreakingPower.put("REFINED_MITHRIL_PICKAXE", 5);
//        tc$picksToBreakingPower.put("MITHRIL_PICKAXE", 5);
//        tc$picksToBreakingPower.put("DIAMOND_PICKAXE", 4);
//        tc$picksToBreakingPower.put("BANDAGED_MITHRIL_PICKAXE", 5);
//        tc$picksToBreakingPower.put("FRACTURED_MITHRIL_PICKAXE", 5);
//        tc$picksToBreakingPower.put("PICKONIMBUS", 7);
//        tc$picksToBreakingPower.put("STONK", 6);
//        tc$picksToBreakingPower.put("JUNGLE_PICKAXE", 5);
//        tc$picksToBreakingPower.put("TITANIUM_PICKAXE", 6);
//        tc$picksToBreakingPower.put("REFINED_TITANIUM_PICKAXE", 6);
//        tc$picksToBreakingPower.put("GEMSTONE_GAUNTLET", 9);
//        tc$picksToBreakingPower.put("MITHRIL_DRILL_1", 5);
//        tc$picksToBreakingPower.put("MITHRIL_DRILL_2", 6);
//        tc$picksToBreakingPower.put("GEMSTONE_DRILL_1", 7);
//        tc$picksToBreakingPower.put("GEMSTONE_DRILL_2", 8);
//        tc$picksToBreakingPower.put("GEMSTONE_DRILL_3", 9);
//        tc$picksToBreakingPower.put("GEMSTONE_DRILL_4", 9);
//        tc$picksToBreakingPower.put("TITANIUM_DRILL_1", 7);
//        tc$picksToBreakingPower.put("TITANIUM_DRILL_2", 8);
//        tc$picksToBreakingPower.put("TITANIUM_DRILL_3", 9);
//        tc$picksToBreakingPower.put("TITANIUM_DRILL_4", 9);
//        tc$picksToBreakingPower.put("DIVAN_DRILL", 10);
    }
    
    // Gets the block damage per tick
    // Example logic
    // mc.clickMouse()
    // playerController.clickBlock()
    // block.getPlayerRelativeBlockHardness()
    // ForgeHooks.blockStrength()
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
            float pickaxeBreakSpeed = 0.0f;
            MiningTweaks.SpecialBlock sbBlock = Tweakception.miningTweaks.getSpecialBlock(world, pos);
            
            if (sbBlock == null || stack == null)
                return;
            else if (getBreakingPower(stack) < sbBlock.breakingPower)
                return;
            else
            {
                pickaxeBreakSpeed = Tweakception.miningTweaks.getHeldToolMiningSpeed();
                pickaxeBreakSpeed += Tweakception.miningTweaks.getBaseMiningSpeed();
                pickaxeBreakSpeed *= Tweakception.miningTweaks.getMiningSpeedMultiplier();
            }
            
            if (player.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(player))
                pickaxeBreakSpeed /= 5.0F;
            
            if (!player.onGround)
                pickaxeBreakSpeed /= 5.0F;
            
            float blockHardness = sbBlock.hardness;
            boolean print = DevSettings.printSimMiningSpeedNums && tc$printSpeedTimer.checkAndResetIfElapsed();
            if (print)
                sendChat("Hardness: " + blockHardness);
            
            float dmg = pickaxeBreakSpeed / blockHardness / 30.0f;
            if (print)
                sendChatf("%.1f / %.1f / 30.0f = %.3f, ticks required: %d",
                    pickaxeBreakSpeed, blockHardness, dmg, (int) Math.ceil(1 / dmg));
            int extra = Tweakception.miningTweaks.getSimulateBlockHardnessExtraTicks();
            if (Tweakception.miningTweaks.isMiningSpeedBoostActivated())
                extra += Tweakception.miningTweaks.getSimulateBlockHardnessExtraTicksOnBoost();
            if (extra > 0)
            {
                int ticks = (int) Math.ceil(1 / dmg);
                ticks += extra;
                dmg = 1.0f / ticks;
                if (print)
                    sendChatf("Overwrote dmg to %.3f, ticks required: %d", dmg, (int) Math.ceil(1 / dmg));
            }
            cir.setReturnValue(dmg);
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
            !tc$switchingSlots &&
            player.getCurrentEquippedItem() != null &&
            player.getCurrentEquippedItem().getItem() instanceof ItemAxe)
        {
            int rod = Utils.findFishingRodInHotbar();
            if (rod == -1)
                return;
            tc$switchingSlots = true;
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
                    tc$switchingSlots = false;
                }, 3);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
    
    private static int getBreakingPower(ItemStack stack)
    {
        NBTTagList list = McUtils.getDisplayLoreNbt(stack);
        if (list == null)
            return 0;
        for (int i = 0; i < 5 && i < list.tagCount(); i++)
        {
            String line = list.getStringTagAt(i);
            if (line.startsWith("§8Breaking Power "))
            {
                try
                {
                    return Integer.parseInt(line.substring("§8Breaking Power ".length()));
                }
                catch (Exception ignored)
                {
                    return 0;
                }
            }
        }
        return 0;
    }
}
