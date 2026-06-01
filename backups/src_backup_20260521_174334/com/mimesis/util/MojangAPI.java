package com.mimesis.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility to fetch player profiles and texture properties from Mojang API.
 * Works for any player, online or offline.
 */
public class MojangAPI {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MOJANG_API_USERNAME_TO_UUID = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_API_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Fetch a player's UUID by their username.
     * @param playerName The player's username
     * @return The UUID or null if not found
     */
    public static UUID getUUIDByName(String playerName) {
        try {
            String url = MOJANG_API_USERNAME_TO_UUID + URLEncoder.encode(playerName, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String uuidString = json.get("id").getAsString();
                return parseUUID(uuidString);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch UUID for player " + playerName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch a player's profile including texture properties by UUID.
     * @param uuid The player's UUID
     * @return A JsonObject with name and texture properties, or null if not found
     */
    public static JsonObject getProfileByUUID(UUID uuid) {
        try {
            String uuidString = uuid.toString().replace("-", "");
            String url = MOJANG_API_PROFILE + uuidString + "?unsigned=false";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch profile for UUID " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract texture properties (Base64) from a player profile.
     * @param profile The profile JsonObject from getProfileByUUID
     * @return The Base64-encoded texture properties, or empty string if not found
     */
    public static String extractTexturePropertiesFromProfile(JsonObject profile) {
        try {
            if (profile != null && profile.has("properties")) {
                JsonArray properties = profile.getAsJsonArray("properties");
                for (int i = 0; i < properties.size(); i++) {
                    JsonObject prop = properties.get(i).getAsJsonObject();
                    if (prop.has("name") && "textures".equals(prop.get("name").getAsString())) {
                        if (prop.has("value")) {
                            return prop.get("value").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract texture properties: " + e.getMessage());
        }
        return "";
    }

    /**
     * Fetch texture properties for a player by username (convenience method).
     * @param playerName The player's username
     * @return The Base64-encoded texture properties, or empty string if not found
     */
    public static String getTexturePropertiesByName(String playerName) {
        UUID uuid = getUUIDByName(playerName);
        if (uuid != null) {
            JsonObject profile = getProfileByUUID(uuid);
            if (profile != null) {
                return extractTexturePropertiesFromProfile(profile);
            }
        }
        return "";
    }

    /**
     * Parse UUID string (with or without dashes) into a UUID object.
     */
    private static UUID parseUUID(String uuidString) {
        try {
            if (uuidString.contains("-")) {
                return UUID.fromString(uuidString);
            } else {
                // Insert dashes: 32 chars -> 8-4-4-4-12
                String formatted = uuidString.substring(0, 8) + "-" +
                                   uuidString.substring(8, 12) + "-" +
                                   uuidString.substring(12, 16) + "-" +
                                   uuidString.substring(16, 20) + "-" +
                                   uuidString.substring(20, 32);
                return UUID.fromString(formatted);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to parse UUID: " + uuidString);
            return null;
        }
    }
}
