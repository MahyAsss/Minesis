package com.mimesis.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import com.mimesis.entity.MimesisEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MimesisEntityRenderer extends LivingEntityRenderer<MimesisEntity, HumanoidModel<MimesisEntity>> {
    private static final ResourceLocation HOSTILE_SKIN = new ResourceLocation("mimesis", "textures/entity/mimesis_skin.png");
    private static final Logger LOGGER = LogManager.getLogger();

    public MimesisEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        // Skin outer layer is handled by merged textures; no extra layer required
    }
    
    @Override
    public ResourceLocation getTextureLocation(MimesisEntity pEntity) {
        if (pEntity.isHostileModeActive()) {
            return HOSTILE_SKIN;
        }

        UUID appearancePlayerUUID = pEntity.getAppearancePlayerUUID();
        if (appearancePlayerUUID != null) {
            // Try to get from PlayerInfo first (online players)
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(appearancePlayerUUID);
                if (playerInfo != null) {
                    return playerInfo.getSkinLocation();
                }
            }
            
            // For offline players, decode and download texture from properties
            String textureProperties = pEntity.getSkinTextureProperties();
            if (textureProperties != null && !textureProperties.isEmpty()) {
                String skinUrl = SkinTextureLoader.extractSkinUrlFromProperties(textureProperties);
                if (skinUrl != null) {
                    // This method downloads the texture and returns a valid ResourceLocation
                    ResourceLocation texLoc = SkinTextureLoader.downloadAndRegisterSkinTexture(skinUrl, appearancePlayerUUID);
                    if (texLoc != null) {
                        return texLoc;
                    }
                }
            }
            
            return DefaultPlayerSkin.getDefaultSkin(appearancePlayerUUID);
        }

        UUID targetPlayerUUID = pEntity.getTargetPlayerUUID();
        if (targetPlayerUUID != null) {
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(targetPlayerUUID);
                if (playerInfo != null) {
                    return playerInfo.getSkinLocation();
                }
            }
            return DefaultPlayerSkin.getDefaultSkin(targetPlayerUUID);
        }

        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    protected RenderType getRenderType(MimesisEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(entity);
        return RenderType.entityTranslucent(texture);
    }
}
