package com.minesis.voice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.minesis.utils.AudioUtils;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@ForgeVoicechatPlugin
public class MinesisVoiceChatPlugin implements VoicechatPlugin {

    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private static VoicechatServerApi cachedApi = null;

    @Override
    public String getPluginId() { return "minesis_voice_bridge"; }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApi) {
            cachedApi = serverApi;
            VoicePlaybackService.setVoicechatApi(serverApi);
            VoicePersistenceManager.setApi(serverApi);
        }
    }

    public static VoicechatServerApi getVoicechatApi() { return cachedApi; }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onVoicechatServerStopped);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null || senderConnection.getPlayer() == null) return;

        Object rawPlayer = senderConnection.getPlayer().getPlayer();
        if (!(rawPlayer instanceof ServerPlayer serverPlayer)) return;

        UUID playerUUID = serverPlayer.getUUID();
        String playerName = serverPlayer.getGameProfile().getName();

        VoiceCaptureHandler handler = VoiceManager.getOrCreateCaptureHandler(playerUUID, playerName);

        OpusDecoder decoder = decoders.computeIfAbsent(playerUUID,
                key -> event.getVoicechat().createDecoder());
        if (decoder == null || decoder.isClosed()) return;

        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData == null || opusData.length == 0) return;

        short[] pcmShorts = decoder.decode(opusData);
        if (pcmShorts == null || pcmShorts.length == 0) return;

        byte[] pcmBytes = AudioUtils.shortsToBytes(pcmShorts);
        PlayerActivityTracker.markSpeaking(playerUUID);
        VoiceContext context = computePlayerContext(serverPlayer);
        handler.onAudioData(pcmBytes, context);
    }

    /**
     * Determines what a player is doing right now so the clip can be tagged.
     * Called on every audio packet but is O(1) — no allocations on the common path.
     */
    private VoiceContext computePlayerContext(ServerPlayer player) {
        // Crafting table open
        if (player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu)
            return VoiceContext.CRAFTING;

        // Recently broke a block → mining
        if (PlayerActivityTracker.isMining(player.getUUID()))
            return VoiceContext.MINING;

        // In water
        if (player.isSwimming() || player.isUnderWater())
            return VoiceContext.SWIMMING;

        // Movement
        if (player.isSprinting()) return VoiceContext.RUNNING;

        Vec3 vel = player.getDeltaMovement();
        if (vel.x * vel.x + vel.z * vel.z > 0.005D) return VoiceContext.WALKING;

        return VoiceContext.IDLE;
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        UUID playerUUID = event.getPlayerUuid();
        VoiceManager.unregisterCaptureHandler(playerUUID);
        OpusDecoder decoder = decoders.remove(playerUUID);
        if (decoder != null && !decoder.isClosed()) decoder.close();
    }

    private void onVoicechatServerStopped(VoicechatServerStoppedEvent event) {
        VoiceManager.stopAllCaptures();
        decoders.values().forEach(d -> { if (d != null && !d.isClosed()) d.close(); });
        decoders.clear();
    }
}
