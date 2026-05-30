# Sistem Informasi Manajemen Divisi Pendidikan dan Pelatihan

## Problems indentification
1. Pelaporan nilai yang masih manual via whatsapp, sehingga perlu pencatatan tambahan
2. Manajemen kelompok, pelajar dan instruktur yang kurang rapi
3. Soal - soal kuis/tugas yang kurang terintegrasi, sehingga menyusahkan pelajar untuk latihan soal dari kelompok lain
4. Tidak adanya bukti/laporan jumlah pelatihan yang telah dilaksanakkan 
5. Sistem rangking yang masih tertutup, hanya bisa diketahui setelah UTS atau UAS
6. Kesenjangan kesulitan soal antar kelompok 


## User targets
1. Owner/Admin yang perlu laporan
2. Pelajar yang agak terhambat saat ingin berlatih soal2 dari kelompok lain
3. Instruktur, yaitu pengguna yang membuat soal, mengelola tugas/kuis, dan memberikan nilai.


## Main features
1. Autentikasi pengguna

   * Login
   * Register akun
   * Logout
   * Ban/nonaktifkan akun

2. Manajemen pengguna

   * Data admin
   * Data instruktur
   * Data pelajar
   * Role dan hak akses pengguna

3. Manajemen kelompok pelatihan

   * CRUD kelompok
   * Menambahkan pelajar ke kelompok
   * Menentukan instruktur kelompok
   * Melihat daftar anggota kelompok

4. Manajemen pelatihan

   * Membuat jadwal/topik pelatihan
   * Mencatat pelatihan yang telah dilaksanakan
   <!-- * Melihat riwayat pelatihan -->
   * Laporan jumlah pelatihan

5. Manajemen soal

   * Instruktur mengunggah/membuat soal
   * Admin melakukan acc/reject soal
   * Pelajar dapat melihat soal yang sudah disetujui

6. Manajemen tugas/kuis

   * Instruktur upload tugas/kuis
   * Pelajar mengerjakan atau mengunggah jawaban
   * Instruktur memberikan nilai
   * Pelajar melihat hasil nilai

7. Laporan nilai

   * Laporan nilai per pelajar
   * Laporan nilai per kelompok
   * Rata-rata nilai kelompok
   * Rekap nilai tugas/kuis

8. Dashboard admin

   * Jumlah pelajar
   * Jumlah instruktur
   * Jumlah kelompok
   * Jumlah pelatihan
   * Jumlah soal yang menunggu persetujuan


## Must exist features
1. Login, register, logout, dan ban user.
2. Role user: admin, instruktur, dan pelajar.
3. CRUD data pelajar.
4. CRUD data instruktur.
5. CRUD kelompok.
6. Manajemen anggota kelompok.
7. Manajemen soal.
8. Acc/reject soal.
9. Manajemen tugas/kuis.
10. Input dan laporan nilai.
11. Validasi input.
12. Database.
13. Session management.
14. Paginasi data.


## Important Features
1. Search data pelajar, instruktur, soal, dan kelompok.
2. Dashboard admin.
3. Export laporan nilai.
4. Export laporan pelatihan.
5. Riwayat pelatihan per kelompok.


## Main flow
1. Admin membuat/mengelola akun instruktur dan pelajar.
2. Admin membuat kelompok pelatihan.
3. Admin menambahkan pelajar dan instruktur ke kelompok.
4. Instruktur membuat soal atau tugas/kuis.
5. Admin menyetujui atau menolak soal.
6. Pelajar mengakses soal latihan atau mengumpulkan tugas.
7. Instruktur memberikan nilai.
8. Admin melihat laporan nilai dan laporan pelatihan.


## Definisi Selesai
Project dianggap selesai jika admin dapat mengelola pengguna, kelompok, soal, dan laporan; instruktur dapat membuat tugas/kuis serta memberi nilai; pelajar dapat melihat soal, mengumpulkan tugas, dan melihat hasil nilai.
