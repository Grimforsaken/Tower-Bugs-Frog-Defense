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
'''            if (frog.boss) {\n                if (hasAdjacentWall(frog)) {\n                    damageShortcutWall(frog, dt);\n                    continue;\n                }\n            } else {\n                if (canAttackBugs(frog) && attackAdjacentBug(frog, dt)) continue;\n                if (frog.trait == FrogTrait.WALL_BREAKER) damageShortcutWall(frog, dt);\n            }\n            moveFrog(frog, dt);''',
'''            if (!frog.boss) {\n                if (canAttackBugs(frog) && attackAdjacentBug(frog, dt)) continue;\n                if (frog.trait == FrogTrait.WALL_BREAKER) damageShortcutWall(frog, dt);\n            }\n            moveFrog(frog, dt);''',
'bosses do not attack')

replace_once(
'''    private int advancedTrainingCost(BugType type) { return 12 + type.unlockCost / 2; }''',
'''    private int advancedTrainingCost(BugType type) { return 3 + type.ordinal(); }''',
'bug buck training starts at 3')

p.write_text(s)
print('Applied Tower Bugs v0.2.3 gameplay patch')
