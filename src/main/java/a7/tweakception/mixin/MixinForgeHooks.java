package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.tweaks.Tweak;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ForgeHooks.class)
public class MixinForgeHooks
{
    private static final Map<String, Integer> skyblockPicksToBreakingPower = new HashMap<>();

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

    @Inject(method = "blockStrength", at = @At("HEAD"), remap = false, cancellable = true)
    private static void blockStrength(IBlockState state, EntityPlayer player, World world, BlockPos pos, CallbackInfoReturnable<Float> cir)
    {
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
}
