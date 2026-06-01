/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.blaze3d.platform.NativeImage
 *  java.net.http.HttpClient
 *  java.net.http.HttpRequest
 *  java.net.http.HttpResponse
 *  java.net.http.HttpResponse$BodyHandlers
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.resources.ResourceLocation
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mimesis.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SkinTextureLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<String, ResourceLocation>();

    public static String extractSkinUrlFromProperties(String texturePropertiesBase64) {
        try {
            JsonObject skin;
            JsonObject textures;
            if (texturePropertiesBase64 == null || texturePropertiesBase64.isEmpty()) {
                return null;
            }
            byte[] decodedBytes = Base64.getDecoder().decode(texturePropertiesBase64);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString((String)jsonString).getAsJsonObject();
            if (root.has("textures") && (textures = root.getAsJsonObject("textures")).has("SKIN") && (skin = textures.getAsJsonObject("SKIN")).has("url")) {
                return skin.get("url").getAsString();
            }
        }
        catch (Exception e) {
            LOGGER.debug("Failed to extract skin URL from texture properties: " + e.getMessage());
        }
        return null;
    }

    public static String extractSkinModelFromProperties(String texturePropertiesBase64) {
        try {
            JsonObject meta;
            JsonObject skin;
            JsonObject textures;
            if (texturePropertiesBase64 == null || texturePropertiesBase64.isEmpty()) {
                return null;
            }
            byte[] decodedBytes = Base64.getDecoder().decode(texturePropertiesBase64);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString((String)jsonString).getAsJsonObject();
            if (root.has("textures") && (textures = root.getAsJsonObject("textures")).has("SKIN") && (skin = textures.getAsJsonObject("SKIN")).has("metadata") && (meta = skin.getAsJsonObject("metadata")).has("model")) {
                String model = meta.get("model").getAsString();
                return model != null && !model.isEmpty() ? model : null;
            }
        }
        catch (Exception e) {
            LOGGER.debug("Failed to extract skin model from texture properties: " + e.getMessage());
        }
        return null;
    }

    public static ResourceLocation downloadAndRegisterSkinTexture(String skinUrl, UUID entityUUID) {
        try {
            if (skinUrl == null || skinUrl.isEmpty()) {
                return null;
            }
            ResourceLocation cachedLoc = TEXTURE_CACHE.get(skinUrl);
            if (cachedLoc != null) {
                return cachedLoc;
            }
            int hash = skinUrl.hashCode();
            ResourceLocation texLoc = new ResourceLocation("mimesis", "skins/" + Math.abs(hash));
            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(new URI(skinUrl)).GET().build();
                    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() == 200) {
                        byte[] imageData = (byte[])response.body();
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                        NativeImage image = NativeImage.m_85058_((InputStream)inputStream);
                        if (image != null) {
                            Minecraft.m_91087_().execute(() -> {
                                try {
                                    DynamicTexture texture = new DynamicTexture(image);
                                    Minecraft.m_91087_().m_91097_().m_118495_(texLoc, (AbstractTexture)texture);
                                    LOGGER.info("Successfully loaded skin from " + skinUrl);
                                }
                                catch (Exception e) {
                                    LOGGER.debug("Failed to register texture: " + e.getMessage());
                                }
                            });
                        }
                    } else {
                        LOGGER.debug("Failed to download skin: HTTP " + response.statusCode());
                    }
                }
                catch (Exception e) {
                    LOGGER.debug("Failed to download skin texture: " + e.getMessage());
                }
            }, "SkinDownloadThread").start();
            TEXTURE_CACHE.put(skinUrl, texLoc);
            return texLoc;
        }
        catch (Exception e) {
            LOGGER.debug("Failed to start skin texture download: " + e.getMessage());
            return null;
        }
    }

    public static ResourceLocation createResourceLocationFromUrl(String skinUrl, UUID entityUUID) {
        return SkinTextureLoader.downloadAndRegisterSkinTexture(skinUrl, entityUUID);
    }
}

