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
'''        if (boss) { maxHp *= 4.7f; speed *= 0.78f; defense += 3f; reward = 24 + tier * 6; }''',
'''        if (boss) {
            float bossHpMultiplier = Math.min(3.15f, 1.85f + Math.max(0, level - 1) * 0.18f);
            maxHp *= bossHpMultiplier;
            speed *= level <= 1 ? 0.62f : 0.68f;
            float bossDefenseCap = 3.0f + Math.max(0, level - 1) * 0.65f;
            defense = Math.min(defense + 1.0f, bossDefenseCap);
            reward = 24 + tier * 6;
        }''',
'boss balance')

replace_once(
'''        float routeBoost = pathPressureBoost(frog);''',
'''        float routeBoost = frog.boss ? 1f : pathPressureBoost(frog);''',
'boss ignores path pressure speed boost')

p.write_text(s)
print('Applied Tower Bugs v0.2.4 Wave 5 balance patch')
