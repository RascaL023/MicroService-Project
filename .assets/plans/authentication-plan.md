  # Plan Aktivasi Akun via Email

  ## Summary

  - Flow ini bukan register, tapi aktivasi akun: admin bikin user dulu di user-service, lalu user “claim” akun via email.
  - Verifikasi email dilakukan oleh token di link email. Setelah token valid, user isi password untuk pertama kali.
  - Email dikirim lewat notification-service Spring Boot + Gmail SMTP, tapi async lewat Redis Stream supaya request user tidak nunggu proses
    SMTP.
  - Login normal setelah aktivasi: email + password.

  ## Key Changes

  ### User Service

  - Saat admin create/patch user, user-service publish event ke Redis Stream:
      - UserCreated: berisi userId, email, roleIds, isBanned.
      - UserEmailUpdated: berisi userId, oldEmail, newEmail.
      - UserBanUpdated: berisi userId, isBanned.
  - Untuk sekarang, kalau email diubah admin, auth-service cukup update email juga. Password tetap berlaku.

  ### Auth Service

  - Ubah identity auth dari username menjadi email.
  - Auth-service menyimpan data login:

    user_id
    email
    password_hash nullable
    status: PENDING_ACTIVATION | ACTIVE | BANNED
    email_verified_at nullable

  - Endpoint baru:

    POST /api/auths/activations/request
    POST /api/auths/activations/complete
    POST /api/auths/login
    POST /api/auths/logout

  - request activation:
      - input: email
      - kalau email ada dan belum aktif, buat token aktivasi
      - simpan hash token + TTL
      - publish job email ke Redis Stream
      - response tetap generik: “Jika email valid, link aktivasi dikirim”
  - complete activation:
      - input: token, password
      - token valid = email dianggap terbukti
      - set password hash
      - ubah status jadi ACTIVE
      - langsung buat session dan return login response
  - Hapus flow register public lama, karena user tidak boleh daftar bebas.

  ### Notification Service

  - Buat/lanjutkan notification-service Spring Boot.
  - Consume Redis Stream notification:emails.
  - Kirim email via Gmail SMTP app password.
  - Email sending berjalan background, bukan di request auth-service.
  - Job minimal:

    {
      "type": "ACTIVATION_EMAIL",
      "to": "user@mail.com",
      "activationUrl": "http://localhost:5173/activate?token=..."
    }

  - Kalau gagal kirim, log error dan retry sederhana.

  ### Frontend

  - Tambah halaman:

    /activate/request
    /activate?token=...
    /login
    /dashboard atau /profile

  - Flow UI:
      - user input email di halaman aktivasi
      - user buka link dari email
      - halaman aktivasi membaca token dari URL
      - user input password
      - setelah sukses, redirect ke dashboard/profile

  ### Redis
    notification:emails

  - Consumer harus idempotent: event user yang sama tidak boleh bikin duplicate auth identity.

  ## Test Plan

  - Admin create user di user-service, auth-service menerima event dan membuat identity PENDING_ACTIVATION.
  - User request aktivasi dengan email valid, job email masuk ke Redis Stream.
  - User request aktivasi dengan email tidak valid, response tetap generik.
  - Token aktivasi valid + password valid membuat akun ACTIVE dan langsung login.
  - Token expired/reused ditolak.
  - Login sebelum aktivasi ditolak.
  - Login setelah aktivasi sukses pakai email + password.
  - Admin update email, auth-service ikut update email.
  - Notification-service tetap tidak membuat endpoint auth lambat walau SMTP Gmail lambat.
  - Build/check service terkait tetap pass.

  ## Assumptions

  - Untuk v1 kuliahan, Gmail SMTP app password dipakai sebagai default karena gratis dan mudah.
  - notification-service dibuat dengan Spring Boot Java.
  - Auth identity tetap menyimpan userId + email; user tetap hanya input email + password.
  - Email update sementara hanya sinkron biasa, tidak reset aktivasi.
  - Link token adalah bukti email; input password di halaman aktivasi adalah proses set password, bukan bukti tambahan.
