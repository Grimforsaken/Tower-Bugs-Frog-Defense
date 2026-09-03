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
'''    private boolean bugShopOpen;\n    private boolean bugPanelOpen;''',
'''    private boolean bugShopOpen;\n    private boolean bugPanelOpen;\n    private boolean paused;''',
'paused field')

replace_once(
'''    private final RectF actionButton = new RectF();''',
'''    private final RectF actionButton = new RectF();\n    private final RectF pauseButton = new RectF();''',
'pause rect')

replace_once(
'''        if (phase == Phase.WAVE || phase == Phase.INTERMISSION) updateGame(dt);''',
'''        if (!paused && (phase == Phase.WAVE || phase == Phase.INTERMISSION)) updateGame(dt);''',
'pause simulation')

replace_once(
'''            if (bugPanelOpen) drawBugPanel(canvas);\n            if (phase == Phase.LEVEL_COMPLETE) drawEndOverlay(canvas, true);''',
'''            if (bugPanelOpen) drawBugPanel(canvas);\n            if (paused) drawPauseOverlay(canvas);\n            if (phase == Phase.LEVEL_COMPLETE) drawEndOverlay(canvas, true);''',
'pause overlay draw')

replace_once(
'''        bugPanelOpen = false;\n        phase = Phase.BUILD;''',
'''        bugPanelOpen = false;\n        paused = false;\n        phase = Phase.BUILD;''',
'pause reset')

replace_once(
'''            if (canAttackBugs(frog) && attackAdjacentBug(frog, dt)) continue;\n            if (frog.boss || frog.trait == FrogTrait.WALL_BREAKER) damageShortcutWall(frog, dt);\n            moveFrog(frog, dt);''',
'''            if (frog.boss) {\n                if (hasAdjacentWall(frog)) {\n                    damageShortcutWall(frog, dt);\n                    continue;\n                }\n            } else {\n                if (canAttackBugs(frog) && attackAdjacentBug(frog, dt)) continue;\n                if (frog.trait == FrogTrait.WALL_BREAKER) damageShortcutWall(frog, dt);\n            }\n            moveFrog(frog, dt);''',
'boss wall-only behavior')

replace_once(
'''        if (spawnRemaining == 0 && frogs.isEmpty()) {\n            if (wave >= WAVES_PER_LEVEL) completeLevel();''',
'''        if (spawnRemaining == 0 && frogs.isEmpty()) {\n            awardFirstClearWaveBugBuck();\n            if (wave >= WAVES_PER_LEVEL) completeLevel();''',
'wave first-clear reward call')

replace_once(
'''    private boolean canAttackBugs(Frog frog) {\n        return frog.tier >= 4 || frog.trait == FrogTrait.BUG_HUNTER || frog.boss;\n    }''',
'''    private boolean canAttackBugs(Frog frog) {\n        if (frog.boss) return false;\n        return frog.tier >= 4 || frog.trait == FrogTrait.BUG_HUNTER;\n    }\n\n    private boolean hasAdjacentWall(Frog frog) {\n        int fx = Math.round(frog.x);\n        int fy = Math.round(frog.y);\n        for (int i = 0; i < 4; i++) {\n            int nx = fx + DIR_X[i];\n            int ny = fy + DIR_Y[i];\n            if (inside(nx, ny) && wallHp[nx][ny] > 0f) return true;\n        }\n        return false;\n    }''',
'boss cannot attack bugs')

replace_once(
'''        Point bestWall = null;\n        int bestLength = currentLength;''',
'''        Point bestWall = null;\n        int bestLength = currentLength;\n        Point fallbackWall = null;\n        float fallbackHp = Float.MAX_VALUE;''',
'boss fallback wall fields')

replace_once(
'''            float oldHp = wallHp[nx][ny];\n            wallHp[nx][ny] = 0f;''',
'''            float oldHp = wallHp[nx][ny];\n            if (frog.boss && oldHp < fallbackHp) {\n                fallbackHp = oldHp;\n                fallbackWall = new Point(nx, ny);\n            }\n            wallHp[nx][ny] = 0f;''',
'boss fallback wall choice')

replace_once(
'''        if (bestWall != null) {\n            wallHp[bestWall.x][bestWall.y] -= frog.boss ? 24f : 14f;''',
'''        if (bestWall == null && frog.boss) bestWall = fallbackWall;\n\n        if (bestWall != null) {\n            wallHp[bestWall.x][bestWall.y] -= frog.boss ? 24f : 14f;''',
'boss uses any adjacent wall')

replace_once(
'''    private void completeLevel() {\n        int award = 6 + level * 2;''',
'''    private void awardFirstClearWaveBugBuck() {\n        if (wave <= 0) return;\n        String key = "highest_rewarded_wave_level_" + level;\n        int highestRewardedWave = prefs.getInt(key, 0);\n        if (wave > highestRewardedWave) {\n            bugBucks += 1;\n            prefs.edit()\n                    .putInt("bug_bucks", bugBucks)\n                    .putInt(key, wave)\n                    .apply();\n        }\n    }\n\n    private void completeLevel() {\n        int award = 6 + level * 2;''',
'first-clear reward method')

replace_once(
'''        paint.setTextAlign(Paint.Align.RIGHT);\n        paint.setColor(Color.rgb(244, 205, 88));\n        c.drawText("Bug Bucks: " + bugBucks, getWidth() - 16f, hudH * 0.58f, paint);''',
'''        pauseButton.set(getWidth() - 148f, 7f, getWidth() - 10f, hudH - 7f);\n        paint.setTextAlign(Paint.Align.RIGHT);\n        paint.setColor(Color.rgb(244, 205, 88));\n        c.drawText("Bug Bucks: " + bugBucks, pauseButton.left - 10f, hudH * 0.58f, paint);\n        drawButton(c, pauseButton, paused ? "RESUME" : "PAUSE",\n                phase == Phase.BUILD || phase == Phase.WAVE || phase == Phase.INTERMISSION, paused);''',
'pause HUD button')

replace_once(
'''    private void drawEndOverlay(Canvas c, boolean won) {''',
'''    private void drawPauseOverlay(Canvas c) {\n        paint.setColor(Color.argb(185, 5, 8, 6));\n        c.drawRect(0, Math.max(62f, getHeight() * 0.09f), getWidth(),\n                getHeight() - Math.max(82f, getHeight() * 0.12f), paint);\n        paint.setTextAlign(Paint.Align.CENTER);\n        paint.setColor(Color.rgb(244, 218, 119));\n        paint.setTextSize(Math.max(34f, getHeight() * 0.075f));\n        paint.setFakeBoldText(true);\n        c.drawText("PAUSED", getWidth() / 2f, getHeight() * 0.48f, paint);\n        paint.setFakeBoldText(false);\n        paint.setColor(Color.WHITE);\n        paint.setTextSize(Math.max(15f, getHeight() * 0.03f));\n        c.drawText("Tap RESUME to continue", getWidth() / 2f, getHeight() * 0.55f, paint);\n    }\n\n    private void drawEndOverlay(Canvas c, boolean won) {''',
'pause overlay method')

replace_once(
'''        if (phase == Phase.HOME) return handleHomeTouch(x, y);\n\n        if (phase == Phase.LEVEL_COMPLETE || phase == Phase.GAME_OVER) {''',
'''        if (phase == Phase.HOME) return handleHomeTouch(x, y);\n\n        if ((phase == Phase.BUILD || phase == Phase.WAVE || phase == Phase.INTERMISSION)\n                && pauseButton.contains(x, y)) {\n            paused = !paused;\n            lastFrameNanos = System.nanoTime();\n            return true;\n        }\n        if (paused) return true;\n\n        if (phase == Phase.LEVEL_COMPLETE || phase == Phase.GAME_OVER) {''',
'pause touch handling')

p.write_text(s)
print('Applied Tower Bugs v0.2.2 gameplay patch')
