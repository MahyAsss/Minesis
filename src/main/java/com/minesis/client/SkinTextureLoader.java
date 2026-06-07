package com.minesis.client;

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
    // URLs dont la DynamicTexture est effectivement enregistrée dans TextureManager
    private static final java.util.Set<String> REGISTERED_TEXTURES =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    // Détection slim par pixels : true = slim, false = wide, absent = pas encore chargé
    private static final Map<String, Boolean> SLIM_SKIN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> SLIM_DETECT_IN_PROGRESS =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * Retourne true si la texture à cette URL est téléchargée et enregistrée dans TextureManager.
     * Tant que le téléchargement async n'est pas terminé, retourne false.
     */
    public static boolean isTextureReady(String url) {
        return REGISTERED_TEXTURES.contains(url);
    }

    /**
     * Retourne true si le skin est détecté slim, false si wide,
     * ou null si l'analyse pixel n'a pas encore eu lieu.
     */
    public static Boolean isSlimDetected(String skinUrl) {
        return SLIM_SKIN_CACHE.get(skinUrl);
    }

    /**
     * Lance un téléchargement léger du skin uniquement pour la détection slim/wide
     * (sans enregistrer de texture). Utile pour les joueurs en ligne dont le skin
     * est rendu via PlayerInfo.getSkinLocation() et ne passe pas par downloadAndRegisterSkinTexture.
     * Sans effet si l'analyse est déjà en cours ou terminée.
     */
    public static void triggerSlimDetection(String skinUrl) {
        if (skinUrl == null || skinUrl.isEmpty()) return;
        if (SLIM_SKIN_CACHE.containsKey(skinUrl)) return;
        if (!SLIM_DETECT_IN_PROGRESS.add(skinUrl)) return; // déjà en cours
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(skinUrl)).GET().build();
                HttpResponse<byte[]> response = client.send(
                        request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(response.body()));
                    if (img != null) {
                        SLIM_SKIN_CACHE.put(skinUrl, detectSlimFromImage(img));
                        img.close();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                SLIM_DETECT_IN_PROGRESS.remove(skinUrl);
            }
        }, "MinesisSlimDetect").start();
    }

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

    public static String extractCapeUrlFromProperties(String texturePropertiesBase64) {
        try {
            if (texturePropertiesBase64 == null || texturePropertiesBase64.isEmpty()) return null;
            byte[] decodedBytes = Base64.getDecoder().decode(texturePropertiesBase64);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            if (root.has("textures")) {
                JsonObject textures = root.getAsJsonObject("textures");
                if (textures.has("CAPE")) {
                    JsonObject cape = textures.getAsJsonObject("CAPE");
                    if (cape.has("url")) return cape.get("url").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract cape URL from texture properties: " + e.getMessage());
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
     * Converts old 64×32 format to 64×64 automatically.
     */
    public static ResourceLocation downloadAndRegisterSkinTexture(String skinUrl, UUID entityUUID) {
        return downloadAndRegisterTexture(skinUrl, true);
    }

    /**
     * Downloads a cape texture from a URL and registers it with TextureManager.
     * Does NOT apply the 64×32→64×64 skin conversion (capes are legitimately 64×32).
     */
    public static ResourceLocation downloadAndRegisterCapeTexture(String capeUrl, UUID entityUUID) {
        return downloadAndRegisterTexture(capeUrl, false);
    }

    /**
     * Core download+register implementation.
     * @param convertOldFormat true for skin textures (expand 64×32→64×64), false for capes.
     */
    private static ResourceLocation downloadAndRegisterTexture(String url, boolean convertOldFormat) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }

            // Use URL as cache key to avoid re-downloading the same texture
            ResourceLocation cachedLoc = TEXTURE_CACHE.get(url);
            if (cachedLoc != null) {
                return cachedLoc;
            }

            // Create a unique ResourceLocation for this texture
            int hash = url.hashCode();
            ResourceLocation texLoc = new ResourceLocation("minesis", "skins/" + Math.abs(hash));

            // Start async download on a background thread
            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .GET()
                        .build();

                    HttpResponse<byte[]> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                    );

                    if (response.statusCode() == 200) {
                        byte[] imageData = response.body();

                        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                        NativeImage image = NativeImage.read(inputStream);
                        if (image != null) {
                            if (convertOldFormat && image.getHeight() < 64) {
                                NativeImage converted = expandOldFormatSkin(image);
                                image.close();
                                image = converted;
                            }
                            if (convertOldFormat) {
                                SLIM_SKIN_CACHE.put(url, detectSlimFromImage(image));
                            }
                            final NativeImage finalImage = image;
                            Minecraft.getInstance().execute(() -> {
                                try {
                                    DynamicTexture texture = new DynamicTexture(finalImage);
                                    Minecraft.getInstance().getTextureManager().register(texLoc, texture);
                                    REGISTERED_TEXTURES.add(url);
                                    LOGGER.info("Successfully loaded texture from " + url);
                                } catch (Exception e) {
                                    LOGGER.debug("Failed to register texture: " + e.getMessage());
                                }
                            });
                        }
                    } else {
                        LOGGER.debug("Failed to download texture: HTTP " + response.statusCode());
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to download texture: " + e.getMessage());
                }
            }, "SkinDownloadThread").start();

            TEXTURE_CACHE.put(url, texLoc);
            return texLoc;
        } catch (Exception e) {
            LOGGER.debug("Failed to start texture download: " + e.getMessage());
        }
        return null;
    }

    /**
     * Legacy method - now downloads the actual texture.
     */
    public static ResourceLocation createResourceLocationFromUrl(String skinUrl, UUID entityUUID) {
        return downloadAndRegisterSkinTexture(skinUrl, entityUUID);
    }

    /**
     * Convertit un skin 64×32 (ancien format pré-1.8) en 64×64.
     * Le bras droit et la jambe droite sont miroirs vers leur équivalent gauche
     * (groupes de faces échangés + inversion horizontale dans chaque face de 4px).
     * Les zones outer-layer (veste, manches, pantalon) restent transparentes.
     */
    private static NativeImage expandOldFormatSkin(NativeImage src) {
        NativeImage dst = new NativeImage(64, 64, false);
        // Initialiser tout en transparent
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                dst.setPixelRGBA(x, y, 0);
        // Copier le skin original dans la moitié supérieure
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 32; y++)
                dst.setPixelRGBA(x, y, src.getPixelRGBA(x, y));
        // Jambe droite (0,16) → Jambe gauche (16,48) avec miroir
        copyMirroredLimb(src, dst, 0, 16, 16, 48);
        // Bras droit (40,16) → Bras gauche (32,48) avec miroir
        copyMirroredLimb(src, dst, 40, 16, 32, 48);
        return dst;
    }

    /**
     * Copie une zone de membre 16×16 en miroir horizontal correct.
     * Layout des colonnes (4px chacune) : [interne][avant][externe][arrière]
     * Le miroir échange interne↔externe et retourne horizontalement chaque face.
     */
    private static void copyMirroredLimb(NativeImage src, NativeImage dst,
            int sx, int sy, int dx, int dy) {
        final int[] destGroup = {2, 1, 0, 3}; // interne(0)↔externe(2), avant(1) et arrière(3) restent
        for (int y = 0; y < 16; y++) {
            for (int g = 0; g < 4; g++) {
                int dg = destGroup[g];
                for (int fx = 0; fx < 4; fx++) {
                    int pixel = src.getPixelRGBA(sx + g * 4 + fx, sy + y);
                    dst.setPixelRGBA(dx + dg * 4 + (3 - fx), dy + y, pixel);
                }
            }
        }
    }

    /**
     * Détecte si un skin est slim en analysant les pixels de la zone utilisée
     * uniquement par le bras wide (face arrière : x=54-55, y=20-27 dans un 64×64).
     * Si tous ces pixels sont transparents → slim. Sinon → wide.
     * Les skins 64×32 (ancien format) sont toujours wide.
     */
    private static boolean detectSlimFromImage(NativeImage img) {
        if (img.getWidth() < 64 || img.getHeight() < 64) return false;
        for (int x = 54; x <= 55; x++) {
            for (int y = 20; y <= 27; y++) {
                int pixel = img.getPixelRGBA(x, y);
                if (((pixel >> 24) & 0xFF) != 0) return false;
            }
        }
        return true;
    }
}
