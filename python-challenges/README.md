# Python Challenges (Task 1)

Direktori ini berisi solusi untuk coding challenge Phase 1.

## Cara Menjalankan Test

Pastikan Python 3.11+ dan `pytest` sudah terinstal, lalu jalankan:

```bash
pytest test_solutions.py
```

---

## 1. Reverse Words

Fungsi ini membalik huruf di setiap kata, tapi tetap menjaga spasi persis seperti aslinya.

Tantangannya ada di cara kita memotong string. `split()` biasa tanpa argumen akan membuang spasi di awal/akhir dan menganggap beberapa spasi berurutan sebagai satu pemisah — jadi informasi spasinya hilang. Solusinya pakai `split(" ")` yang memotong tepat di setiap satu karakter spasi, sehingga ketika digabung kembali dengan `" ".join()`, formatnya terjaga utuh.

```python
"  halo   dunia ".split(" ")
# ['', '', 'halo', '', '', 'dunia', '']
# string kosong di sini berperan sebagai "penanda tempat" spasi
```

---

## 2. Two Sum

Dua pendekatan disediakan untuk mencari pasangan angka yang jumlahnya sama dengan `target`.

**Brute Force** mengecek semua kemungkinan pasangan menggunakan nested loop.

- Time: O(n²) — Space: O(1)

**Hash Map** hanya melewati list sekali. Untuk setiap angka, kita cek apakah "pasangan yang dibutuhkan" sudah pernah kita lihat sebelumnya, menggunakan dictionary sebagai catatan.

- Time: O(n) — Space: O(n)

Hash map jauh lebih cepat untuk input besar. Trade-off-nya hanya di penggunaan memori tambahan — yang dalam kebanyakan kasus sangat sepadan.

---

## 3. FizzBuzz

Implementasi standar FizzBuzz dengan satu catatan penting: **pengecekan kelipatan 15 harus dilakukan paling awal**, sebelum cek 3 atau 5. Kalau tidak, angka seperti 15 atau 30 akan langsung tertangkap sebagai "Fizz" dan tidak pernah jadi "FizzBuzz".

Logika dipisah menjadi dua fungsi:

- `fizzbuzz(n)` — mengembalikan list, mudah untuk di-test
- `print_fizzbuzz(n)` — wrapper tipis yang hanya menampilkan hasilnya ke konsol
