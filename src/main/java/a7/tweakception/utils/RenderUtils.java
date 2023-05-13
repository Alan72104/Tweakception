package a7.tweakception.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;

import static a7.tweakception.utils.McUtils.getMc;
import static a7.tweakception.utils.McUtils.getPartialTicks;

public class RenderUtils
{
    public static final Color DEFAULT_HIGHLIGHT_COLOR = new Color(0, 255, 0, 85);
    public static final Color DEFAULT_HIGHLIGHT_COLOR_ALPHA = new Color(0, 255, 0, 85 / 2);
    private static final ResourceLocation beaconBeam = new ResourceLocation("textures/entity/beacon_beam.png");
    
    public static AxisAlignedBB getAABBFromType(int type)
    {
        AxisAlignedBB bb;
        switch (type)
        {
            case -1: // Armor stand
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.5, 1.975, 0.5);
                break;
            default:
            case 0: // Zombie
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.6, 1.95, 0.6);
                break;
            case 1: // Spider
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.4, 0.9, 1.4);
                break;
            case 2: // Wolf
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.6, 0.85, 0.6);
                break;
            case 3: // Enderman
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.6, 2.9, 0.6);
                break;
            case 4: // Blaze
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 1.8, 0.6, 1.8);
                break;
            case 5: // Bat
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.5, 0.9, 0.5);
                break;
            case 6: // Pig
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.9, 0.9, 0.9);
                break;
            case 7: // Player
                bb = new AxisAlignedBB(0.0, 0.0, 0.0, 0.6, 1.8, 0.6);
                break;
        }
        return bb;
    }
    
    public static void drawDefaultHighlightBoxUnderEntity(Entity entity, int type, Color c, boolean depth)
    {
        AxisAlignedBB bb = getAABBFromType(type);
//        bb.offset(-(bb.maxX / 2.0), -bb.maxY, -(bb.maxZ / 2.0));
        double defaultYOffset = 0.19375;
        bb = bb.offset(-(bb.maxX - entity.width) / 2.0, -bb.maxY - defaultYOffset, -(bb.maxZ - entity.height) / 2.0);
        drawHighlightBox(entity, bb, c, getPartialTicks(), depth);
    }
    
    public static void drawDefaultHighlightBox(Entity entity, int type, Color c, boolean depth)
    {
        AxisAlignedBB bb = getAABBFromType(type);
        bb = bb.offset(-bb.maxX / 2.0, 0.0, -bb.maxZ / 2.0);
        drawHighlightBox(entity, bb, c, getPartialTicks(), depth);
    }
    
    public static void drawDefaultHighlightBoxForEntity(Entity entity, Color c, boolean depth)
    {
        AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, entity.width, entity.height, entity.width);
        bb = bb.offset(-bb.maxX / 2.0, 0.0, -bb.maxZ / 2.0);
        drawHighlightBox(entity, bb, c, getPartialTicks(), depth);
    }
    
    // Draws a box relative to the entity, origin is the entity's center
    public static void drawHighlightBox(Entity entity, AxisAlignedBB axisAlignedBB, Color c, float partialTicks, boolean depth)
    {
        Entity viewEntity = Minecraft.getMinecraft().getRenderViewEntity();
        
        double viewX = viewEntity.lastTickPosX + ((viewEntity.posX - viewEntity.lastTickPosX) * partialTicks);
        double viewY = viewEntity.lastTickPosY + ((viewEntity.posY - viewEntity.lastTickPosY) * partialTicks);
        double viewZ = viewEntity.lastTickPosZ + ((viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks);
        
        GlStateManager.pushMatrix();
        
        GlStateManager.translate(-viewX, -viewY, -viewZ);
        
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        
        if (!depth)
            GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, c.getAlpha() / 255.0f);
        
        Vec3 renderPos = new Vec3(
            (float) (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks),
            (float) (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks),
            (float) (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks)
        );
        GlStateManager.translate(axisAlignedBB.minX + renderPos.xCoord, axisAlignedBB.minY + renderPos.yCoord, axisAlignedBB.minZ + renderPos.zCoord);
        
        double x = axisAlignedBB.maxX - axisAlignedBB.minX;
        double y = axisAlignedBB.maxY - axisAlignedBB.minY;
        double z = axisAlignedBB.maxZ - axisAlignedBB.minZ;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, 0, z);
        GL11.glVertex3d(0, y, z);
        GL11.glVertex3d(0, y, 0); // TOP LEFT / BOTTOM LEFT / TOP RIGHT/ BOTTOM RIGHT
        
        GL11.glVertex3d(x, 0, z);
        GL11.glVertex3d(x, 0, 0);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(x, y, z);
        
        GL11.glVertex3d(0, y, z);
        GL11.glVertex3d(0, 0, z);
        GL11.glVertex3d(x, 0, z);
        GL11.glVertex3d(x, y, z); // TOP LEFT / BOTTOM LEFT / TOP RIGHT/ BOTTOM RIGHT
        
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, y, 0);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(x, 0, 0);
        
        GL11.glVertex3d(0, y, 0);
        GL11.glVertex3d(0, y, z);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y, 0);
        
        GL11.glVertex3d(0, 0, z);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(x, 0, 0);
        GL11.glVertex3d(x, 0, z);
        GL11.glEnd();
        if (!depth)
            GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
    
    public static void drawOutlineForEntity(Entity e, Color c, float partialTicks, boolean depth, float lineWidth)
    {
        if (!depth)
            GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableTexture2D();
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer r = tessellator.getWorldRenderer();
        
        Vector3d view = getInterpolatedViewingPos(partialTicks);
        Vector3d pos = getInterpolatedPos(e, partialTicks);
        float w = e.width;
        float h = e.height;
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(-view.x, -view.y, -view.z);
        GlStateManager.translate(pos.x, pos.y, pos.z);
        GlStateManager.translate(-w / 2.0f, 0, -w / 2.0f);
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        GL11.glLineWidth(lineWidth);
        
        r.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        r.pos(0, 0, 0).endVertex();
        r.pos(w, 0, 0).endVertex();
        r.pos(w, 0, w).endVertex();
        r.pos(0, 0, w).endVertex();
        tessellator.draw();
        r.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        r.pos(0, 0, 0).endVertex();
        r.pos(0, h, 0).endVertex();
        r.pos(w, 0, 0).endVertex();
        r.pos(w, h, 0).endVertex();
        r.pos(w, 0, w).endVertex();
        r.pos(w, h, w).endVertex();
        r.pos(0, 0, w).endVertex();
        r.pos(0, h, w).endVertex();
        tessellator.draw();
        r.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        r.pos(0, h, 0).endVertex();
        r.pos(w, h, 0).endVertex();
        r.pos(w, h, w).endVertex();
        r.pos(0, h, w).endVertex();
        tessellator.draw();
        
        GlStateManager.popMatrix();
        
        if (!depth)
            GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GL11.glLineWidth(1.0f);
    }
    
    public static void drawTexturedRect(float x, float y, float width, float height)
    {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1);
    }
    
    public static void drawTexturedRect(float x, float y, float width, float height, int filter)
    {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1, filter);
    }
    
    public static void drawTexturedRect(
        float x,
        float y,
        float width,
        float height,
        float uMin,
        float uMax,
        float vMin,
        float vMax
    )
    {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin, vMax, GL11.GL_NEAREST);
    }
    
    public static void drawTexturedRect(
        float x,
        float y,
        float width,
        float height,
        float uMin,
        float uMax,
        float vMin,
        float vMax,
        int filter
    )
    {
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        drawTexturedRectNoBlend(x, y, width, height, uMin, uMax, vMin, vMax, filter);
        
        GlStateManager.disableBlend();
    }
    
    public static void drawTexturedRectNoBlend(
        float x,
        float y,
        float width,
        float height,
        float uMin,
        float uMax,
        float vMin,
        float vMax,
        int filter
    )
    {
        GlStateManager.enableTexture2D();
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer
            .pos(x, y + height, 0.0D)
            .tex(uMin, vMax).endVertex();
        worldrenderer
            .pos(x + width, y + height, 0.0D)
            .tex(uMax, vMax).endVertex();
        worldrenderer
            .pos(x + width, y, 0.0D)
            .tex(uMax, vMin).endVertex();
        worldrenderer
            .pos(x, y, 0.0D)
            .tex(uMin, vMin).endVertex();
        tessellator.draw();
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }
    
    public static void drawFilledBoundingBox(AxisAlignedBB bb, Color c, boolean depth)
    {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        if (!depth)
            GlStateManager.disableDepth();
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        
        //vertical
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        tessellator.draw();
        
        GlStateManager.color(
            c.getRed() / 255f * 0.8f,
            c.getGreen() / 255f * 0.8f,
            c.getBlue() / 255f * 0.8f,
            c.getAlpha() / 255f
        );
        
        //x
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        tessellator.draw();
        
        GlStateManager.color(
            c.getRed() / 255f * 0.9f,
            c.getGreen() / 255f * 0.9f,
            c.getBlue() / 255f * 0.9f,
            c.getAlpha() / 255f
        );
        //z
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        tessellator.draw();
        
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        if (!depth)
            GlStateManager.enableDepth();
    }
    
    public static void drawFilledBoundingBoxBlockSize(double x, double y, double z, Color c)
    {
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        drawFilledBoundingBox(bb, c, false);
    }
    
    public static void drawFilledBoundingBoxChestSize(double x, double y, double z, Color c)
    {
        AxisAlignedBB bb = new AxisAlignedBB(x + 0.0625, y, z + 0.0625,
            x + 0.9375, y + 0.875, z + 0.9375);
        drawFilledBoundingBox(bb, c, false);
    }
    
    public static Vector3d getInterpolatedViewingPos(float partialTicks)
    {
        return getInterpolatedPos(getMc().getRenderViewEntity(), partialTicks);
    }
    
    public static Vector3d getInterpolatedPos(Entity e, float partialTicks)
    {
        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks;
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks;
        return new Vector3d(x, y, z);
    }
    
    /**
     * @param type -1 beacon beam, 0 auto, 1 bounding box
     */
    public static void drawBeaconBeamOrBoundingBox(BlockPos block, Color c, float partialTicks, int type)
    {
        drawBeaconBeamOrBoundingBox(block, c, partialTicks, type, 10);
    }
    
    public static void drawBeaconBeamOrBoundingBox(BlockPos block, Color c, float partialTicks, int type, float range)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        double x = block.getX() - viewer.x;
        double y = block.getY() - viewer.y;
        double z = block.getZ() - viewer.z;
        
        double distSq = x * x + y * y + z * z;
        
        if (type < 0 || (type == 0 && distSq > range * range))
            RenderUtils.drawBeaconBeam(x, y, z, c, partialTicks, false);
        else
            RenderUtils.drawFilledBoundingBoxBlockSize(x, y, z, c);
    }
    
    // Type -1 is beacon beam
    // Type 0 is auto
    // Type 1 is bounding box
    public static void drawBeaconBeamOrBoundingBox(Entity e, Color c, float partialTicks, int type)
    {
        drawBeaconBeamOrBoundingBox(e, c, partialTicks, type, 10);
    }
    
    public static void drawBeaconBeamOrBoundingBox(Entity e, Color c, float partialTicks, int type, float range)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        double x = e.posX - viewer.x;
        double y = e.posY - viewer.y;
        double z = e.posZ - viewer.z;
        
        double distSq = x * x + y * y + z * z;
        
        if (type < 0 || (type == 0 && distSq > range * range))
            RenderUtils.drawBeaconBeam(x - 0.5, y, z - 0.5, c, partialTicks, false);
        else
            RenderUtils.drawDefaultHighlightBoxForEntity(e, c, false);
    }
    
    public static void drawBeaconBeamOrBoundingBoxWithBoxSize(Entity e, Color c, float partialTicks, int type,
                                                              float range, float boxWidth, float boxHeight)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        double x = e.posX - viewer.x;
        double y = e.posY - viewer.y;
        double z = e.posZ - viewer.z;
        
        double distSq = x * x + y * y + z * z;
        
        if (type < 0 || (type == 0 && distSq > range * range))
            RenderUtils.drawBeaconBeam(x - 0.5, y, z - 0.5, c, partialTicks, false);
        else
        {
            AxisAlignedBB bb = new AxisAlignedBB(0.0, 0.0, 0.0, boxWidth, boxHeight, boxWidth);
            bb = bb.offset(-bb.maxX / 2.0, 0.0, -bb.maxZ / 2.0);
            drawHighlightBox(e, bb, c, partialTicks, false);
        }
    }
    
    public static void drawFilledBoundingBox(AxisAlignedBB bb, Color c, float partialTicks)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        drawFilledBoundingBox(bb.offset(-viewer.x, -viewer.y, -viewer.z), c, false);
    }
    
    public static void drawFilledBoundingBox(AxisAlignedBB bb, Color c, float partialTicks, boolean depth)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        drawFilledBoundingBox(bb.offset(-viewer.x, -viewer.y, -viewer.z), c, depth);
    }
    
    public static void drawFilledBoundingBox(BlockPos block, Color c, float partialTicks)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        double x = block.getX() - viewer.x;
        double y = block.getY() - viewer.y;
        double z = block.getZ() - viewer.z;
        drawFilledBoundingBox(new AxisAlignedBB(x, y, z, x + 1d, y + 1d, z + 1d), c, false);
    }
    
    public static void drawBeaconBeamAtEntity(Entity entity, Color c, float partialTicks)
    {
        Vector3d viewer = getInterpolatedViewingPos(partialTicks);
        double x = entity.posX - viewer.x - 0.5;
        double y = entity.posY - viewer.y;
        double z = entity.posZ - viewer.z - 0.5;
        RenderUtils.drawBeaconBeam(x, y, z, c, partialTicks, false);
    }
    
    private static void drawBeaconBeam(
        double x, double y, double z, Color c,
        float partialTicks, Boolean depth
    )
    {
        int height = 300;
        int bottomOffset = 0;
        int topOffset = bottomOffset + height;
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        if (!depth)
        {
            GlStateManager.disableDepth();
        }
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(beaconBeam);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        
        double time = Minecraft.getMinecraft().theWorld.getTotalWorldTime() + (double) partialTicks;
        double d1 = MathHelper.func_181162_h(-time * 0.2D - (double) MathHelper.floor_double(-time * 0.1D));
        
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        float a = c.getAlpha() / 255f;
        double d2 = time * 0.025D * -1.5D;
        double d4 = 0.5D + Math.cos(d2 + 2.356194490192345D) * 0.2D;
        double d5 = 0.5D + Math.sin(d2 + 2.356194490192345D) * 0.2D;
        double d6 = 0.5D + Math.cos(d2 + (Math.PI / 4D)) * 0.2D;
        double d7 = 0.5D + Math.sin(d2 + (Math.PI / 4D)) * 0.2D;
        double d8 = 0.5D + Math.cos(d2 + 3.9269908169872414D) * 0.2D;
        double d9 = 0.5D + Math.sin(d2 + 3.9269908169872414D) * 0.2D;
        double d10 = 0.5D + Math.cos(d2 + 5.497787143782138D) * 0.2D;
        double d11 = 0.5D + Math.sin(d2 + 5.497787143782138D) * 0.2D;
        double d14 = -1.0D + d1;
        double d15 = (double) (height) * 2.5D + d14;
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(1.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(0.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(1.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(0.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(1.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(0.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(1.0D, d15).color(r, g, b, a).endVertex();
        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(0.0D, d15).color(r, g, b, a).endVertex();
        tessellator.draw();
        
        GlStateManager.disableCull();
        double d12 = -1.0D + d1;
        double d13 = height + d12;
        
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.2D).tex(1.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.2D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.2D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.2D).tex(0.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.8D).tex(1.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.8D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.8D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.8D).tex(0.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.2D).tex(1.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.2D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.8D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.8D).tex(0.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.8D).tex(1.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.8D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.2D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.2D).tex(0.0D, d13).color(r, g, b, a * 0.25f).endVertex();
        tessellator.draw();
        
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        if (!depth)
        {
            GlStateManager.enableDepth();
        }
    }
    
    public static void renderTextInWorldOnEntity(String s, Entity e)
    {
        Vector3d v = getInterpolatedPos(e, getPartialTicks());
        drawTextInWorld(s, v.x, v.y + 0.5f, v.z);
    }
    
    public static void drawTextInWorld(String s, double x, double y, double z)
    {
        FontRenderer r = getMc().fontRendererObj;
        RenderManager manager = getMc().getRenderManager();
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-manager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(manager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int j = r.getStringWidth(s) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-j - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-j - 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        r.drawString(s, -r.getStringWidth(s) / 2, 0, 553648127);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        r.drawString(s, -r.getStringWidth(s) / 2, 0, -1);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
    
    public static class Vector3d
    {
        public double x;
        public double y;
        public double z;
        
        public Vector3d(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
