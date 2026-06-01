package com.mimesis.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SkinTextureLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    /**
     * Decodes texture properties from Base64 and extracts the skin URL.
     * Texture properties format: Base64 encoded JSON with structure:
     * { "textures": { "SKIN": { "url": "http://..." } } }
     */
    public static String extractSkinUrlFromProperties(String texturePropertiesBase64) {
        try {
            if (texturePropertiesBase64 == null || texturePropertiesBase64.isEmpty()) {
                return null;
            }

            // Decode Base64
            byte[] decodedBytes = Base64.getDecoder().decode(texturePropertiesBase64);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);

            // Parse JSON
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            if (root.has("textures")) {
                JsonObject textures = root.getAsJsonObject("textures");
                if (textures.has("SKIN")) {
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    if (skin.has("url")) {
                        return skin.get("url").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract skin URL from texture properties: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extracts the skin model metadata (e.g., "slim") if present in the Base64 properties.
     * Returns "slim" or the model name, or null when not present.
     */
    public static String extractSkinModelFromProperties(String texturePropertiesBase64) {
        try {
            if (texturePropertiesBase64 == null || texturePropertiesBase64.isEmpty()) {
                return null;
            }

            byte[] decodedBytes = Base64.getDecoder().decode(texturePropertiesBase64);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            if (root.has("textures")) {
                JsonObject textures = root.getAsJsonObject("textures");
                if (textures.has("SKIN")) {
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    if (skin.has("metadata")) {
                        JsonObject meta = skin.getAsJsonObject("metadata");
                        if (meta.has("model")) {
                            String model = meta.get("model").getAsString();
                            return model != null && !model.isEmpty() ? model : null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract skin model from texture properties: " + e.getMessage());
        }
        return null;
    }

    /**
     * Downloads a skin texture from a URL and registers it with TextureManager.
     * Properly handles the image download and texture creation asynchronously.
     * Returns a ResourceLocation that can be used for rendering.
     */
    public static ResourceLocation downloadAndRegisterSkinTexture(String skinUrl, UUID entityUUID) {
        try {
            if (skinUrl == null || skinUrl.isEmpty()) {
                return null;
            }

            // Use URL as cache key to avoid re-downloading the same texture
            ResourceLocation cachedLoc = TEXTURE_CACHE.get(skinUrl);
            if (cachedLoc != null) {
                return cachedLoc;
            }

            // Create a unique ResourceLocation for this texture
            int hash = skinUrl.hashCode();
            ResourceLocation texLoc = new ResourceLocation("mimesis", "skins/" + Math.abs(hash));

            // Start async download on a background thread
            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(skinUrl))
                        .GET()
                        .build();

                    HttpResponse<byte[]> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                    );

                    if (response.statusCode() == 200) {
                        byte[] imageData = response.body();
                        
                        // Load the image from bytes
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                        NativeImage image = NativeImage.read(inputStream);
                        if (image != null) {
                            // Register on main thread
                            Minecraft.getInstance().execute(() -> {
                                try {
                                    DynamicTexture texture = new DynamicTexture(image);
                                    Minecraft.getInstance().getTextureManager().register(texLoc, texture);
                                    LOGGER.info("Successfully loaded skin from " + skinUrl);
                                } catch (Exception e) {
                                    LOGGER.debug("Failed to register texture: " + e.getMessage());
                                }
                            });
                        }
                    } else {
                        LOGGER.debug("Failed to download skin: HTTP " + response.statusCode());
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to download skin texture: " + e.getMessage());
                }
            }, "SkinDownloadThread").start();

            TEXTURE_CACHE.put(skinUrl, texLoc);
            return texLoc;
        } catch (Exception e) {
            LOGGER.debug("Failed to start skin texture download: " + e.getMessage());
        }
        return null;
    }

    /**
     * Legacy method - now downloads the actual texture.
     */
    public static ResourceLocation createResourceLocationFromUrl(String skinUrl, UUID entityUUID) {
        return downloadAndRegisterSkinTexture(skinUrl, entityUUID);
    }
}
