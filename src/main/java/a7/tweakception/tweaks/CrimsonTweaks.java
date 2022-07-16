package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static a7.tweakception.Tweakception.BlockSearchThread;
import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class CrimsonTweaks extends Tweak
{
    private final CrimsonTweaksConfig c;
    public static class CrimsonTweaksConfig
    {
        public boolean enableMap = false;
        public float mapPosX = 50.0f;
        public float mapPosY = 50.0f;
        public float mapScale = 1.0f;
        public float mapMarkerScale = 1.0f;
        public boolean highlightSulfur = false;
    }
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
    private List<BlockPos> sponges = new ArrayList<>(25);
    private List<BlockPos> spongesTemp = new ArrayList<>(25);
    private BlockSearchThread searchThread;

    public CrimsonTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.crimsonTweaks;
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (getCurrentIsland() != SkyblockIsland.CRIMSON_ISLE) return;

        if (getTicks() % 20 == 5)
        {
            if (c.highlightSulfur)
            {
                if (searchThread == null || searchThread.done)
                {
                    EntityPlayerSP p = getPlayer();
                    sponges = spongesTemp;
                    spongesTemp = new ArrayList<>(20);
                    searchThread = new BlockSearchThread((int)p.posX - 64, 70, (int)p.posZ - 64,
                            (int)p.posX + 64, 150, (int)p.posZ + 64, getWorld(), Blocks.sponge, spongesTemp);
                    Tweakception.threadPool.execute(searchThread);
                }
            }
        }
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() != SkyblockIsland.CRIMSON_ISLE) return;

        if (c.highlightSulfur)
        {
            for (BlockPos pos : sponges)
                RenderUtils.drawBeaconBeamOrBoundingBox(pos, new Color(168, 157, 50, 127), event.partialTicks, 0);
        }
    }

    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (getCurrentIsland() != SkyblockIsland.CRIMSON_ISLE) return;

        if (c.enableMap)
        {
            GlStateManager.pushMatrix();
            getMc().getTextureManager().bindTexture(MAP_TEXTURE);
            GlStateManager.translate(c.mapPosX, c.mapPosY, 0);
            GlStateManager.pushMatrix();
            GlStateManager.scale(MAP_SCALE * c.mapScale, MAP_SCALE * c.mapScale, 1);
            RenderUtils.drawTexturedRect(0.0f, 0.0f, MAP_WIDTH, MAP_HEIGHT, GL11.GL_NEAREST);
            GlStateManager.popMatrix();

            float playerX = (float)getPlayer().posX;
//            float playerX = WORLD_SPAWNPOINT_X;
            float playerZ = (float)getPlayer().posZ;
//            float playerZ = WORLD_SPAWNPOINT_Z;
            getMc().getTextureManager().bindTexture(MAP_PLAYER_MARKER_TEXTURE);
            GlStateManager.translate(
                    (MAP_SPAWNPOINT_X + (playerX - WORLD_SPAWNPOINT_X) * WORLD_TO_MAP_SCALE) * MAP_SCALE * c.mapScale,
                    (MAP_SPAWNPOINT_Z + (playerZ - WORLD_SPAWNPOINT_Z) * WORLD_TO_MAP_SCALE) * MAP_SCALE * c.mapScale, 0);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(getPlayer().rotationYaw, 0, 0, 1);
            GlStateManager.scale(MAP_PLAYER_MARKER_SCALE * c.mapMarkerScale, MAP_PLAYER_MARKER_SCALE * c.mapMarkerScale, 1);
            RenderUtils.drawTexturedRect(
                    -MAP_PLAYER_MARKER_WIDTH / 2.0f, -MAP_PLAYER_MARKER_HEIGHT / 2.0f,
                    MAP_PLAYER_MARKER_WIDTH, MAP_PLAYER_MARKER_HEIGHT, GL11.GL_NEAREST);
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
    }

    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (searchThread != null)
            searchThread.cancel = true;
    }

    public void toggleMap()
    {
        c.enableMap = !c.enableMap;
        sendChat("CT-Map: toggled " + c.enableMap);
    }

    public void setMapPos(int x, int y)
    {
        c.mapPosX = x >= 0 ? x : new CrimsonTweaksConfig().mapPosX;
        c.mapPosY = y >= 0 ? y : new CrimsonTweaksConfig().mapPosY;
        sendChat("CT-Map: set pos to " + c.mapPosX + ", " + c.mapPosY);
    }

    public void setMapScale(float scale)
    {
        c.mapScale = scale > 0.0f ? scale : new CrimsonTweaksConfig().mapScale;
        sendChat("CT-Map: set scale to " + c.mapScale);
    }

    public void setMapMarkerScale(float scale)
    {
        c.mapMarkerScale = scale > 0.0f ? scale : new CrimsonTweaksConfig().mapMarkerScale;
        sendChat("CT-Map: set marker scale to " + c.mapMarkerScale);
    }

    public void toggleSulfurHighlight()
    {
        c.highlightSulfur = !c.highlightSulfur;
        sendChat("CT-SulfurHighlight: toggled " + c.highlightSulfur);
        if (!c.highlightSulfur)
        {
            if (searchThread != null && !searchThread.done)
                searchThread.cancel = true;
            searchThread = null;
        }
    }
}
