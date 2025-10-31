package com.raiiiden.taczblueprints.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.item.GunBlueprintItem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.Optional;

public class BlueprintItemRenderer extends BlockEntityWithoutLevelRenderer {

    public BlueprintItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext displayContext,
                             @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {

        int overlay = OverlayTexture.NO_OVERLAY;

        String gunId = GunBlueprintItem.getGunId(stack);
        if (gunId == null || gunId.isEmpty()) {
            return;
        }

        ResourceLocation baseTexture = getBaseTexture(stack);
        ResourceLocation overlayTexture = getGunSlotTexture(gunId);

        if (overlayTexture == null) {
            return;
        }

        boolean isGuiContext = (displayContext == ItemDisplayContext.GUI ||
                displayContext == ItemDisplayContext.GROUND ||
                displayContext == ItemDisplayContext.FIXED ||
                displayContext == ItemDisplayContext.NONE);

        if (isGuiContext) {
            int light = LightTexture.FULL_BRIGHT;
            float baseZLevel = 0.0f;
            float overlayZLevel = baseZLevel + 0.01f;
            float baseScale = 1.0f;
            float overlayScale = 0.45f;

            renderTexturedQuad(poseStack, buffer, light, overlay, baseTexture,
                    baseZLevel, 0.0f, 0.0f, baseScale, displayContext, true, false);

            renderTexturedQuad(poseStack, buffer, light, overlay, overlayTexture,
                    overlayZLevel, 0.0f, 0.0f, overlayScale, displayContext, true, false);

        } else {
            int light = packedLight;
            float baseZLevel = 0.0f;
            float overlayZLevel = baseZLevel + 0.01f;
            float baseScale = 1.0f;
            float overlayScale = 0.45f;

            // Apply transforms based on display context
            if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                poseStack.translate(0.8f, 0.5f, 1.0f);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-75));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(120));
            } else if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                poseStack.translate(-0.8f, 0.5f, 1.0f);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(75));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(120));
            } else if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.translate(0.5f, 0.4f, 0.8f);
                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(0));
            } else if (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                poseStack.translate(0.5f, 0.4f, 0.5f);
                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(0));
            }

            renderTexturedQuad(poseStack, buffer, light, overlay, baseTexture,
                    baseZLevel, 0.0f, 0.0f, baseScale, displayContext, false, false);

            renderTexturedQuad(poseStack, buffer, light, overlay, overlayTexture,
                    overlayZLevel, 0.0f, 0.0f, overlayScale, displayContext, true, false);
        }
    }

    private void renderTexturedQuad(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int light, int overlay, ResourceLocation texture,
                                    float zLevel, float xOffset, float yOffset,
                                    float scale, ItemDisplayContext displayContext,
                                    boolean flipOverlay, boolean rotateOverlay) {

        boolean isGuiContext = (displayContext == ItemDisplayContext.GUI ||
                displayContext == ItemDisplayContext.GROUND ||
                displayContext == ItemDisplayContext.FIXED ||
                displayContext == ItemDisplayContext.NONE);

        RenderType renderType = isGuiContext ? RenderType.text(texture)
                : RenderType.entityCutout(texture);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();

        poseStack.translate(xOffset, yOffset, 0.0f);
        poseStack.translate(0.5f, 0.5f, 0.0f);

        if (rotateOverlay) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-75F));
        }

        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5f, -0.5f, 0.0f);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        float minX = 0.0f;
        float minY = 0.0f;
        float maxX = 1.0f;
        float maxY = 1.0f;

        float u1 = flipOverlay ? 0.0f : 1.0f;
        float u2 = flipOverlay ? 1.0f : 0.0f;

        // FRONT FACE
        vertexConsumer.vertex(matrix, minX, minY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u1, 1.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, -1.0f)
                .endVertex();

        vertexConsumer.vertex(matrix, maxX, minY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u2, 1.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, -1.0f)
                .endVertex();

        vertexConsumer.vertex(matrix, maxX, maxY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u2, 0.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, -1.0f)
                .endVertex();

        vertexConsumer.vertex(matrix, minX, maxY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u1, 0.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, -1.0f)
                .endVertex();

        // BACK FACE (reversed winding order)
        vertexConsumer.vertex(matrix, minX, maxY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u1, 0.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();

        vertexConsumer.vertex(matrix, maxX, maxY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u2, 0.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();

        vertexConsumer.vertex(matrix, maxX, minY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u2, 1.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();

        vertexConsumer.vertex(matrix, minX, minY, zLevel)
                .color(255, 255, 255, 255)
                .uv(u1, 1.0f)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();

        poseStack.popPose();
    }

    private ResourceLocation getBaseTexture(ItemStack stack) {
        String itemName = stack.getItem().toString();

        if (itemName.contains("pistol")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_pistol.png");
        } else if (itemName.contains("smg")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_smg.png");
        } else if (itemName.contains("rifle")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_rifle.png");
        } else if (itemName.contains("shotgun")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_shotgun.png");
        } else if (itemName.contains("sniper")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_sniper.png");
        } else if (itemName.contains("mg")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_mg.png");
        } else if (itemName.contains("rpg")) {
            return new ResourceLocation("taczblueprints", "textures/item/blueprint_rpg.png");
        }

        return new ResourceLocation("taczblueprints", "textures/item/blueprint_default.png");
    }

    private ResourceLocation getGunSlotTexture(String gunId) {
        try {
            ResourceLocation lookupId = getGunIdForLookup(gunId);
            Optional<ClientGunIndex> gunIndex = TimelessAPI.getClientGunIndex(lookupId);

            if (gunIndex.isPresent()) {
                ResourceLocation slotTexture = gunIndex.get().getDefaultDisplay().getSlotTexture();
                return slotTexture;
            } else {
                TaCZBlueprints.LOGGER.warn("[Blueprint] No ClientGunIndex found for: {}", lookupId);
            }
        } catch (Exception e) {
            TaCZBlueprints.LOGGER.error("[Blueprint] Error getting slot texture: ", e);
        }

        return null;
    }

    private static ResourceLocation getGunIdForLookup(String storedGunId) {
        ResourceLocation rl = new ResourceLocation(storedGunId);
        String path = rl.getPath();
        if (path.startsWith("gun/")) {
            path = path.substring(4);
        }
        return new ResourceLocation(rl.getNamespace(), path);
    }
}