package a7.tweakception.tweaks;

import a7.tweakception.utils.RenderUtils;
import jdk.nashorn.internal.ir.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.utils.McUtils.*;

public class CrimsonTweaks
{
    private final ResourceLocation MAP_TEXTURE = new ResourceLocation("tweakception:crimson_map.png");
    private final ResourceLocation MAP_PLAYER_MARKER_TEXTURE = new ResourceLocation("tweakception:map_player_marker.png");
    private final float MAP_WIDTH = 1200.0f;
    private final float MAP_HEIGHT = 964.0f;
    private final float MAP_SCALE = 200.0f / MAP_WIDTH;
    private final float MAP_SPAWNPOINT_X = 590.0f;
    private final float MAP_SPAWNPOINT_Z = 925.0f;
    private final float WORLD_SPAWNPOINT_X = -360.0f;
    private final float WORLD_SPAWNPOINT_Z = -430.0f;
    private final float MAP_RIGHTMOST_X = 1147.0f;
    private final float MAP_RIGHTMOST_Z = 394.0f;
    private final float WORLD_RIGHTMOST_X = 36.0f;
    private final float WORLD_RIGHTMOST_Z = -811.0f;
    private final float MAP_PLAYER_MARKER_WIDTH = 5.0f;
    private final float MAP_PLAYER_MARKER_HEIGHT = 7.0f;
    private final float MAP_PLAYER_MARKER_SCALE = 5.0f / MAP_PLAYER_MARKER_WIDTH;
    private final float WORLD_TO_MAP_SCALE = (MAP_SPAWNPOINT_X - MAP_RIGHTMOST_X) / (WORLD_SPAWNPOINT_X - WORLD_RIGHTMOST_X);
    private boolean isMapEnabled = false;
    private float mapPosX = 50.0f;
    private float mapPosY = 50.0f;
    private float mapScale = 1.0f;
    private float mapMarkerScale = 1.0f;
    private boolean isSpongeEnabled = false;
//    private final LinkedHashMap<Tuple<Integer, Integer>, ArrayList<BlockPos>> sponges = new LinkedHashMap<>();

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (!isMapEnabled) return;
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() != SkyblockIsland.CRIMSON_ISLE) return;

        if (isSpongeEnabled)
        {
            for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(getPlayer().posX - 50, 70, getPlayer().posZ - 50),
                    new BlockPos(getPlayer().posX + 50, 210, getPlayer().posZ + 50)))
            {
                if (getWorld().getBlockState(pos).getBlock() == Blocks.sponge)
                {
                    RenderUtils.renderBeaconBeamOrBoundingBox(pos, 0xa89d32, 0.5f, event.partialTicks);
                }
            }
        }
    }

    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (getCurrentIsland() != SkyblockIsland.CRIMSON_ISLE) return;

        if (isMapEnabled)
        {
            GlStateManager.pushMatrix();
            getMc().getTextureManager().bindTexture(MAP_TEXTURE);
            GlStateManager.translate(mapPosX, mapPosY, 0);
            GlStateManager.pushMatrix();
            GlStateManager.scale(MAP_SCALE * mapScale, MAP_SCALE * mapScale, 1);
            RenderUtils.drawTexturedRect(0.0f, 0.0f, MAP_WIDTH, MAP_HEIGHT, GL11.GL_NEAREST);
            GlStateManager.popMatrix();

            float playerX = (float)getPlayer().posX;
//            float playerX = WORLD_SPAWNPOINT_X;
            float playerZ = (float)getPlayer().posZ;
//            float playerZ = WORLD_SPAWNPOINT_Z;
            getMc().getTextureManager().bindTexture(MAP_PLAYER_MARKER_TEXTURE);
            GlStateManager.translate(
                    (MAP_SPAWNPOINT_X + (playerX - WORLD_SPAWNPOINT_X) * WORLD_TO_MAP_SCALE) * MAP_SCALE * mapScale,
                    (MAP_SPAWNPOINT_Z + (playerZ - WORLD_SPAWNPOINT_Z) * WORLD_TO_MAP_SCALE) * MAP_SCALE * mapScale, 0);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(getPlayer().rotationYaw, 0, 0, 1);
            GlStateManager.scale(MAP_PLAYER_MARKER_SCALE * mapMarkerScale, MAP_PLAYER_MARKER_SCALE * mapMarkerScale, 1);
            RenderUtils.drawTexturedRect(
                    -MAP_PLAYER_MARKER_WIDTH / 2.0f, -MAP_PLAYER_MARKER_HEIGHT / 2.0f,
                    MAP_PLAYER_MARKER_WIDTH, MAP_PLAYER_MARKER_HEIGHT, GL11.GL_NEAREST);
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
    }

    public void toggleMap()
    {
        isMapEnabled = !isMapEnabled;
        sendChat("CrimsonTweaks => Map: toggled " + isMapEnabled);
    }

    public void setMapPos(int x, int y)
    {
        mapPosX = x;
        mapPosY = y;
    }

    public void setMapScale(float mapScale)
    {
        this.mapScale = mapScale;
    }

    public void setMapMarkerScale(float scale)
    {
        this.mapMarkerScale = scale;
    }

    public void toggleSponge()
    {
        isSpongeEnabled = !isSpongeEnabled;
    }
}
