# Modeconomia - Economy Mod for Cobblemon

Economy mod for Minecraft Fabric with Cobblemon integration. Adds a complete economy system with daily missions, shop, marketplace, and AFK rewards.

**Active Minecraft version:** 1.21.11 (using Stonecutter for multi-version support)

## Features

### Daily Missions System
- **5 random daily missions** that auto-generate every 24 hours
- **3 CobbleCoins reward** per mission
- **110+ mission types** including:
  - **Vanilla**: Break blocks, kill mobs, walk distance, playtime, craft items, fish
  - **Cobblemon Capture**: All elemental types, specific species, different Poké Balls
  - **Combat**: Defeat wild Pokémon, win PvP battles, defeat specific types
  - **Evolution & Breeding**: Evolve Pokémon, hatch eggs, use evolution stones
  - **Special Conditions**: Night captures, rain captures, no-damage captures
  - **Level Range**: Catch Pokémon at specific level ranges
  - **Biome**: Catch Pokémon in specific biomes

### Shop System
- **250+ items** across **35+ categories**:
  - Poké Balls (all types)
  - Potions & Vitamins
  - Mints (all natures)
  - Exp Candies
  - Apricorns
  - Berries
  - Evolution Items
  - Hold Items
  - TMs
  - Gems & Z-Crystals
  - Mega Stones
  - Training Items
  - Breeding Items
  - Fossils
  - Shards & Cosmic Items
  - Legendary Items
  - And more...

- **Decimal price support** (e.g., 0.50, 1.25, 2.75 CC)
- **Player marketplace** - players can sell items to other players

### AFK Rewards
- Earn CobbleCoins for being AFK in designated zones
- Configurable reward intervals and multipliers by rank

### Rank System
- Trainer, Trainer+, Elite, Legendary, Mythical
- Multipliers for AFK and mission rewards based on rank

## Commands

### Player Commands
- `/register <password>` - Register your account
- `/login <password>` - Login to your account
- `/logout` - Logout from your account
- `/account` - Account management
- `/ccoins` - View your balance
- `/ccoins redeem <code>` - Redeem CobbleCoins code

### Admin Commands
- `/auth` - Admin panel (view accounts, force login/logout, reload config)
- `/ccoins owner` - Owner panel for mission management

## Configuration

All configuration files are located in `.minecraft/config/`:

- `modeconomia.json` - Main configuration (missions, shop, AFK, ranks)
- `modeconomia-data.json` - Player data (balances, mission progress)

## Building

```bash
# Build all versions
./gradlew build

# Run development client
./gradlew runClient

# Run development server
./gradlew runServer
```

## Requirements

- Minecraft 1.21.11 (active version)
- Fabric Loader
- Cobblemon 1.7.3+
- Fabric API

## License

MIT License

## Credits

- Built with Fabric API
- Cobblemon integration
- Stonecutter for multi-version support
