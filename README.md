
![Logo](https://raw.githubusercontent.com/JortvanSchijndel/ResourcepackPlus/refs/heads/main/branding/logo.png)

<table align="center" border="0">
  <tr>
    <td align="center">
      <a href="https://bstats.org/plugin/bukkit/ResourcepackPlus/26937">
        <img alt="bStats Servers" src="https://img.shields.io/bstats/servers/26937?logo=gnometerminal&logoColor=5c5c5c&label=Servers%20Running%20RPP&labelColor=ffffff&color=be6cf0&cacheSeconds=600">
      </a>
    </td>
    <td align="center">
      <a href="https://modrinth.com/plugin/resourcepackplus">
        <img alt="Downloads" src="https://img.shields.io/modrinth/dt/resourcepackplus?logo=modrinth&logoColor=5c5c5c&label=Downloads&labelColor=ffffff&color=be6cf0&cacheSeconds=600">
      </a>
    </td>
    <td align="center">
      <a href="https://modrinth.com/plugin/resourcepackplus">
        <img alt="Dynamic YAML Badge" src="https://img.shields.io/badge/dynamic/yaml?url=https%3A%2F%2Fraw.githubusercontent.com%2FJortvanSchijndel%2FResourcepackPlus%2Frefs%2Fheads%2Fmain%2Fsrc%2Fmain%2Fresources%2Fplugin.yml&query=%24.version&logo=v&logoColor=5c5c5c&label=Latest%20Release&labelColor=ffffff&color=be6cf0&cacheSeconds=600">
      </a>
    </td>
  </tr>
</table>

___

# ResourcepackPlus
ResourcepackPlus is a Minecraft plugin that automatically keeps your server’s resource pack up to date. It pulls the pack from GitHub, uploads it to Dropbox, and updates server.properties with the correct SHA-1 and URL. Storing the pack on GitHub makes version control and collaboration easy, while Dropbox provides a direct download link for Minecraft to use.

I originally built this for a Minecraft theme park project, but open-sourced it so anyone can use it for their own server. The plugin is built with the help of AI.

## Setup

### 1. Install the plugin
1. Download the plugin [here](https://modrinth.com/plugin/resourcepackplus)
2. Stop your Minecraft server
3. Upload the downloaded `.jar` file into the `/plugins` folder
4. Start your server

---

### 2. Connect Dropbox
1. Create a Dropbox app here: https://www.dropbox.com/developers/apps/create
2. Choose **Scoped Access**, then **App Folder**, and give it a name
3. In the **Permissions** tab, enable:
   - `account_info.read`
   - `files.metadata.write`
   - `files.metadata.read`
   - `files.content.write`
   - `files.content.read`
   - `sharing.write`
   - `sharing.read`
   - `profile`
4. In the **Settings** tab, copy your **App key** and **App secret**
5. In Minecraft, run: `/rpp dropbox <appkey> <appsecret>`
*(You must be OP or have the permission `resourcepackplus.use`)*
6. Click the link in chat, authorize the app, and paste the authorization code back into chat
7. On https://www.dropbox.com/home, open the **Apps** folder, go to your app, and create a new folder (e.g. `MyResourcepacks`)
8. In Minecraft, run: `/rpp dropbox-path <folder>` The folder will be the folder you just created with a / infront eg. `/MyResourcepacks`


---

### 3. Connect GitHub
1. Go to https://github.com/settings/personal-access-tokens → **Generate new token**
2. Fill in:
- **Token name:** anything you like
- **Expiration:** No Expiration
- **Repository access:** Only select your resourcepack repository
- **Permissions:**
  - Contents → Read Only
  - Metadata → Read Only
3. Generate the token and copy it
4. In Minecraft, run: `/rpp github <token>`
5. Open the plugin’s `config.yml` and set the GitHub URL & branch that is used for tab-completion

---

### 4. Update your resource pack
Whenever you push changes to your resource pack repository, just run:
`/rpp update <GitHub URL> <branch>`

# Issues
If you encounter any problems while using the integration, please [open an issue](https://github.com/JortvanSchijndel/ResourcepackPlus/issues).
Be sure to include as much relevant information as possible, this helps with troubleshooting and speeds up the resolution process.

