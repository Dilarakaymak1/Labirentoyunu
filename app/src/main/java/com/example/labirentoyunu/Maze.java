package com.example.labirentoyunu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Izgara tabanlı labirent. size tek sayı olmalı (örn. 15).
// grid[y][x] == true ise duvar, false ise yol demektir.
public class Maze {

    // Kapalı duvarların yüzde kaçının yıkılacağı (0.0 - 1.0).
    // Büyütürsen daha çok alternatif yol olur, oyun kolaylaşır.
    private static final double LOOP_CHANCE = 0.25;

    public final int size;
    public final boolean[][] grid;
    private final Random random = new Random();

    public Maze(int size) {
        this.size = size;
        this.grid = new boolean[size][size];
        // Başlangıçta her yer duvar
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                grid[y][x] = true;
            }
        }
        generate();
        addLoops(LOOP_CHANCE);
    }

    // DFS (derinlik öncelikli arama) ile rastgele labirent üretir
    private void generate() {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        grid[1][1] = false;
        stack.push(new int[]{1, 1});

        // 2 kare atlayarak dört yöne gidebiliriz (arada duvar kalır)
        int[][] dirs = {{0, -2}, {0, 2}, {-2, 0}, {2, 0}};

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            int x = cur[0];
            int y = cur[1];

            // Henüz ziyaret edilmemiş (hala duvar olan) komşuları bul
            List<int[]> options = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 1 && nx < size - 1 && ny >= 1 && ny < size - 1 && grid[ny][nx]) {
                    options.add(d);
                }
            }

            if (options.isEmpty()) {
                stack.pop(); // çıkmaz sokak, geri dön
            } else {
                int[] d = options.get(random.nextInt(options.size()));
                grid[y + d[1] / 2][x + d[0] / 2] = false; // aradaki duvarı kır
                grid[y + d[1]][x + d[0]] = false;         // yeni kareyi aç
                stack.push(new int[]{x + d[0], y + d[1]});
            }
        }
    }

    // Bazı duvarları yıkarak döngü (alternatif yol) oluşturur.
    // Yollar tek sayılı koordinatlarda (odd, odd), köşe direkleri ise
    // (çift, çift) koordinatlarda durur. Direklere dokunmayız;
    // sadece iki yolu ayıran duvar parçalarını yıkarız.
    private void addLoops(double chance) {
        for (int y = 1; y < size - 1; y++) {
            for (int x = 1; x < size - 1; x++) {
                boolean isPillar = (x % 2 == 0 && y % 2 == 0);
                boolean isCell = (x % 2 == 1 && y % 2 == 1);
                if (grid[y][x] && !isPillar && !isCell && random.nextDouble() < chance) {
                    grid[y][x] = false;
                }
            }
        }
    }

    // (sx, sy) karesinden her yol karesine kaç adımda gidilir (BFS).
    // Ulaşılamayan kareler ve duvarlar -1 olarak kalır.
    public int[][] distancesFrom(int sx, int sy) {
        int[][] dist = new int[size][size];
        for (int[] row : dist) {
            Arrays.fill(row, -1);
        }

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        dist[sy][sx] = 0;
        queue.add(new int[]{sx, sy});

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (!isWall(nx, ny) && dist[ny][nx] == -1) {
                    dist[ny][nx] = dist[cur[1]][cur[0]] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) return true;
        return grid[y][x];
    }
}