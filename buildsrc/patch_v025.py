from pathlib import Path

p = Path('app/src/main/java/com/grimforsaken/towerbugs/GameView.java')
s = p.read_text()

def replace_once(old, new, label):
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 match, found {count}')
    s = s.replace(old, new, 1)

replace_once(
'''        spawnRemaining = 5 + level + wave * 2;\n        if (level == 1 && wave == WAVES_PER_LEVEL) spawnRemaining = 11;''',
'''        // Smooth wave-size growth: add one frog per wave instead of two.\n        spawnRemaining = 5 + level + wave;''',
'slower wave-size growth')

replace_once(
'''        int tier = Math.min(7, 1 + (level - 1) / 2 + (wave - 1) / 2);''',
'''        // Frog tiers now advance roughly once every two full levels instead of\n        // every two waves. Levels 1-2 are Tier 1, Levels 3-4 Tier 2, etc.\n        int campaignWave = Math.max(0, (level - 1) * WAVES_PER_LEVEL + (wave - 1));\n        int tier = Math.min(7, 1 + campaignWave / 10);''',
'slower tier progression')

replace_once(
'''        int armorTier = Math.min(3, Math.max(0, tier - 1));\n        if (trait == FrogTrait.ARMORED) armorTier = Math.min(3, armorTier + 1);\n        if (boss) armorTier = 3;''',
'''        // Armor also grows more slowly: Tier 2-3 = Armor 1, Tier 4-5 = Armor 2,\n        // Tier 6-7 = Armor 3. Bosses no longer jump straight to Armor 3.\n        int armorTier = tier <= 1 ? 0 : Math.min(3, 1 + (tier - 2) / 2);\n        if (trait == FrogTrait.ARMORED) armorTier = Math.min(3, armorTier + 1);\n        if (boss) armorTier = Math.min(armorTier, Math.max(0, (level - 1) / 2));''',
'slower armor progression')

replace_once(
'''        float maxHp = (34f + tier * 24f) * (1f + (level - 1) * 0.15f);\n        float speed = 0.92f + tier * 0.075f;\n        float defense = Math.max(0f, tier - 1) * 1.35f + armorTier * 0.9f;''',
'''        // Stats rise gradually so quantity, armor, HP and speed do not all spike together.\n        float maxHp = (40f + tier * 18f) * (1f + (level - 1) * 0.08f);\n        float speed = 0.88f + tier * 0.045f;\n        float defense = Math.max(0f, tier - 1) * 0.65f + armorTier * 0.55f;''',
'slower stat progression')

replace_once(
'''                spawnTimer = 0.60f;''',
'''                // Early levels space frogs out more so starter bugs can finish targets.\n                spawnTimer = level <= 2 ? 0.78f : level <= 4 ? 0.69f : 0.62f;''',
'slower early spawn pacing')

replace_once(
'''    private FrogTrait chooseTrait(int tier, boolean boss, int serial) {\n        if (boss) return FrogTrait.WALL_BREAKER;\n        if (level >= 5 && serial % 13 == 0) return FrogTrait.COMMANDER;\n        if (level >= 4 && serial % 11 == 0) return FrogTrait.REGENERATING;\n        if (level >= 4 && serial % 7 == 0) return FrogTrait.BUG_HUNTER;\n        if (level >= 3 && serial % 9 == 0) return FrogTrait.ARMORED;\n        if (level >= 3 && serial % 8 == 0) return FrogTrait.RESISTANT;\n        if (level >= 2 && serial % 6 == 0) return FrogTrait.FAST;\n        if (level >= 2 && serial % 5 == 0) return FrogTrait.SWARM;\n        if (tier >= 6 && serial % 4 == 0) return FrogTrait.WALL_BREAKER;\n        return FrogTrait.NORMAL;\n    }''',
'''    private FrogTrait chooseTrait(int tier, boolean boss, int serial) {\n        // Bosses are movement/HP challenges only and never receive an attack trait.\n        if (boss) return FrogTrait.NORMAL;\n        // Special traits are deliberately delayed so players have time to unlock bugs.\n        if (level >= 8 && serial % 13 == 0) return FrogTrait.COMMANDER;\n        if (level >= 6 && serial % 11 == 0) return FrogTrait.REGENERATING;\n        if (level >= 6 && serial % 7 == 0) return FrogTrait.BUG_HUNTER;\n        if (level >= 5 && serial % 9 == 0) return FrogTrait.ARMORED;\n        if (level >= 5 && serial % 8 == 0) return FrogTrait.RESISTANT;\n        if (level >= 4 && serial % 6 == 0) return FrogTrait.FAST;\n        if (level >= 4 && serial % 5 == 0) return FrogTrait.SWARM;\n        if (tier >= 6 && serial % 4 == 0) return FrogTrait.WALL_BREAKER;\n        return FrogTrait.NORMAL;\n    }''',
'delay special traits')

replace_once(
'''    private float pathPressureBoost(Frog frog) {\n        if (frog.path == null) return 1f;\n        float natural = Math.abs(tower.x - spawn.x) + Math.abs(tower.y - spawn.y) + 1f;\n        float ratio = frog.path.size() / natural;\n        if (ratio <= 1.45f) return 1f;\n        return 1f + Math.min(0.25f, (ratio - 1.45f) * 0.18f);\n    }''',
'''    private float pathPressureBoost(Frog frog) {\n        // Do not punish maze-building during the early progression.\n        if (level < 5 || frog.path == null) return 1f;\n        float natural = Math.abs(tower.x - spawn.x) + Math.abs(tower.y - spawn.y) + 1f;\n        float ratio = frog.path.size() / natural;\n        if (ratio <= 1.80f) return 1f;\n        return 1f + Math.min(0.10f, (ratio - 1.80f) * 0.07f);\n    }''',
'gentler path pressure')

p.write_text(s)
print('Applied Tower Bugs v0.2.5 slower campaign progression patch')
