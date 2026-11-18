package com.example.labirentoyunu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class MazeView extends View {

    // Eşya türleri (MainActivity de bunları kullanır)
    public static final int ITEM_NONE = 0;
    public static final int ITEM_FREEZE = 1; // ❄️ düşmanı dondur
    public static final int ITEM_LIFE = 2;   // ❤️ ekstra can

    // Labirent boyutu (her seviyede MainActivity belirler)
    private int size = 11;

    // Düşmanın kaç adımının oyuncuyu kovalayacağı (0.0 - 1.0). Seviyeyle artar.
    private double chaseChance = 0.5;
    private final Random random = new Random();

    private Maze maze = new Maze(size);

    // items[y][x] = o karedeki eşya türü (ITEM_NONE ise boş)
    private int[][] items = new int[size][size];

    // Oyuncunun ızgaradaki konumu (piksel değil, kare numarası)
    private int playerX = 1;
    private int playerY = 1;

    // Düşmanın o anki konumu ve başlangıç noktası
    private int enemyX = 1;
    private int enemyY = 1;
    private int enemyStartX = 1;
    private int enemyStartY = 1;

    // Çıkış kapısı: sağ alt köşedeki yol karesi
    private int exitX = size - 2;
    private int exitY = size - 2;

    // Ekranda gösterilecek karakterler (emoji)
    private String playerEmoji = "🐱";
    private String enemyEmoji = "👻";

    private final Paint wallPaint = new Paint();
    private final Paint pathPaint = new Paint();
    private final Paint exitPaint = new Paint();
    private final Paint emojiPaint = new Paint();

    public MazeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wallPaint.setColor(Color.parseColor("#2B2B44"));
        pathPaint.setColor(Color.parseColor("#E8E8F0"));
        exitPaint.setColor(Color.parseColor("#2A9D8F"));
        emojiPaint.setAntiAlias(true);
        emojiPaint.setTextAlign(Paint.Align.CENTER);

        placeEnemyStart();
        placeItems();
        resetPositions();
    }

    public void setPlayerEmoji(String emoji) {
        playerEmoji = emoji;
        invalidate();
    }

    public void setEnemyEmoji(String emoji) {
        enemyEmoji = emoji;
        invalidate();
    }

    // Düşmanın kovalama şansını ayarlar (seviye arttıkça yükselir)
    public void setChaseChance(double chance) {
        chaseChance = chance;
    }

    // Düşmanın başlangıç karesini seçer:
    // oyuncudan en az 'size' adım uzak VE çıkıştan en az size/2 adım uzak
    private void placeEnemyStart() {
        int[][] fromPlayer = maze.distancesFrom(1, 1);
        int[][] fromExit = maze.distancesFrom(exitX, exitY);

        ArrayList<int[]> candidates = new ArrayList<>();

        int bestX = size - 2;
        int bestY = 1;
        int bestDist = -1;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (maze.isWall(x, y)) continue;

                int dp = fromPlayer[y][x];
                if (dp > bestDist) {
                    bestDist = dp;
                    bestX = x;
                    bestY = y;
                }
                if (dp >= size && fromExit[y][x] >= size / 2) {
                    candidates.add(new int[]{x, y});
                }
            }
        }

        if (candidates.isEmpty()) {
            enemyStartX = bestX;
            enemyStartY = bestY;
        } else {
            int[] pick = candidates.get(random.nextInt(candidates.size()));
            enemyStartX = pick[0];
            enemyStartY = pick[1];
        }
    }

    // Labirentteki boş yol karelerine rastgele eşyalar yerleştirir.
    // Oyuncunun, düşmanın başlangıç karesine ve çıkışa eşya konmaz.
    private void placeItems() {
        items = new int[size][size];

        ArrayList<int[]> free = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (maze.isWall(x, y)) continue;
                boolean isPlayerStart = (x == 1 && y == 1);
                boolean isExit = (x == exitX && y == exitY);
                boolean isEnemyStart = (x == enemyStartX && y == enemyStartY);
                if (!isPlayerStart && !isExit && !isEnemyStart) {
                    free.add(new int[]{x, y});
                }
            }
        }
        Collections.shuffle(free, random); // kareleri karıştır

        // Karışık listenin başından sırayla al: 2 tane ❄️, 1 tane ❤️
        int index = 0;
        index = putItems(free, index, ITEM_FREEZE, 2);
        putItems(free, index, ITEM_LIFE, 1);
    }

    // free listesinde index'ten başlayarak 'count' kareye 'type' eşyası koyar.
    // Kaldığı sırayı geri döndürür.
    private int putItems(ArrayList<int[]> free, int index, int type, int count) {
        while (count > 0 && index < free.size()) {
            int[] cell = free.get(index);
            items[cell[1]][cell[0]] = type;
            index++;
            count--;
        }
        return index;
    }

    // Oyuncunun durduğu karedeki eşyayı alır (karedeki eşya silinir).
    // Eşya türünü döndürür; yoksa ITEM_NONE.
    public int takeItemAtPlayer() {
        int item = items[playerY][playerX];
        if (item != ITEM_NONE) {
            items[playerY][playerX] = ITEM_NONE;
            invalidate();
        }
        return item;
    }

    // Oyuncuyu ve düşmanı başlangıç noktalarına koyar (labirent aynı kalır)
    public void resetPositions() {
        playerX = 1;
        playerY = 1;
        enemyX = enemyStartX;
        enemyY = enemyStartY;
        invalidate();
    }

    // Verilen boyutta yeni labirent üret ve her şeyi başa al.
    // newSize tek sayı olmalı (11, 13, 15, ...)
    public void newMaze(int newSize) {
        size = newSize;
        maze = new Maze(size);
        exitX = size - 2;
        exitY = size - 2;
        placeEnemyStart();
        placeItems();
        resetPositions();
    }

    // Oyuncuyu (dx, dy) kadar hareket ettirir.
    // Çıkışa ulaştıysa true döner.
    public boolean movePlayer(int dx, int dy) {
        int nx = playerX + dx;
        int ny = playerY + dy;

        if (!maze.isWall(nx, ny)) {
            playerX = nx;
            playerY = ny;
            invalidate();
        }
        return playerX == exitX && playerY == exitY;
    }

    // Düşman yakaladı mı? (aynı karedeler mi)
    public boolean isCaught() {
        return playerX == enemyX && playerY == enemyY;
    }

    // Düşmanı duvar olmayan rastgele bir komşu kareye taşır
    private void randomStep() {
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        int startIndex = random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int[] d = dirs[(startIndex + i) % 4];
            int nx = enemyX + d[0];
            int ny = enemyY + d[1];
            if (!maze.isWall(nx, ny)) {
                enemyX = nx;
                enemyY = ny;
                invalidate();
                return;
            }
        }
    }

    // Düşmanı oyuncuya doğru BİR kare ilerletir (BFS ile en kısa yol).
    public void moveEnemy() {
        int n = maze.size;
        int start = enemyY * n + enemyX;
        int target = playerY * n + playerX;

        if (start == target) return;

        // Bazen kovalamak yerine rastgele bir yöne git
        if (random.nextDouble() > chaseChance) {
            randomStep();
            return;
        }

        int[] prev = new int[n * n];
        Arrays.fill(prev, -1);
        prev[start] = start;

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        while (!queue.isEmpty() && prev[target] == -1) {
            int cur = queue.poll();
            int cx = cur % n;
            int cy = cur / n;

            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                int next = ny * n + nx;
                if (!maze.isWall(nx, ny) && prev[next] == -1) {
                    prev[next] = cur;
                    queue.add(next);
                }
            }
        }

        if (prev[target] == -1) return;

        int step = target;
        while (prev[step] != start) {
            step = prev[step];
        }

        enemyX = step % n;
        enemyY = step / n;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, w);
    }

    // Verilen karenin ortasına emoji çizer
    private void drawEmoji(Canvas canvas, String emoji, int gx, int gy, float cell) {
        emojiPaint.setTextSize(cell * 0.75f);
        Paint.FontMetrics fm = emojiPaint.getFontMetrics();
        float offset = -(fm.ascent + fm.descent) / 2f;
        float cx = gx * cell + cell / 2f;
        float cy = gy * cell + cell / 2f;
        canvas.drawText(emoji, cx, cy + offset, emojiPaint);
    }

    // Eşya türüne göre çizilecek emojiyi verir
    private String itemEmoji(int type) {
        switch (type) {
            case ITEM_FREEZE:
                return "❄️";
            case ITEM_LIFE:
                return "❤️";
            default:
                return "";
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cell = (float) getWidth() / maze.size;

        // 1) Labirenti çiz
        for (int y = 0; y < maze.size; y++) {
            for (int x = 0; x < maze.size; x++) {
                Paint p = maze.isWall(x, y) ? wallPaint : pathPaint;
                canvas.drawRect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell, p);
            }
        }

        // 2) Çıkış kapısı
        canvas.drawRect(exitX * cell, exitY * cell,
                (exitX + 1) * cell, (exitY + 1) * cell, exitPaint);
        drawEmoji(canvas, "🏁", exitX, exitY, cell);

        // 3) Eşyalar (karakterlerin altında kalsın diye önce çizilir)
        for (int y = 0; y < maze.size; y++) {
            for (int x = 0; x < maze.size; x++) {
                if (items[y][x] != ITEM_NONE) {
                    drawEmoji(canvas, itemEmoji(items[y][x]), x, y, cell);
                }
            }
        }

        // 4) Düşman
        drawEmoji(canvas, enemyEmoji, enemyX, enemyY, cell);

        // 5) Oyuncu (en üstte görünsün diye en son)
        drawEmoji(canvas, playerEmoji, playerX, playerY, cell);
    }
}