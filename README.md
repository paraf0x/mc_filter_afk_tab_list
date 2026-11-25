# AFK Filter

A client-side Fabric mod that hides AFK players from the tab list, or moves them to the end of it.

## Features

- **Toggle with F6** - Quickly enable/disable the filter
- **Client command** - Use `/afkfilter` to toggle
- **Two modes** - `hide` removes AFK players, `sort` keeps them and lists them last
- **Configurable pattern** - Change what counts as "AFK" in the config
- **Case-insensitive** - Matches "AFK", "afk", "Afk", etc. by default
- **Join notification** - Shows mod status when joining a server

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the mod and place it in your `mods` folder

## Usage

- Press **F6** to toggle the AFK filter on/off
- Or type `/afkfilter` in chat
- Status is shown in the action bar
- `/afkfilter mode sort` keeps AFK players in the tab list and puts them after everyone else
- `/afkfilter mode hide` hides them again (default)
- `/afkfilter mode` shows the current mode. The mode is saved in the config file

## Configuration

Config file: `.minecraft/config/afk-filter.json`

```json
{
  "afkPattern": "AFK",
  "caseSensitive": false,
  "enabledByDefault": true,
  "mode": "hide"
}
```

| Option | Description | Default |
|--------|-------------|---------|
| `afkPattern` | Text pattern to match AFK players | `"AFK"` |
| `caseSensitive` | Match exact case only | `false` |
| `enabledByDefault` | Filter enabled on game start | `true` |
| `mode` | `hide` removes AFK players, `sort` lists them last | `"hide"` |

## Requirements

- Minecraft 26.3
- Fabric Loader ≥0.19.5
- Fabric API

## License

MIT License - see [LICENSE](LICENSE)
