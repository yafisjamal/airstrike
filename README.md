# SKYFALL – Tactical Airstrike System (Minecraft 1.20.1 Forge)

**SKYFALL** is a Call of Duty killstreak-inspired tactical UAV airstrike mod for **Minecraft 1.20.1 (Forge 47.2.0 / Java 17)**. It features custom 3D MQ-27 Strike UAV entities with spinning pusher propellers and blinking red targeting beacons, five distinct strike patterns, volumetric lingering battlefield smoke, 4-stage layered explosion audio, camera shake, dynamic sky illumination, a 3D world laser target marker, and a military tactical HUD.

---

## 1. How to Compile the `.jar` File (2 Easy Methods)

### Method A: 1-Click Cloud Build via GitHub Actions (No Java/Gradle Installation Needed)
1. Create a new repository on GitHub and upload all files from this project folder (including the `.github/workflows/build.yml` file).
2. Go to the **Actions** tab in your GitHub repository.
3. GitHub's servers will automatically run the **Build SKYFALL Forge 1.20.1 JAR** workflow using Java 17 and Gradle 8.4.
4. Once finished (~2 minutes), click the workflow run and download `skyfall-forge-1.20.1-jar` containing your ready-to-play `skyfall-1.0.0.jar`!

### Method B: Local Build with Forge MDK / Gradle
1. Ensure **Java 17 JDK** (e.g., Eclipse Temurin 17) is installed.
2. Open a terminal in this project root directory (or copy `src/`, `build.gradle`, `gradle.properties`, `settings.gradle`, and `defaultconfigs/` into your Forge 1.20.1 MDK folder).
3. Run the build command:
   - **Windows:** `gradlew.bat build` (or `gradle build`)
   - **Linux / macOS:** `./gradlew build` (or `gradle build`)
4. Grab the compiled, reobfuscated mod JAR from `build/libs/skyfall-1.0.0.jar` and place it into your `.minecraft/mods/` folder.

---

## 2. In-Game Controls & Tactical Designator Usage

Craft the **Tactical Airstrike Designator** (`skyfall:strike_designator`) using:
- **Top Row:** Redstone Torch | Tinted Glass | Redstone Torch
- **Middle Row:** Iron Ingot | Redstone Comparator | Iron Ingot
- **Bottom Row:** Iron Ingot | Redstone Torch | Iron Ingot
*(Or grab it from the **SKYFALL Tactical Systems** Creative Tab).*

### Controls while holding the Tactical Airstrike Designator:
| Action | Control | Description |
| :--- | :--- | :--- |
| **Call in Airstrike** | `Right-Click` | Locks onto the block you are looking at (up to 160m away), plays the target lock tone & warning siren, and deploys the UAV wave |
| **Cycle Strike Mode** | `Sneak + Right-Click` | Cycles between **Precision**, **Cluster**, **Carpet**, **Area Suppression**, and **EMP Strike** |
| **Cycle Strike Radius** | `Sneak + Left-Click` | Cycles target zone radius between `8m`, `14m`, `24m`, `36m`, and `48m` |

---

## 3. Airstrike Modes

| Mode | Default UAVs | Default Radius | Behavior & Tactical Effect |
| :--- | :--- | :--- | :--- |
| **Precision Strike** | 1 | 4.0m | Single high-speed UAV with 1.65x warhead payload for pinpoint bunker-busting destruction |
| **Cluster Strike** | 6 | 14.0m | Coordinated multi-UAV wave saturating the target perimeter simultaneously |
| **Carpet Strike** | 10 | 24.0m | Staggered dual-lane rolling bombardment along the UAV approach vector |
| **Area Suppression** | 14 | 28.0m | Continuous staggered UAV impacts suppressing a wide battlefield zone over time |
| **EMP Strike** | 3 | 18.0m | Non-destructive electromagnetic detonation with cyan plasma arcs, visual lightning, extinguish effects, and heavy debuffs (`Slowness IV`, `Weakness II`, `Glowing`, `Darkness`, plus massive bonus damage to Iron Golems) |

---

## 4. Commands (`/skyfall`)

Requires operator permission level 2:
- `/skyfall strike <x> <y> <z>` — Launch an airstrike at exact coordinates using current settings
- `/skyfall strike <x> <y> <z> <radius>` — Launch an airstrike at exact coordinates with a custom radius
- `/skyfall mode <precision|cluster|carpet|suppression|emp>` — Switch active airstrike mode
- `/skyfall drones <amount>` — Set number of incoming UAVs (`1` to `maxDroneCount`)
- `/skyfall radius <amount>` — Set target radius in blocks (`1.0` to `maxStrikeRadius`)
- `/skyfall altitude <amount>` — Set UAV spawn altitude above target (`30.0` to `220.0`)
- `/skyfall power <amount>` — Set warhead explosion intensity (`1.0` to `32.0`)
- `/skyfall cancel` — Immediately abort all active airstrikes and despawn airborne UAVs
- `/skyfall reload` — Reload player strike defaults from `config/skyfall-common.toml`
- `/skyfall help` — Display tactical command reference in chat
