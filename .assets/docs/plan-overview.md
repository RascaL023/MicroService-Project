# Sistem Informasi Manajemen Divisi Pendidikan dan Pelatihan

## Identifikasi Masalah

1. Pelaporan nilai masih manual via WhatsApp, sehingga perlu pencatatan tambahan.
2. Manajemen kelompok, pelajar, dan instruktur belum cukup rapi.
3. Soal kuis/tugas belum terintegrasi, sehingga pelajar sulit memakai soal dari kelompok lain untuk latihan.
4. Belum ada bukti atau laporan jumlah pelatihan yang telah dilaksanakan.
5. Sistem ranking masih tertutup dan biasanya baru diketahui setelah UTS atau UAS.

## Sasaran Pengguna

1. Owner/Admin yang membutuhkan laporan.
2. Pelajar yang ingin berlatih dari materi atau soal lintas kelompok.
3. Instruktur yang membuat soal, mengelola tugas/kuis, dan memberikan nilai.

## Fitur Utama

1. Autentikasi pengguna

   - Login.
   - Aktivasi akun.
   - Logout.
   - Ban/nonaktifkan akun.
   - Reset password.
   - Ganti email.

2. Manajemen pengguna

   - Data admin.
   - Data instruktur.
   - Data pelajar.
   - Data kursus.
   - Role dan hak akses pengguna.

3. Manajemen kelompok pelatihan

   - CRUD kelompok.
   - Menambahkan pelajar ke kelompok.
   - Menentukan instruktur kelompok.
   - Melihat daftar anggota kelompok.

4. Manajemen pelatihan

   - Membuat jadwal pelatihan.
   - Mencatat pelatihan yang telah dilaksanakan.
   - Melihat riwayat pelatihan.
   - Membuat laporan jumlah pelatihan.

5. Manajemen soal

   - Instruktur mengunggah atau membuat soal.
   - Pelajar dapat melihat soal yang sudah disetujui.

6. Manajemen tugas/kuis

   - Instruktur mengunggah tugas/kuis.
   - Instruktur memberikan nilai.
   - Pelajar melihat hasil nilai.

7. Laporan nilai

   - Laporan nilai per pelajar.
   - Laporan nilai per kelompok.
   - Rata-rata nilai kelompok.
   - Rekap nilai tugas/kuis.

8. Dashboard admin

   - Jumlah instruktur.
   - Jumlah kelompok.

## Fitur Wajib

1. Login, aktivasi, logout, dan ban user.
2. Role user: admin, instruktur, dan pelajar.
3. CRUD data pelajar.
4. CRUD data instruktur.
5. CRUD kelompok.
6. Manajemen anggota kelompok.
7. Manajemen soal.
8. Manajemen tugas/kuis.
9. Input dan laporan nilai.
10. Validasi input.
11. Database.
12. Manajemen session.
13. Paginasi data.

## Fitur Penting

1. Pencarian data pelajar, instruktur, soal, dan kelompok.
2. Dashboard admin.
3. Ekspor laporan nilai.

## Alur Utama

1. Admin membuat atau mengelola akun instruktur dan pelajar.
2. Admin membuat kelompok pelatihan.
3. Admin menambahkan pelajar dan instruktur ke kelompok.
4. Instruktur membuat soal atau tugas/kuis.
5. Pelajar mengakses soal latihan atau menandai tugas offline sebagai selesai.
6. Instruktur memberikan nilai.
7. Admin melihat laporan nilai.
