package com.minesis.client;

import net.minecraft.util.Mth;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.UUID;

import com.minesis.entity.MinesisEntity;

/**
 * Renders MinesisEntity using PlayerModel so both the inner skin layer
 * AND the outer skin layer (jacket, sleeves, pants) are displayed correctly.
 *
 * Two models are kept in memory (wide/Steve and slim/Alex) and the correct
 * one is selected per entity before each render call.
 */
public class MinesisEntityRenderer extends LivingEntityRenderer<MinesisEntity, PlayerModel<MinesisEntity>> {

    private static final ResourceLocation HOSTILE_SKIN =
            new ResourceLocation("minesis", "textures/entity/minesis_hunt.png");
    private static final ResourceLocation SVC_SPEAKER_ICON =
            new ResourceLocation("voicechat", "textures/icons/speaker.png");

    // Wide (Steve) model — default
    private final PlayerModel<MinesisEntity> wideModel;
    // Slim (Alex) model — used when getSkinModel() == "slim"
    private final PlayerModel<MinesisEntity> slimModel;
    // Custom monster model used during hunt (hostile) mode
    private final MinesisHuntModel huntModel;

    public MinesisEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wideModel = this.model; // kept from super()

        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.huntModel = new MinesisHuntModel(context.bakeLayer(MinesisHuntModel.LAYER_LOCATION));

        // Armor layers (wide models cover the vast majority of skins; slim arm
        // armor is a minor visual difference and not worth two separate layer sets).
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new MinesisCapeLayer(this));
        this.addLayer(new MinesisEyeLayer(this));
    }

    // ── Model selection ───────────────────────────────────────────────────────

    @Override
    public void render(MinesisEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = entity.isHostileModeActive() ? this.huntModel : resolvePlayerModel(entity);

        int tt = entity.getTransformTimer();
        MultiBufferSource renderBuffer = buffer;
        if (tt > 0) {
            float progress  = (60 - tt) / 60.0F;         // 0 → 1 as freeze runs out
            float brightness = 1.0F - progress * 0.9F;   // 1.0 → 0.1 (quasi-noir en fin)
            renderBuffer = type -> new DarkenedConsumer(buffer.getBuffer(type), brightness);
        }

        super.render(entity, entityYaw, partialTick, poseStack, renderBuffer, packedLight);
        if (entity.isSpeaking() && this.entityRenderDispatcher.distanceToSqr(entity) < 4096.0) {
            renderSpeakerIcon(entity.getDisplayName(), entity.getNameTagOffsetY(), poseStack, buffer, packedLight);
        }
    }

    /** Wraps a VertexConsumer and multiplies each vertex color by {@code f}. */
    private static final class DarkenedConsumer implements VertexConsumer {
        private final VertexConsumer base;
        private final float f;
        DarkenedConsumer(VertexConsumer base, float f) { this.base = base; this.f = f; }

        @Override public VertexConsumer vertex(double x, double y, double z)     { base.vertex(x, y, z);           return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a)        { base.color((int)(r*f),(int)(g*f),(int)(b*f),a); return this; }
        @Override public VertexConsumer uv(float u, float v)                     { base.uv(u, v);                  return this; }
        @Override public VertexConsumer overlayCoords(int u, int v)              { base.overlayCoords(u, v);       return this; }
        @Override public VertexConsumer uv2(int u, int v)                        { base.uv2(u, v);                 return this; }
        @Override public VertexConsumer normal(float x, float y, float z)        { base.normal(x, y, z);           return this; }
        @Override public void endVertex()                                         { base.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a)           { base.defaultColor((int)(r*f),(int)(g*f),(int)(b*f),a); }
        @Override public void unsetDefaultColor()                                 { base.unsetDefaultColor(); }
    }

    private void renderSpeakerIcon(Component name, float yOffset,
            PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0, yOffset, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        poseStack.translate(0.0, -1.0, 0.0);

        float offset = this.getFont().width(name) / 2.0F + 2.0F;
        Matrix4f mat = poseStack.last().pose();

        VertexConsumer vc = buffer.getBuffer(RenderType.text(SVC_SPEAKER_ICON));
        vc.vertex(mat, offset,        10F, 0F).color(255, 255, 255, 255).uv(0F, 1F).uv2(light).endVertex();
        vc.vertex(mat, offset + 10F,  10F, 0F).color(255, 255, 255, 255).uv(1F, 1F).uv2(light).endVertex();
        vc.vertex(mat, offset + 10F,   0F, 0F).color(255, 255, 255, 255).uv(1F, 0F).uv2(light).endVertex();
        vc.vertex(mat, offset,          0F, 0F).color(255, 255, 255, 255).uv(0F, 0F).uv2(light).endVertex();

        VertexConsumer vcST = buffer.getBuffer(RenderType.textSeeThrough(SVC_SPEAKER_ICON));
        vcST.vertex(mat, offset,        10F, 0F).color(255, 255, 255, 32).uv(0F, 1F).uv2(light).endVertex();
        vcST.vertex(mat, offset + 10F,  10F, 0F).color(255, 255, 255, 32).uv(1F, 1F).uv2(light).endVertex();
        vcST.vertex(mat, offset + 10F,   0F, 0F).color(255, 255, 255, 32).uv(1F, 0F).uv2(light).endVertex();
        vcST.vertex(mat, offset,          0F, 0F).color(255, 255, 255, 32).uv(0F, 0F).uv2(light).endVertex();

        poseStack.popPose();
    }

    /**
     * Returns slim or wide model using a 3-tier detection:
     *  1. Server-synced SKIN_MODEL metadata (fastest)
     *  2. PlayerInfo game-profile texture metadata (online players, client-side)
     *  3. Pixel analysis of the downloaded texture (catches skins designed slim
     *     but uploaded without selecting the slim flag in Mojang's skin uploader)
     */
    private PlayerModel<MinesisEntity> resolvePlayerModel(MinesisEntity entity) {
        // Tier 1: server-synced metadata
        if ("slim".equals(entity.getSkinModel())) return this.slimModel;

        net.minecraft.client.multiplayer.ClientPacketListener conn =
                Minecraft.getInstance().getConnection();
        UUID uuid = entity.getAppearancePlayerUUID();
        if (uuid == null) uuid = entity.getTargetPlayerUUID();

        String skinUrl = null;

        // Tier 2: PlayerInfo profile metadata + extract skin URL for tier 3
        if (uuid != null && conn != null) {
            PlayerInfo info = conn.getPlayerInfo(uuid);
            if (info != null) {
                try {
                    java.util.Collection<com.mojang.authlib.properties.Property> texProps =
                            info.getProfile().getProperties().get("textures");
                    if (!texProps.isEmpty()) {
                        String b64 = texProps.iterator().next().getValue();
                        if ("slim".equals(SkinTextureLoader.extractSkinModelFromProperties(b64)))
                            return this.slimModel;
                        skinUrl = SkinTextureLoader.extractSkinUrlFromProperties(b64);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Fallback URL from entity synced properties (offline / non-online players)
        if (skinUrl == null) {
            String props = entity.getSkinTextureProperties();
            if (props != null && !props.isEmpty())
                skinUrl = SkinTextureLoader.extractSkinUrlFromProperties(props);
        }

        // Tier 3: pixel-based detection (handles slim skins without metadata flag).
        // For online players the texture goes through PlayerInfo, not SkinTextureLoader,
        // so the cache may be empty — trigger an independent download for detection.
        if (skinUrl != null) {
            Boolean slimDetected = SkinTextureLoader.isSlimDetected(skinUrl);
            if (Boolean.TRUE.equals(slimDetected)) return this.slimModel;
            if (slimDetected == null) SkinTextureLoader.triggerSlimDetection(skinUrl);
        }

        return this.wideModel;
    }

    // ── Texture ───────────────────────────────────────────────────────────────

    @Override
    public ResourceLocation getTextureLocation(MinesisEntity entity) {
        if (entity.isHostileModeActive()) {
            return HOSTILE_SKIN;
        }

        UUID appearanceUUID = entity.getAppearancePlayerUUID();
        if (appearanceUUID != null) {
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(appearanceUUID);
                if (info != null) return info.getSkinLocation();
            }

            String props = entity.getSkinTextureProperties();
            if (props != null && !props.isEmpty()) {
                String url = SkinTextureLoader.extractSkinUrlFromProperties(props);
                if (url != null) {
                    // Trigger download (no-op if already started)
                    ResourceLocation loc = SkinTextureLoader.downloadAndRegisterSkinTexture(url, appearanceUUID);
                    // Only return the DynamicTexture location once it is actually registered —
                    // returning an unregistered location gives pink on Xaero's Minimap (and in game).
                    if (loc != null && SkinTextureLoader.isTextureReady(url)) return loc;
                }
            }

            return DefaultPlayerSkin.getDefaultSkin(appearanceUUID);
        }

        UUID targetUUID = entity.getTargetPlayerUUID();
        if (targetUUID != null) {
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(targetUUID);
                if (info != null) return info.getSkinLocation();
            }
            return DefaultPlayerSkin.getDefaultSkin(targetUUID);
        }

        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }

    // ── Rendering overrides ───────────────────────────────────────────────────

    /**
     * Replicates the swimming body-tilt from PlayerRenderer.setupRotations().
     * LivingEntityRenderer does not include this rotation, so without this override
     * the entity stands upright even when Pose.SWIMMING is active.
     *
     * No pivot translation: the entity origin stays at its feet, and the -90° rotation
     * around X lays the body flat (head points in the movement direction).
     * Using a fixed -90° avoids the mob's variable head pitch skewing the angle.
     */
    @Override
    protected void setupRotations(MinesisEntity entity, PoseStack poseStack,
            float ageInTicks, float rotationYaw, float partialTick) {
        // Apply yaw FIRST so the subsequent X tilt works in the entity's local frame,
        // not world space. Reversing this order causes the body to spin (kebab effect)
        // because XP then acts on a non-yawed coordinate system.
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        if (entity.isSwimming()) {
            float swimAmount = entity.getSwimAmount(partialTick);
            if (swimAmount > 0.0F) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(swimAmount * -90.0F));
            }
        }
        int tt = entity.getTransformTimer();
        if (tt > 0) {
            float progress = (60 - tt) / 60.0F; // 0 at start → ~1 at end
            float shake = Mth.sin(ageInTicks * 4.0F) * 0.05F * (0.3F + 0.7F * progress);
            poseStack.translate(shake, 0.0F, 0.0F);
        }
    }

    @Override
    protected RenderType getRenderType(MinesisEntity entity, boolean bodyVisible,
            boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(this.getTextureLocation(entity));
    }

    @Override
    protected boolean shouldShowName(MinesisEntity entity) {
        return !entity.getTargetPlayerName().isEmpty();
    }
}
