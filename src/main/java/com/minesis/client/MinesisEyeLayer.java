package com.minesis.client;

import com.minesis.entity.MinesisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class MinesisEyeLayer extends RenderLayer<MinesisEntity, PlayerModel<MinesisEntity>> {

    private static final ResourceLocation EYES_TEXTURE =
            new ResourceLocation("minesis", "textures/entity/minesis_hunt.png");

    public MinesisEyeLayer(LivingEntityRenderer<MinesisEntity, PlayerModel<MinesisEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(@org.jetbrains.annotations.NotNull PoseStack poseStack,
                       @org.jetbrains.annotations.NotNull MultiBufferSource buffer,
                       int packedLight,
                       @org.jetbrains.annotations.NotNull MinesisEntity entity,
                       float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!entity.isHostileModeActive()) return;

        PlayerModel<MinesisEntity> model = this.getParentModel();
        if (model instanceof MinesisHuntModel huntModel) {
            // Render only the head at full brightness — the eye pixels glow in the dark.
            huntModel.renderEyes(poseStack,
                    buffer.getBuffer(RenderType.eyes(EYES_TEXTURE)),
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
