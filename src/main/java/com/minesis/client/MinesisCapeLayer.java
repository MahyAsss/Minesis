package com.minesis.client;

import com.minesis.entity.MinesisEntity;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.UUID;

public class MinesisCapeLayer extends RenderLayer<MinesisEntity, PlayerModel<MinesisEntity>> {

    public MinesisCapeLayer(LivingEntityRenderer<MinesisEntity, PlayerModel<MinesisEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(@org.jetbrains.annotations.NotNull PoseStack poseStack,
                       @org.jetbrains.annotations.NotNull MultiBufferSource buffer,
                       int packedLight,
                       @org.jetbrains.annotations.NotNull MinesisEntity entity,
                       float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (entity.isInvisible()) return;

        ResourceLocation capeTexture = getCapeTexture(entity);
        if (capeTexture == null) return;

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, 0.125f);

        float bodyRot = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double sinRot = Mth.sin(bodyRot * (float) (Math.PI / 180.0));
        double cosRot = -Mth.cos(bodyRot * (float) (Math.PI / 180.0));

        // Approximate cape physics with entity motion (vanilla uses AbstractClientPlayer-specific
        // cloak anchor tracking; getDeltaMovement() is a close enough substitute)
        Vec3 motion = entity.getDeltaMovement();

        float verticalLag = (float) motion.y * 10.0f;
        verticalLag = Mth.clamp(verticalLag, -6.0f, 32.0f);

        float forwardLag = (float) (motion.x * sinRot + motion.z * cosRot) * 100.0f;
        forwardLag = Mth.clamp(forwardLag, 0.0f, 150.0f);

        float sideLag = (float) (motion.x * cosRot - motion.z * sinRot) * 100.0f;
        sideLag = Mth.clamp(sideLag, -20.0f, 20.0f);

        float horizSpeed = Mth.clamp((float) Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 5.0f, 0.0f, 1.0f);
        verticalLag += Mth.sin(Mth.lerp(partialTick, entity.walkDistO, entity.walkDist) * 6.0f) * 32.0f * horizSpeed;

        if (entity.isCrouching()) verticalLag += 25.0f;

        poseStack.mulPose(Axis.XP.rotationDegrees(6.0f + forwardLag / 2.0f + verticalLag));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideLag / 2.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - sideLag / 2.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-(forwardLag / 2.0f)));

        this.getParentModel().renderCloak(
                poseStack,
                buffer.getBuffer(RenderType.entitySolid(capeTexture)),
                packedLight,
                OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    private ResourceLocation getCapeTexture(MinesisEntity entity) {
        if (entity.isHostileModeActive()) return null;

        UUID uuid = entity.getAppearancePlayerUUID();
        if (uuid == null) uuid = entity.getTargetPlayerUUID();
        if (uuid == null) return null;

        // Online player: read cape URL from their GameProfile "textures" property
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                Collection<Property> texProps = info.getProfile().getProperties().get("textures");
                if (!texProps.isEmpty()) {
                    String capeUrl = SkinTextureLoader.extractCapeUrlFromProperties(
                            texProps.iterator().next().getValue());
                    if (capeUrl != null)
                        return SkinTextureLoader.downloadAndRegisterCapeTexture(capeUrl, uuid);
                }
                return null; // online player with no cape
            }
        }

        // Offline fallback: use the texture properties stored on the entity
        String props = entity.getSkinTextureProperties();
        if (props == null || props.isEmpty()) return null;
        String capeUrl = SkinTextureLoader.extractCapeUrlFromProperties(props);
        if (capeUrl == null) return null;
        return SkinTextureLoader.downloadAndRegisterCapeTexture(capeUrl, uuid);
    }
}
