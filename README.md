# Labirent Oyunu 🐱

Android Studio ve Java ile yapılmış, seviyeli bir labirent oyunu. Düşmandan kaçıp çıkışa ulaşmaya çalışırsınız.

## Özellikler

- **Rastgele labirent:** DFS algoritmasıyla her seviyede yeni labirent üretilir, bazı duvarlar yıkılarak alternatif yollar açılır.
- **Akıllı düşman:** BFS algoritmasıyla oyuncuya en kısa yoldan ilerler, ara sıra şaşırıp rastgele yöne gider.
- **Seviye sistemi:** Her seviyede labirent büyür, düşman hızlanır ve daha sık kovalar.
- **Can sistemi:** 3 canla başlanır, en fazla 5 cana çıkılabilir.
- **Eşyalar:**
  - ❄️ Düşmanı birkaç adım dondurur
  - ❤️ +1 can verir
- **Skor ve rekor:** Seviye bitirdikçe puan kazanılır, en yüksek skor telefona kaydedilir.
- **Karakter seçimi:** Oyuncu ve düşman için farklı emoji karakterleri.
- **Kontroller:** Ekrandaki yön tuşları veya klavyeden ok tuşları / W A S D.

## Kullanılan teknolojiler

- Java
- Android SDK (Canvas ile özel çizim)
- SharedPreferences (rekor kaydı)

## Nasıl çalıştırılır?

1. Projeyi indirin veya klonlayın.
2. Android Studio ile açın.
3. Gradle senkronizasyonunun bitmesini bekleyin.
4. Emülatör veya bağlı bir telefonla **Run** tuşuna basın.

## Proje yapısı

| Dosya Görevi 
| `Maze.java` | Labirent verisi ve rastgele üretim (DFS) |
| `MazeView.java` | Çizim, hareket ve düşman yapay zekası (BFS) |
| `MainActivity.java` | Oyun kuralları, seviye, can, skor ve kontroller |
