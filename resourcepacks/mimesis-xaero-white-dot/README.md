Mimesis - Xaero Minimap White Dot Resource Pack

Goal:
- Make Mimesis appear as a white point on Xaero Minimap instead of the default yellow.

Deployment Methods:

**Option A: Server Automatically Sends Pack (Recommended for Testing)**
- The Mimesis mod can auto-send this pack to all joining players if configured.
- Start the server with these JVM system properties:
  ```
  -Dmimesis.resourcepack.url=https://example.com/mimesis-xaero-white-dot.zip
  -Dmimesis.resourcepack.sha1=<sha1-hash-of-zip>
  ```
- Players will be prompted to accept the pack (they can accept or decline).
- Compute SHA-1:
  ```powershell
  (Get-FileHash .\mimesis-xaero-white-dot.zip -Algorithm SHA1).Hash
  ```

**Option B: Server-Hosted via server.properties**
- Host `mimesis-xaero-white-dot.zip` on any HTTPS/HTTP server (e.g., your own website, GitHub Releases, etc).
- In `server.properties`:
  ```
  resource-pack=https://example.com/mimesis-xaero-white-dot.zip
  resource-pack-sha1=<sha1-hash>
  resource-pack-prompt=true  # or false to force accept
  ```
- Restart the server.

**Option C: Local Deployment (Single Player / LAN)**
- Place `mimesis-xaero-white-dot.zip` in `server/resourcepacks/` folder.
- In `server.properties`:
  ```
  resource-pack=file:///C:/path/to/server/resourcepacks/mimesis-xaero-white-dot.zip
  resource-pack-sha1=<sha1-hash>
  ```
- Note: Some clients reject `file://` URLs for security reasons. HTTP/HTTPS is preferred.

Troubleshooting:

**Pack Not Detected by Xaero Minimap:**
- Xaero Minimap caches textures. Try:
  1. Clear Xaero Minimap cache (usually in `mods/` folder or config).
  2. Restart the game client completely.
  3. Verify the resource pack is actually accepted/installed (server feedback).
  
- The pack contains PNGs at multiple candidate Xaero paths:
  - `assets/xaerominimap/textures/gui/player_dot.png`
  - `assets/xaerominimap/textures/gui/player.png`
  - `assets/xaerominimap/textures/gui/player_marker.png`
  - `assets/xaerominimap/textures/minimap/player_dot.png`
  
  Different Xaero versions may use different texture keys. If the white dot does not appear, one of these paths should still work.

**Still Not Working?**
- Some Xaero Minimap versions apply color transforms instead of using textures; a resource pack may not change the color in those cases.
- As a workaround, you can:
  1. Use a client-side mod addon for Xaero (requires players to install extra mod).
  2. Ask your server admin to use a server-side color override (requires Xaero plugin/addon).
  3. Contact Xaero Minimap support for texture override documentation specific to your version.

Pack Contents:
- `pack.mcmeta` - Metadata (pack format 10 for 1.20+)
- `assets/xaerominimap/textures/gui/*.png` - White marker PNGs
- `assets/xaerominimap/textures/minimap/*.png` - Alternative path for some Xaero versions

Notes:
- All included PNGs are 1×1 white pixels (fully opaque).
- The pack is server-agnostic and can be used with any Minecraft server setup.
- No server-side code changes are needed; just configure the pack URL in server.properties or via the mod's JVM property.

