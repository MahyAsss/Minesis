/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.EntityModel
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.client.model.geom.ModelLayers
 *  net.minecraft.client.multiplayer.PlayerInfo
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.LivingEntityRenderer
 *  net.minecraft.client.renderer.entity.RenderLayerParent
 *  net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
 *  net.minecraft.client.renderer.entity.layers.ItemInHandLayer
 *  net.minecraft.client.renderer.entity.layers.RenderLayer
 *  net.minecraft.client.resources.DefaultPlayerSkin
 *  net.minecraft.resources.ResourceLocation
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mimesis.client;

import com.mimesis.client.SkinTextureLoader;
import com.mimesis.entity.MimesisEntity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MimesisEntityRenderer
extends LivingEntityRenderer<MimesisEntity, HumanoidModel<MimesisEntity>> {
    private static final ResourceLocation HOSTILE_SKIN = new ResourceLocation("mimesis", "textures/entity/mimesis_skin.png");
    private static final Logger LOGGER = LogManager.getLogger();

    public MimesisEntityRenderer(EntityRendererProvider.Context context) {
        super(context, (EntityModel)new HumanoidModel(context.m_174023_(ModelLayers.f_171162_)), 0.5f);
        this.m_115326_((RenderLayer)new HumanoidArmorLayer((RenderLayerParent)this, new HumanoidModel(context.m_174023_(ModelLayers.f_171164_)), new HumanoidModel(context.m_174023_(ModelLayers.f_171165_)), context.m_266367_()));
        this.m_115326_((RenderLayer)new ItemInHandLayer((RenderLayerParent)this, context.m_234598_()));
    }

    public ResourceLocation getTextureLocation(MimesisEntity pEntity) {
        if (pEntity.isHostileModeActive()) {
            return HOSTILE_SKIN;
        }
        UUID appearancePlayerUUID = pEntity.getAppearancePlayerUUID();
        if (appearancePlayerUUID != null) {
            ResourceLocation texLoc;
            String skinUrl;
            PlayerInfo playerInfo;
            if (Minecraft.m_91087_().m_91403_() != null && (playerInfo = Minecraft.m_91087_().m_91403_().m_104949_(appearancePlayerUUID)) != null) {
                return playerInfo.m_105337_();
            }
            String textureProperties = pEntity.getSkinTextureProperties();
            if (textureProperties != null && !textureProperties.isEmpty() && (skinUrl = SkinTextureLoader.extractSkinUrlFromProperties(textureProperties)) != null && (texLoc = SkinTextureLoader.downloadAndRegisterSkinTexture(skinUrl, appearancePlayerUUID)) != null) {
                return texLoc;
            }
            return DefaultPlayerSkin.m_118627_((UUID)appearancePlayerUUID);
        }
        UUID targetPlayerUUID = pEntity.getTargetPlayerUUID();
        if (targetPlayerUUID != null) {
            PlayerInfo playerInfo;
            if (Minecraft.m_91087_().m_91403_() != null && (playerInfo = Minecraft.m_91087_().m_91403_().m_104949_(targetPlayerUUID)) != null) {
                return playerInfo.m_105337_();
            }
            return DefaultPlayerSkin.m_118627_((UUID)targetPlayerUUID);
        }
        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }

    protected RenderType getRenderType(MimesisEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(entity);
        return RenderType.m_110473_((ResourceLocation)texture);
    }
}

