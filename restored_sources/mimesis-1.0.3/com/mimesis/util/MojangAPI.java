/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  java.net.http.HttpClient
 *  java.net.http.HttpRequest
 *  java.net.http.HttpResponse
 *  java.net.http.HttpResponse$BodyHandlers
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mimesis.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MojangAPI {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MOJANG_API_USERNAME_TO_UUID = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_API_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static UUID getUUIDByName(String playerName) {
        try {
            String url = MOJANG_API_USERNAME_TO_UUID + URLEncoder.encode((String)playerName, (Charset)StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString((String)((String)response.body())).getAsJsonObject();
                String uuidString = json.get("id").getAsString();
                return MojangAPI.parseUUID(uuidString);
            }
        }
        catch (Exception e) {
            LOGGER.debug("Failed to fetch UUID for player " + playerName + ": " + e.getMessage());
        }
        return null;
    }

    public static JsonObject getProfileByUUID(UUID uuid) {
        try {
            String uuidString = uuid.toString().replace("-", "");
            String url = MOJANG_API_PROFILE + uuidString + "?unsigned=false";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return JsonParser.parseString((String)((String)response.body())).getAsJsonObject();
            }
        }
        catch (Exception e) {
            LOGGER.debug("Failed to fetch profile for UUID " + String.valueOf(uuid) + ": " + e.getMessage());
        }
        return null;
    }

    public static String extractTexturePropertiesFromProfile(JsonObject profile) {
        try {
            if (profile != null && profile.has("properties")) {
                JsonArray properties = profile.getAsJsonArray("properties");
                for (int i = 0; i < properties.size(); ++i) {
                    JsonObject prop = properties.get(i).getAsJsonObject();
                    if (!prop.has("name") || !"textures".equals(prop.get("name").getAsString()) || !prop.has("value")) continue;
                    return prop.get("value").getAsString();
                }
            }
        }
        catch (Exception e) {
            LOGGER.debug("Failed to extract texture properties: " + e.getMessage());
        }
        return "";
    }

    public static String getTexturePropertiesByName(String playerName) {
        JsonObject profile;
        UUID uuid = MojangAPI.getUUIDByName(playerName);
        if (uuid != null && (profile = MojangAPI.getProfileByUUID(uuid)) != null) {
            return MojangAPI.extractTexturePropertiesFromProfile(profile);
        }
        return "";
    }

    private static UUID parseUUID(String uuidString) {
        try {
            if (uuidString.contains("-")) {
                return UUID.fromString(uuidString);
            }
            String formatted = uuidString.substring(0, 8) + "-" + uuidString.substring(8, 12) + "-" + uuidString.substring(12, 16) + "-" + uuidString.substring(16, 20) + "-" + uuidString.substring(20, 32);
            return UUID.fromString(formatted);
        }
        catch (Exception e) {
            LOGGER.debug("Failed to parse UUID: " + uuidString);
            return null;
        }
    }
}

