# ClientSideSkins

**ClientSideSkins** is a purely client-side Fabric mod for Minecraft that loads custom player skins, capes, and elytra textures from local `.png` files and works in Singleplayer, LAN, and public multiplayer servers.

**How To Use**
   - On the first launch of a Minecraft instance with this mod installed, it creates a folder titled `ClientSideSkins/` along with three subfolders:
     - `ClientSideSkins/skins/` — Place player skin `.png` files here (`64x64`, legacy `64x32` automatically converted to `64x64`, or HD `128x128`+). Any `.png` placed directly in `ClientSideSkins/` is also detected as a skin.
     - `ClientSideSkins/capes/` — Place custom cape `.png` files here (`64x32` or HD).
     - `ClientSideSkins/elytras/` — Place custom elytra `.png` files here (`64x32` or HD).
  - Next, open the menu options to select your custom skin.
**In-Game Options Menu Selector**:
   - Accessible directly via **Options -> "Client Side Skins..."** or **Options -> Skin Customization -> "Client Side Skins..."**.
   - Browse discovered `.png` files across **Skins**, **Capes**, and **Elytras** tabs with live 2D thumbnails and a rotatable 3D player model preview (`PlayerSkinWidget`).
   - Toggle between **Auto**, **Wide (Classic)**, and **Slim (Alex)** arm models.
   - Includes **Rescan Folder** (hot-reloads PNGs without restarting Minecraft) and **Open Folder** buttons.
**Client-Side Commands (`/clientsideskins` & `/css`)**:
   - Works on all servers (Singleplayer, LAN, and public servers) with zero operator permissions required:
     - `/clientsideskins` (or `/css`, `/css menu`, `/css gui`) — Opens the in-game skin selector GUI.
     - `/css skin set <file> [auto|wide|slim]` — Equips a skin `.png` directly (with tab-completion for all discovered skin PNGs).
     - `/css skin clear` (or `reset`) — Resets your skin to default.
     - `/css model <auto|wide|slim>` — Switches arm model between Auto-detect, Wide, and Slim.
     - `/css cape set <file>` — Equips a cape `.png` directly (with tab-completion).
     - `/css cape clear` (or `reset`) — Resets your cape to default.
     - `/css elytra set <file>` — Equips an elytra `.png` directly (with tab-completion).
     - `/css elytra clear` (or `reset`) — Resets your elytra to default.
     - `/css toggle [on|off]` — Enables or disables all custom skin overrides.
     - `/css reload` — Rescans the `ClientSideSkins` folders and reloads textures from disk.
     - `/css list [skins|capes|elytras]` — Lists all discovered `.png` files in chat.
     - `/css status` — Displays currently active skin, model, cape, elytra, and folder path.
     - `/css folder [skins|capes|elytras]` — Opens the `ClientSideSkins` folder in the system file explorer.


