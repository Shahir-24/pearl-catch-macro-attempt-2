# Pearl Catch (Fabric, MC 1.21.11)

Predicts your ender pearl's real trajectory every tick and auto-throws a wind
charge to intercept it mid-air — the vanilla "wind-charge pearl catch" combo,
automated and aimed by physics instead of by hand.

## How it works

1. Press **G** to arm.
2. Throw an ender pearl (works while falling, moving, whatever — the solver
   reads your and the pearl's *real* current state each tick, not a
   one-shot prediction from the moment you threw it).
3. Every client tick, the mod re-reads the live pearl entity's actual
   position and velocity, simulates it forward using vanilla drag/gravity,
   and solves for the earliest tick a wind charge fired *right now* could
   reach it.
4. The instant a valid intercept exists and you have a wind charge in your
   hotbar, it snaps your aim, switches slots, throws, and switches back.

## Building — three ways to get the actual .jar

The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) is included
in this project — it's the real wrapper pulled from Gradle's own repo, not a
stub — so no separate Gradle install is required, just a JDK.

**Option A — locally, if you have JDK 21:**
```
cd pearlcatch
./gradlew build
```
Output: `build/libs/pearlcatch-1.0.0.jar`. That's the file for
`.minecraft/mods`.

**Option B — no local Java needed, via GitHub Actions:** push this folder
to a new GitHub repo, then check the "Actions" tab — `.github/workflows/build.yml`
is already set up to build it on GitHub's own servers (which have full
internet access, unlike the sandbox I built this in) and hand you back a
downloadable `pearlcatch-jar` artifact.

**Option C — IntelliJ:** open the folder, let it sync via Loom, run the
`build` Gradle task from the sidebar.

Either way, alongside the mod you'll need Fabric Loader 0.18.1+ and Fabric
API 0.141.5+1.21.11 installed in `.minecraft/mods` too (get both from
https://fabricmc.net) — this mod depends on Fabric API, it doesn't bundle it.

## I could not compile-test this myself

Important, and I don't want to overstate what I actually verified: my
sandbox's network is allowlisted to a small set of domains for security
reasons, and `maven.fabricmc.net` isn't on it — I confirmed this by actually
trying to reach it and getting an explicit block, not by assuming. That's
where Minecraft, Yarn mappings, Fabric Loader, and Fabric API all get
downloaded from during a build, so I have never compiled this against real
1.21.11 bytecode. Everything here is checked against Fabric's public docs,
GitHub source, and the published Yarn javadocs for 1.21.11/1.21.x — but
"checked against docs" isn't the same guarantee as "compiled clean." The
first `./gradlew build` you run (locally or via Option B above) is the real
test; let it catch anything I missed.

One version-compatibility note I couldn't fully pin down: `gradle-wrapper.properties`
points at Gradle 8.10, which should satisfy Loom 1.14's requirements, but if
the build complains about a Gradle/Loom version mismatch, run
`./gradlew wrapper --gradle-version <newer version>` to bump it — that's a
one-line fix, not a sign anything else is wrong.

## Two constants YOU need to calibrate — don't trust these blindly

- **`PearlPhysics.GRAVITY` / `DRAG`** (0.03, 0.99) — long-documented vanilla
  values, high confidence, but not verified against 1.21.11 bytecode
  specifically.
- **`WindChargePhysics.SPEED`** (1.5) — this is a guess based on the common
  base projectile power, not a confirmed number. If your intercepts
  consistently land short or long, this is almost certainly why. Calibration
  steps are in the comment right above the constant.

If intercepts are close but not exact, tune these two before assuming the
intercept math itself is wrong — the math is solid, the constants feeding it
are the uncertain part.

## Known limitations (not bugs, just things this doesn't handle)

- **No block collision checking.** The pearl-path simulation assumes open
  air. If your pearl would hit a wall/ceiling before the computed intercept
  tick, the aim will be wrong. Test in open areas first.
- **Camera snap.** Auto-aiming necessarily moves your camera for the throw
  tick. There's no way to auto-throw accurately without this.
- **Multiplayer server fairness.** This automates a real vanilla mechanic
  client-side — it's not exploiting a bug — but most PvP servers will
  consider full auto-aim-and-throw an assist/cheat and it can get you
  banned. Built and intended for singleplayer / your own server, per what
  you told me.
- **Cooldowns.** Wind charges have a 10-tick throw cooldown; if you just
  threw one, the auto-throw will silently do nothing until it's off
  cooldown (the interactItem call just won't fire). Not currently
  surfaced to you as a message — worth adding if it bugs you in testing.

## Controls — fully rebindable, this is standard Fabric behavior

Options > Controls > Pearl Catch > "Arm Pearl Catch" — bind it to whatever
you want. This isn't something bolted on; registering a `KeyBinding` via
`KeyBindingHelper.registerKeyBinding()` (which this mod does) is exactly
what makes Fabric mod keybinds show up in vanilla's own Controls menu,
get conflict-checked against every other bind in the game, and stay
rebindable and reset-able through the normal UI — no separate settings
screen, no config file to hand-edit. Default is **G** only because
something had to be the default.
