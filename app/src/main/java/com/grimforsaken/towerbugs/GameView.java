package com.grimforsaken.towerbugs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameView extends View {
    private static final int COLS = 16;
    private static final int ROWS = 10;
    private static final int WAVES_PER_LEVEL = 5;
    private static final int WALL_MAX_HP = 100;

    private enum Phase { HOME, BUILD, WAVE, INTERMISSION, LEVEL_COMPLETE, GAME_OVER }
    private enum Tool { BLOCK, BUG, UPGRADE }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences prefs;
    private final int[][] wallHp = new int[COLS][ROWS];
    private final Map<Point, Bug> bugs = new HashMap<>();
    private final List<Frog> frogs = new ArrayList<>();

    private final Point spawn = new Point(0, ROWS / 2);
    private final Point tower = new Point(COLS - 1, ROWS / 2);

    private Phase phase = Phase.HOME;
    private Tool selectedTool = Tool.BLOCK;

    private int level;
    private int bugBucks;
    private int startCoinUpgrade;
    private int freeBlockUpgrade;
    private int bugPowerUpgrade;

    private int coins;
    private int freeBlocks;
    private int purchasedBlocks;
    private int wave;
    private int towerHp;
    private int spawnRemaining;
    private float spawnTimer;
    private float intermission;
    private long lastFrameNanos;

    private float cell;
    private float boardLeft;
    private float boardTop;

    private RectF blockButton = new RectF();
    private RectF bugButton = new RectF();
    private RectF upgradeButton = new RectF();
    private RectF actionButton = new RectF();
    private RectF homePlayButton = new RectF();
    private RectF homeCoinUpgrade = new RectF();
    private RectF homeBlockUpgrade = new RectF();
    private RectF homeBugUpgrade = new RectF();
    private RectF endButton = new RectF();

    public GameView(Context context) {
        super(context);
        setKeepScreenOn(true);
        prefs = context.getSharedPreferences("tower_bugs_progress", Context.MODE_PRIVATE);
        level = Math.max(1, prefs.getInt("level", 1));
        bugBucks = prefs.getInt("bug_bucks", 0);
        startCoinUpgrade = prefs.getInt("start_coin_upgrade", 0);
        freeBlockUpgrade = prefs.getInt("free_block_upgrade", 0);
        bugPowerUpgrade = prefs.getInt("bug_power_upgrade", 0);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setColor(Color.argb(110, 255, 255, 255));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0f : Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        if (phase == Phase.WAVE || phase == Phase.INTERMISSION) {
            updateGame(dt);
        }

        canvas.drawColor(Color.rgb(18, 25, 19));
        if (phase == Phase.HOME) {
            drawHome(canvas);
        } else {
            calculateBoardGeometry();
            drawHud(canvas);
            drawBoard(canvas);
            drawBottomControls(canvas);
            if (phase == Phase.LEVEL_COMPLETE) drawEndOverlay(canvas, true);
            if (phase == Phase.GAME_OVER) drawEndOverlay(canvas, false);
        }

        postInvalidateOnAnimation();
    }

    private void calculateBoardGeometry() {
        float topHud = 70f;
        float bottomHud = 105f;
        float usableH = Math.max(1f, getHeight() - topHud - bottomHud);
        cell = Math.min(getWidth() / (float) COLS, usableH / ROWS);
        boardLeft = (getWidth() - cell * COLS) / 2f;
        boardTop = topHud + (usableH - cell * ROWS) / 2f;
    }

    private void drawHome(Canvas c) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(235, 226, 188));
        paint.setTextSize(44f);
        paint.setFakeBoldText(true);
        c.drawText("TOWER BUGS: FROG DEFENSE", getWidth() / 2f, 70f, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(23f);
        c.drawText("Level " + level + "   •   Bug Bucks: " + bugBucks, getWidth() / 2f, 112f, paint);

        float w = Math.min(360f, getWidth() * 0.32f);
        float h = 74f;
        float gap = 18f;
        float left = getWidth() / 2f - w / 2f;
        float y = 150f;

        homeCoinUpgrade.set(left, y, left + w, y + h);
        y += h + gap;
        homeBlockUpgrade.set(left, y, left + w, y + h);
        y += h + gap;
        homeBugUpgrade.set(left, y, left + w, y + h);
        y += h + gap + 8f;
        homePlayButton.set(left, y, left + w, y + h);

        drawHomeUpgrade(c, homeCoinUpgrade, "STARTING COINS +25", startCoinUpgrade, upgradeCost(startCoinUpgrade));
        drawHomeUpgrade(c, homeBlockUpgrade, "FREE BLOCKS +2", freeBlockUpgrade, upgradeCost(freeBlockUpgrade));
        drawHomeUpgrade(c, homeBugUpgrade, "BUG DAMAGE +10%", bugPowerUpgrade, upgradeCost(bugPowerUpgrade));
        drawButton(c, homePlayButton, "PLAY LEVEL " + level, true, false);
    }

    private void drawHomeUpgrade(Canvas c, RectF r, String title, int rank, int cost) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(52, 66, 45));
        c.drawRoundRect(r, 12f, 12f, paint);
        stroke.setColor(Color.rgb(179, 149, 69));
        c.drawRoundRect(r, 12f, 12f, stroke);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        c.drawText(title + "  Lv." + rank, r.left + 18f, r.top + 30f, paint);
        paint.setTextSize(15f);
        paint.setColor(Color.rgb(244, 205, 88));
        c.drawText("Cost: " + cost + " Bug Bucks", r.left + 18f, r.top + 55f, paint);
    }

    private int upgradeCost(int currentRank) {
        return 5 + currentRank * 5;
    }

    private void initializeLevel() {
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) wallHp[x][y] = 0;
        }
        bugs.clear();
        frogs.clear();
        coins = 100 + startCoinUpgrade * 25;
        freeBlocks = 10 + freeBlockUpgrade * 2;
        purchasedBlocks = 0;
        wave = 0;
        towerHp = 20;
        spawnRemaining = 0;
        intermission = 0f;
        selectedTool = Tool.BLOCK;
        phase = Phase.BUILD;
    }

    private void updateGame(float dt) {
        if (phase == Phase.INTERMISSION) {
            intermission -= dt;
            if (intermission <= 0f) startNextWave();
            return;
        }

        if (spawnRemaining > 0) {
            spawnTimer -= dt;
            if (spawnTimer <= 0f) {
                spawnOneFrog();
                spawnRemaining--;
                spawnTimer = 0.62f;
            }
        }

        for (Bug bug : bugs.values()) {
            bug.cooldown -= dt;
            if (bug.cooldown <= 0f) {
                Frog target = chooseTarget(bug);
                if (target != null) {
                    float permanentMultiplier = 1f + bugPowerUpgrade * 0.10f;
                    float damage = bug.damage * permanentMultiplier;
                    target.hp -= Math.max(1f, damage - target.defense);
                    bug.cooldown = Math.max(0.18f, 0.78f - bug.level * 0.05f);
                }
            }
        }

        Iterator<Frog> iterator = frogs.iterator();
        while (iterator.hasNext()) {
            Frog frog = iterator.next();
            if (frog.hp <= 0f) {
                coins += frog.reward;
                iterator.remove();
                continue;
            }

            if (frog.tier >= 4 && attackAdjacentBug(frog, dt)) continue;
            if (frog.boss) damageShortcutWall(frog, dt);
            moveFrog(frog, dt);

            if (frog.reachedTower) {
                towerHp -= frog.boss ? 5 : Math.max(1, frog.tier / 2);
                iterator.remove();
                if (towerHp <= 0) {
                    phase = Phase.GAME_OVER;
                    return;
                }
            }
        }

        if (spawnRemaining == 0 && frogs.isEmpty()) {
            if (wave >= WAVES_PER_LEVEL) {
                completeLevel();
            } else {
                phase = Phase.INTERMISSION;
                intermission = 10f;
            }
        }
    }

    private Frog chooseTarget(Bug bug) {
        Frog best = null;
        float bestScore = Float.MAX_VALUE;
        for (Frog frog : frogs) {
            float dx = frog.x - bug.x;
            float dy = frog.y - bug.y;
            float d2 = dx * dx + dy * dy;
            if (d2 <= bug.range * bug.range) {
                float score = distanceToTower(frog);
                if (score < bestScore) {
                    bestScore = score;
                    best = frog;
                }
            }
        }
        return best;
    }

    private float distanceToTower(Frog frog) {
        float dx = tower.x - frog.x;
        float dy = tower.y - frog.y;
        return dx * dx + dy * dy;
    }

    private boolean attackAdjacentBug(Frog frog, float dt) {
        int fx = Math.round(frog.x);
        int fy = Math.round(frog.y);
        Iterator<Map.Entry<Point, Bug>> it = bugs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Point, Bug> e = it.next();
            Point p = e.getKey();
            if (Math.abs(p.x - fx) + Math.abs(p.y - fy) == 1) {
                Bug bug = e.getValue();
                bug.hp -= (8f + frog.tier * 2f) * dt;
                if (bug.hp <= 0f) it.remove();
                return true;
            }
        }
        return false;
    }

    private void damageShortcutWall(Frog frog, float dt) {
        frog.wallAttackCooldown -= dt;
        if (frog.wallAttackCooldown > 0f) return;
        frog.wallAttackCooldown = 0.7f;

        int fx = Math.round(frog.x);
        int fy = Math.round(frog.y);
        int currentLength = frog.path == null ? 999 : Math.max(0, frog.path.size() - frog.pathIndex);
        Point bestWall = null;
        int bestLength = currentLength;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        for (int[] d : dirs) {
            int nx = fx + d[0];
            int ny = fy + d[1];
            if (!inside(nx, ny) || wallHp[nx][ny] <= 0) continue;
            int oldHp = wallHp[nx][ny];
            wallHp[nx][ny] = 0;
            List<Point> candidate = findPath(new Point(fx, fy));
            wallHp[nx][ny] = oldHp;
            if (candidate != null && candidate.size() + 1 < bestLength) {
                bestLength = candidate.size();
                bestWall = new Point(nx, ny);
            }
        }

        if (bestWall != null) {
            wallHp[bestWall.x][bestWall.y] -= 24;
            if (wallHp[bestWall.x][bestWall.y] <= 0) {
                wallHp[bestWall.x][bestWall.y] = 0;
                bugs.remove(bestWall);
                recalculateAllFrogPaths();
            }
        }
    }

    private void moveFrog(Frog frog, float dt) {
        if (frog.path == null || frog.path.isEmpty()) {
            frog.path = findPath(new Point(Math.round(frog.x), Math.round(frog.y)));
            frog.pathIndex = 1;
            if (frog.path == null) return;
        }
        if (frog.pathIndex >= frog.path.size()) {
            frog.reachedTower = true;
            return;
        }

        Point targetCell = frog.path.get(frog.pathIndex);
        float dx = targetCell.x - frog.x;
        float dy = targetCell.y - frog.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float step = frog.speed * dt;
        if (dist <= step || dist < 0.001f) {
            frog.x = targetCell.x;
            frog.y = targetCell.y;
            frog.pathIndex++;
        } else {
            frog.x += dx / dist * step;
            frog.y += dy / dist * step;
        }
    }

    private void startNextWave() {
        wave++;
        phase = Phase.WAVE;
        spawnRemaining = 5 + level + wave * 2;
        spawnTimer = 0f;
    }

    private void spawnOneFrog() {
        int tier = Math.min(6, 1 + (level - 1) / 2 + (wave - 1) / 2);
        boolean boss = wave == WAVES_PER_LEVEL && spawnRemaining == 1;
        float maxHp = (38f + tier * 24f) * (1f + (level - 1) * 0.16f);
        if (boss) maxHp *= 4.5f;
        float speed = boss ? 0.72f : 1.05f + tier * 0.08f;
        float defense = Math.max(0f, tier - 1) * 1.5f;
        int reward = boss ? 20 + tier * 5 : Math.max(1, tier * 2);
        Frog frog = new Frog(spawn.x, spawn.y, tier, maxHp, speed, defense, reward, boss);
        frog.path = findPath(spawn);
        frog.pathIndex = 1;
        frogs.add(frog);
    }

    private void completeLevel() {
        int award = 5 + level * 2;
        bugBucks += award;
        prefs.edit().putInt("bug_bucks", bugBucks).apply();
        phase = Phase.LEVEL_COMPLETE;
    }

    private void drawHud(Canvas c) {
        paint.setColor(Color.rgb(12, 16, 13));
        c.drawRect(0, 0, getWidth(), 66f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        paint.setTextAlign(Paint.Align.LEFT);
        c.drawText("Level " + level + "   Wave " + wave + "/" + WAVES_PER_LEVEL, 18f, 27f, paint);
        c.drawText("Coins: " + coins + "   Free Blocks: " + freeBlocks + "   Tower HP: " + Math.max(0, towerHp), 18f, 53f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(Color.rgb(244, 205, 88));
        c.drawText("Bug Bucks: " + bugBucks, getWidth() - 18f, 38f, paint);
    }

    private void drawBoard(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(55, 63, 49));
        c.drawRect(boardLeft, boardTop, boardLeft + COLS * cell, boardTop + ROWS * cell, paint);

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                float l = boardLeft + x * cell;
                float t = boardTop + y * cell;
                RectF r = new RectF(l, t, l + cell, t + cell);
                if (wallHp[x][y] > 0) drawWall(c, r, wallHp[x][y]);
                c.drawRect(r, stroke);
            }
        }

        drawSpawn(c);
        drawTower(c);

        for (Map.Entry<Point, Bug> e : bugs.entrySet()) drawBug(c, e.getKey(), e.getValue());
        for (Frog frog : frogs) drawFrog(c, frog);
    }

    private void drawWall(Canvas c, RectF r, int hp) {
        paint.setColor(Color.rgb(91, 91, 88));
        RectF inner = new RectF(r.left + 2, r.top + 2, r.right - 2, r.bottom - 2);
        c.drawRect(inner, paint);
        paint.setColor(Color.rgb(132, 128, 118));
        paint.setStrokeWidth(2f);
        if (hp < 70) {
            c.drawLine(r.centerX(), r.top + 5, r.centerX() - cell * 0.18f, r.centerY(), paint);
            c.drawLine(r.centerX() - cell * 0.18f, r.centerY(), r.centerX() + cell * 0.12f, r.bottom - 5, paint);
        }
        if (hp < 35) {
            c.drawLine(r.left + 6, r.centerY(), r.centerX(), r.centerY() - cell * 0.12f, paint);
            c.drawLine(r.centerX(), r.centerY() - cell * 0.12f, r.right - 6, r.top + 9, paint);
        }
    }

    private void drawSpawn(Canvas c) {
        RectF r = cellRect(spawn.x, spawn.y);
        paint.setColor(Color.rgb(71, 109, 69));
        c.drawRect(r, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(12f, cell * 0.28f));
        c.drawText("SPAWN", r.centerX(), r.centerY() + 5f, paint);
    }

    private void drawTower(Canvas c) {
        RectF r = cellRect(tower.x, tower.y);
        paint.setColor(Color.rgb(154, 123, 55));
        c.drawRect(r, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(12f, cell * 0.28f));
        c.drawText("TOWER", r.centerX(), r.centerY() + 5f, paint);
    }

    private void drawBug(Canvas c, Point p, Bug bug) {
        float cx = boardLeft + (p.x + 0.5f) * cell;
        float cy = boardTop + (p.y + 0.5f) * cell;
        paint.setColor(Color.rgb(238, 181, 56));
        c.drawCircle(cx, cy, cell * 0.27f, paint);
        paint.setColor(Color.rgb(34, 30, 18));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(11f, cell * 0.25f));
        c.drawText("B" + bug.level, cx, cy + 5f, paint);
        drawHealthBar(c, cx - cell * 0.34f, cy - cell * 0.42f, cell * 0.68f, bug.hp / bug.maxHp);
    }

    private void drawFrog(Canvas c, Frog frog) {
        float cx = boardLeft + (frog.x + 0.5f) * cell;
        float cy = boardTop + (frog.y + 0.5f) * cell;
        paint.setColor(frog.boss ? Color.rgb(111, 45, 119) : Color.rgb(73, 183, 77));
        c.drawCircle(cx, cy, frog.boss ? cell * 0.35f : cell * 0.28f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(10f, cell * 0.22f));
        c.drawText(frog.boss ? "BOSS" : "F" + frog.tier, cx, cy + 4f, paint);
        drawHealthBar(c, cx - cell * 0.36f, cy - cell * 0.44f, cell * 0.72f, frog.hp / frog.maxHp);
    }

    private void drawHealthBar(Canvas c, float x, float y, float width, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        paint.setColor(Color.rgb(35, 35, 35));
        c.drawRect(x, y, x + width, y + 6f, paint);
        paint.setColor(healthColor(ratio));
        c.drawRect(x + 1f, y + 1f, x + 1f + (width - 2f) * ratio, y + 5f, paint);
    }

    private int healthColor(float ratio) {
        if (ratio >= 0.5f) {
            float t = (1f - ratio) * 2f;
            return Color.rgb((int)(255 * t), 220, 40);
        }
        float t = ratio * 2f;
        return Color.rgb(255, (int)(220 * t), 40);
    }

    private void drawBottomControls(Canvas c) {
        float y = getHeight() - 88f;
        float gap = 10f;
        float side = 12f;
        float w = (getWidth() - side * 2f - gap * 3f) / 4f;
        blockButton.set(side, y, side + w, y + 66f);
        bugButton.set(side + (w + gap), y, side + (w + gap) + w, y + 66f);
        upgradeButton.set(side + 2f * (w + gap), y, side + 2f * (w + gap) + w, y + 66f);
        actionButton.set(side + 3f * (w + gap), y, side + 3f * (w + gap) + w, y + 66f);

        int blockCost = blockCost();
        drawButton(c, blockButton, freeBlocks > 0 ? "BLOCK  FREE (" + freeBlocks + ")" : "BLOCK  " + blockCost + "¢", true, selectedTool == Tool.BLOCK);
        drawButton(c, bugButton, "BUG  40¢", true, selectedTool == Tool.BUG);
        drawButton(c, upgradeButton, "UPGRADE BUG", true, selectedTool == Tool.UPGRADE);

        String action;
        boolean enabled = true;
        if (phase == Phase.BUILD) action = "START WAVE 1";
        else if (phase == Phase.INTERMISSION) action = "SKIP  " + Math.max(0, (int)Math.ceil(intermission)) + "s";
        else if (phase == Phase.WAVE) { action = "WAVE " + wave + " ACTIVE"; enabled = false; }
        else { action = ""; enabled = false; }
        drawButton(c, actionButton, action, enabled, false);
    }

    private void drawButton(Canvas c, RectF r, String text, boolean enabled, boolean selected) {
        paint.setColor(!enabled ? Color.rgb(55, 55, 55) : selected ? Color.rgb(121, 102, 44) : Color.rgb(48, 70, 48));
        c.drawRoundRect(r, 10f, 10f, paint);
        stroke.setColor(selected ? Color.rgb(255, 220, 103) : Color.rgb(150, 158, 143));
        c.drawRoundRect(r, 10f, 10f, stroke);
        paint.setColor(enabled ? Color.WHITE : Color.LTGRAY);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(16f);
        c.drawText(text, r.centerX(), r.centerY() + 6f, paint);
    }

    private void drawEndOverlay(Canvas c, boolean won) {
        paint.setColor(Color.argb(220, 10, 12, 10));
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(won ? Color.rgb(247, 210, 96) : Color.rgb(238, 106, 93));
        paint.setTextSize(42f);
        paint.setFakeBoldText(true);
        c.drawText(won ? "LEVEL COMPLETE" : "THE TOWER FELL", getWidth() / 2f, getHeight() / 2f - 55f, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(20f);
        paint.setColor(Color.WHITE);
        if (won) c.drawText("Bug Bucks awarded: " + (5 + level * 2), getWidth() / 2f, getHeight() / 2f - 18f, paint);
        else c.drawText("Rebuild the maze and try again.", getWidth() / 2f, getHeight() / 2f - 18f, paint);
        endButton.set(getWidth() / 2f - 160f, getHeight() / 2f + 18f, getWidth() / 2f + 160f, getHeight() / 2f + 82f);
        drawButton(c, endButton, won ? "RETURN TO UPGRADES" : "RETRY LEVEL", true, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        float x = event.getX();
        float y = event.getY();

        if (phase == Phase.HOME) {
            if (homePlayButton.contains(x, y)) {
                initializeLevel();
                return true;
            }
            if (homeCoinUpgrade.contains(x, y)) buyPermanentUpgrade(0);
            else if (homeBlockUpgrade.contains(x, y)) buyPermanentUpgrade(1);
            else if (homeBugUpgrade.contains(x, y)) buyPermanentUpgrade(2);
            return true;
        }

        if (phase == Phase.LEVEL_COMPLETE || phase == Phase.GAME_OVER) {
            if (endButton.contains(x, y)) {
                if (phase == Phase.LEVEL_COMPLETE) {
                    level++;
                    prefs.edit().putInt("level", level).apply();
                    phase = Phase.HOME;
                } else {
                    initializeLevel();
                }
            }
            return true;
        }

        if (blockButton.contains(x, y)) { selectedTool = Tool.BLOCK; return true; }
        if (bugButton.contains(x, y)) { selectedTool = Tool.BUG; return true; }
        if (upgradeButton.contains(x, y)) { selectedTool = Tool.UPGRADE; return true; }
        if (actionButton.contains(x, y)) {
            if (phase == Phase.BUILD) startNextWave();
            else if (phase == Phase.INTERMISSION) startNextWave();
            return true;
        }

        int gx = (int)((x - boardLeft) / cell);
        int gy = (int)((y - boardTop) / cell);
        if (!inside(gx, gy)) return true;

        if (selectedTool == Tool.BLOCK) tryPlaceBlock(gx, gy);
        else if (selectedTool == Tool.BUG) tryPlaceBug(gx, gy);
        else tryUpgradeBug(gx, gy);
        return true;
    }

    private void buyPermanentUpgrade(int which) {
        int rank = which == 0 ? startCoinUpgrade : which == 1 ? freeBlockUpgrade : bugPowerUpgrade;
        int cost = upgradeCost(rank);
        if (bugBucks < cost) return;
        bugBucks -= cost;
        if (which == 0) startCoinUpgrade++;
        if (which == 1) freeBlockUpgrade++;
        if (which == 2) bugPowerUpgrade++;
        prefs.edit()
                .putInt("bug_bucks", bugBucks)
                .putInt("start_coin_upgrade", startCoinUpgrade)
                .putInt("free_block_upgrade", freeBlockUpgrade)
                .putInt("bug_power_upgrade", bugPowerUpgrade)
                .apply();
    }

    private int blockCost() {
        return 15 + purchasedBlocks * 3;
    }

    private void tryPlaceBlock(int x, int y) {
        if ((x == spawn.x && y == spawn.y) || (x == tower.x && y == tower.y)) return;
        if (wallHp[x][y] > 0 || bugs.containsKey(new Point(x, y)) || frogOccupies(x, y)) return;
        int cost = freeBlocks > 0 ? 0 : blockCost();
        if (coins < cost) return;

        wallHp[x][y] = WALL_MAX_HP;
        if (!allRoutesRemainOpen()) {
            wallHp[x][y] = 0;
            return;
        }

        if (freeBlocks > 0) freeBlocks--;
        else {
            coins -= cost;
            purchasedBlocks++;
        }
        recalculateAllFrogPaths();
    }

    private boolean allRoutesRemainOpen() {
        if (findPath(spawn) == null) return false;
        for (Frog frog : frogs) {
            Point p = new Point(Math.round(frog.x), Math.round(frog.y));
            if (findPath(p) == null) return false;
        }
        return true;
    }

    private boolean frogOccupies(int x, int y) {
        for (Frog frog : frogs) {
            if (Math.round(frog.x) == x && Math.round(frog.y) == y) return true;
        }
        return false;
    }

    private void tryPlaceBug(int x, int y) {
        Point p = new Point(x, y);
        if (wallHp[x][y] <= 0 || bugs.containsKey(p) || coins < 40) return;
        coins -= 40;
        bugs.put(p, new Bug(x, y));
    }

    private void tryUpgradeBug(int x, int y) {
        Bug bug = bugs.get(new Point(x, y));
        if (bug == null || bug.level >= 6) return;
        int cost = 25 + bug.level * 15;
        if (coins < cost) return;
        coins -= cost;
        bug.level++;
        bug.damage += 7f;
        bug.range += 0.12f;
        bug.maxHp += 20f;
        bug.hp = bug.maxHp;
    }

    private void recalculateAllFrogPaths() {
        for (Frog frog : frogs) {
            Point start = new Point(Math.round(frog.x), Math.round(frog.y));
            frog.path = findPath(start);
            frog.pathIndex = 1;
        }
    }

    private List<Point> findPath(Point start) {
        if (!inside(start.x, start.y) || wallHp[start.x][start.y] > 0) return null;
        boolean[][] seen = new boolean[COLS][ROWS];
        Point[][] previous = new Point[COLS][ROWS];
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(start));
        seen[start.x][start.y] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();
            if (p.equals(tower)) {
                ArrayList<Point> path = new ArrayList<>();
                Point cur = p;
                while (cur != null) {
                    path.add(0, cur);
                    cur = previous[cur.x][cur.y];
                }
                return path;
            }
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (!inside(nx, ny) || seen[nx][ny] || wallHp[nx][ny] > 0) continue;
                seen[nx][ny] = true;
                previous[nx][ny] = p;
                queue.addLast(new Point(nx, ny));
            }
        }
        return null;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && x < COLS && y >= 0 && y < ROWS;
    }

    private RectF cellRect(int x, int y) {
        float l = boardLeft + x * cell;
        float t = boardTop + y * cell;
        return new RectF(l, t, l + cell, t + cell);
    }

    private static class Bug {
        final int x;
        final int y;
        int level = 1;
        float hp = 80f;
        float maxHp = 80f;
        float damage = 11f;
        float range = 2.6f;
        float cooldown = 0f;

        Bug(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Frog {
        float x;
        float y;
        final int tier;
        float hp;
        final float maxHp;
        final float speed;
        final float defense;
        final int reward;
        final boolean boss;
        float wallAttackCooldown = 0f;
        List<Point> path;
        int pathIndex;
        boolean reachedTower;

        Frog(float x, float y, int tier, float maxHp, float speed, float defense, int reward, boolean boss) {
            this.x = x;
            this.y = y;
            this.tier = tier;
            this.hp = maxHp;
            this.maxHp = maxHp;
            this.speed = speed;
            this.defense = defense;
            this.reward = reward;
            this.boss = boss;
        }
    }
}
