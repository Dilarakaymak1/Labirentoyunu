package com.example.labirentoyunu;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Oyun başında ve can kaybedince düşmanın kaç adım boyunca beklediği
    private static final int GRACE_TICKS = 5;
    private int graceTicks = GRACE_TICKS;

    // ❄️ alınca düşmanın kaç adım boyunca donacağı
    private static final int FREEZE_TICKS = 6;
    private int freezeTicks = 0;

    private static final int MAX_LIVES = 5;

    // Seçilebilir karakterler
    private final String[] playerChars = {"🐱", "🐶", "🦊", "🐼", "🐸"};
    private final String[] enemyChars = {"👻", "👹", "🐍", "🤖", "🦇"};
    private int playerIndex = 0;
    private int enemyIndex = 0;

    private MazeView mazeView;
    private TextView txtMessage;
    private Button btnPlayerChar;
    private Button btnEnemyChar;
    private Button btnNewMaze;

    private int level = 1;
    private int lives = 3;
    private int score = 0;
    private int bestScore = 0; // telefona kaydedilen rekor
    private boolean levelDone = false;
    private boolean gameOver = false;

    private SharedPreferences prefs;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Düşman her adımda bu süre kadar bekler; seviye arttıkça kısalır
    private final Runnable enemyTick = new Runnable() {
        @Override
        public void run() {
            if (!levelDone && !gameOver) {
                if (graceTicks > 0) {
                    graceTicks--;          // oyun başı bekleme süresi
                } else if (freezeTicks > 0) {
                    freezeTicks--;         // düşman donmuş, kıpırdamaz
                    updateMessage();
                } else {
                    mazeView.moveEnemy();
                    checkCaught();
                }
            }
            handler.postDelayed(this, getEnemyDelay());
        }
    };

    // ---- Seviyeye göre zorluk formülleri ----

    // Labirent boyutu: 11'den başlar, her seviyede 2 artar, en fazla 25
    private int getMazeSize() {
        return Math.min(11 + 2 * (level - 1), 25);
    }

    // Düşman adım süresi (ms): 900'den başlar, her seviyede 80 azalır, en az 350
    private int getEnemyDelay() {
        return Math.max(350, 900 - 80 * (level - 1));
    }

    // Kovalama şansı: %50'den başlar, her seviyede %5 artar, en fazla %90
    private double getChaseChance() {
        return Math.min(0.9, 0.5 + 0.05 * (level - 1));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Rekoru telefon hafızasından oku (daha önce kaydedilmediyse 0)
        prefs = getSharedPreferences("labirent", MODE_PRIVATE);
        bestScore = prefs.getInt("best", 0);

        mazeView = findViewById(R.id.mazeView);
        txtMessage = findViewById(R.id.txtMessage);
        btnPlayerChar = findViewById(R.id.btnPlayerChar);
        btnEnemyChar = findViewById(R.id.btnEnemyChar);
        btnNewMaze = findViewById(R.id.btnNewMaze);

        updateCharacterButtons();
        startLevel();

        ((Button) findViewById(R.id.btnUp)).setOnClickListener(v -> move(0, -1));
        ((Button) findViewById(R.id.btnDown)).setOnClickListener(v -> move(0, 1));
        ((Button) findViewById(R.id.btnLeft)).setOnClickListener(v -> move(-1, 0));
        ((Button) findViewById(R.id.btnRight)).setOnClickListener(v -> move(1, 0));

        btnPlayerChar.setOnClickListener(v -> {
            playerIndex = (playerIndex + 1) % playerChars.length;
            updateCharacterButtons();
        });
        btnEnemyChar.setOnClickListener(v -> {
            enemyIndex = (enemyIndex + 1) % enemyChars.length;
            updateCharacterButtons();
        });

        // Seviye bittiyse -> sonraki seviye, aksi halde -> yeni oyun (1. seviye)
        btnNewMaze.setOnClickListener(v -> {
            if (levelDone) {
                level++;
            } else {
                level = 1;
                lives = 3;
                score = 0;
            }
            startLevel();
        });
    }

    // Mevcut seviyeyi kurar: labirent, düşman hızı ve zorluk ayarları
    private void startLevel() {
        levelDone = false;
        gameOver = false;
        graceTicks = GRACE_TICKS;
        freezeTicks = 0;
        mazeView.setChaseChance(getChaseChance());
        mazeView.newMaze(getMazeSize());
        btnNewMaze.setText("Yeni Oyun");
        updateMessage();
    }

    private void updateCharacterButtons() {
        btnPlayerChar.setText("Oyuncu: " + playerChars[playerIndex]);
        btnEnemyChar.setText("Düşman: " + enemyChars[enemyIndex]);
        mazeView.setPlayerEmoji(playerChars[playerIndex]);
        mazeView.setEnemyEmoji(enemyChars[enemyIndex]);
    }

    // Klavye: ok tuşları ve W A S D
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_W:
                    move(0, -1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_S:
                    move(0, 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_A:
                    move(-1, 0);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_D:
                    move(1, 0);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(enemyTick, getEnemyDelay());
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(enemyTick);
    }

    private void move(int dx, int dy) {
        if (levelDone || gameOver) return;

        boolean reachedExit = mazeView.movePlayer(dx, dy);
        collectItem(); // bastığımız karede eşya var mı?

        if (reachedExit) {
            levelDone = true;
            int bonus = 50 + 10 * level;
            score += bonus;
            saveBest();
            txtMessage.setText("Seviye " + level + " tamamlandı! 🎉 (+" + bonus + ")\n"
                    + "Skor: " + score + "   Rekor: " + bestScore);
            btnNewMaze.setText("Sonraki Seviye ▶");
        } else {
            checkCaught();
        }
    }

    // Oyuncunun bastığı karedeki eşyayı al ve etkisini uygula
    private void collectItem() {
        int item = mazeView.takeItemAtPlayer();
        if (item == MazeView.ITEM_FREEZE) {
            freezeTicks = FREEZE_TICKS;
        } else if (item == MazeView.ITEM_LIFE) {
            lives = Math.min(lives + 1, MAX_LIVES);
        }
        if (item != MazeView.ITEM_NONE) {
            updateMessage();
        }
    }

    private void checkCaught() {
        if (mazeView.isCaught()) {
            lives--;
            if (lives <= 0) {
                gameOver = true;
                saveBest();
                txtMessage.setText("Oyun bitti! 💀  Seviye: " + level + "\n"
                        + "Skor: " + score + "   Rekor: " + bestScore);
            } else {
                mazeView.resetPositions();
                graceTicks = GRACE_TICKS;
                freezeTicks = 0;
                updateMessage();
            }
        }
    }

    // Skor rekoru geçtiyse hem değişkene hem telefon hafızasına kaydet
    private void saveBest() {
        if (score > bestScore) {
            bestScore = score;
            prefs.edit().putInt("best", bestScore).apply();
        }
    }

    // Üstteki bilgi yazısı: seviye, can, skor, (donma varsa süresi) ve rekor
    private void updateMessage() {
        String text = "Seviye " + level + "   Can: " + lives + "   Skor: " + score;
        if (freezeTicks > 0) {
            text += "   ❄️ " + freezeTicks;
        }
        text += "\nRekor: " + bestScore;
        txtMessage.setText(text);
    }
}