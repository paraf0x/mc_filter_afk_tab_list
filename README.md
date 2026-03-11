# AFK Filter

A client-side Fabric mod that hides AFK players from the tab list.

## Features

- **Toggle with F6** - Quickly enable/disable the filter
- **Client command** - Use `/afkfilter` to toggle
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

## Configuration

Config file: `.minecraft/config/afk-filter.json`

```json
{
  "afkPattern": "AFK",
  "caseSensitive": false,
  "enabledByDefault": true
}
```

| Option | Description | Default |
|--------|-------------|---------|
| `afkPattern` | Text pattern to match AFK players | `"AFK"` |
| `caseSensitive` | Match exact case only | `false` |
| `enabledByDefault` | Filter enabled on game start | `true` |

## Requirements

- Minecraft 1.21.4
- Fabric Loader ≥0.15.0
- Fabric API

## License

MIT License - see [LICENSE](LICENSE)
