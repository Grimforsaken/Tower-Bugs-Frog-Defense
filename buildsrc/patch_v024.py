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
'''        freeBlocks = 12 + freeBlockUpgrade * 2;''',
'''        freeBlocks = 18 + freeBlockUpgrade * 2;''',
'starting blocks')

replace_once(
'''        spawnRemaining = 5 + level + wave * 2;''',
'''        spawnRemaining = 5 + level + wave * 2;
        if (level == 1 && wave == WAVES_PER_LEVEL) spawnRemaining = 11;''',
'level 1 wave 5 frog count')

replace_once(
'''        if (trait == FrogTrait.COMMANDER) { maxHp *= 1.35f; reward += 3; }
        if (boss) { maxHp *= 4.7f; speed *= 0.78f; defense += 3f; reward = 24 + tier * 6; }''',
'''        if (trait == FrogTrait.COMMANDER) { maxHp *= 1.35f; reward += 3; }

        // Introductory armor must be beatable with Fly-only progression and the
        // coins available in Level 1. It keeps the armor art but is not a stat wall.
        if (level == 1 && armorTier == 1 && !boss) {
            maxHp *= 0.72f;
            speed *= 0.90f;
            defense = Math.min(defense, 0.75f);
        }

        // Wave 5 must be a strategy check, not require a Bug Buck unlock.
        if (level == 1 && wave == WAVES_PER_LEVEL && !boss) {
            maxHp *= 0.66f;
            speed *= 0.85f;
            defense *= 0.45f;
        }
        if (boss) {
            float bossHpMultiplier = Math.min(3.15f, 1.85f + Math.max(0, level - 1) * 0.18f);
            maxHp *= bossHpMultiplier;
            speed *= level <= 1 ? 0.62f : 0.68f;
            float bossDefenseCap = 3.0f + Math.max(0, level - 1) * 0.65f;
            defense = Math.min(defense + 1.0f, bossDefenseCap);
            reward = 24 + tier * 6;
        }''',
'early armor wave 5 and boss balance')

replace_once(
'''        float routeBoost = pathPressureBoost(frog);''',
'''        float routeBoost = (frog.boss || (level == 1 && wave == WAVES_PER_LEVEL)) ? 1f : pathPressureBoost(frog);''',
'wave 5 ignores path pressure speed boost')

p.write_text(s)
print('Applied Tower Bugs v0.2.4 early armor, starting blocks, and Wave 5 balance patch')
