# User Import Excel Format

Gunakan sheet pertama dengan header di baris ke-4.

| Nama | Email | Jenis Kelamin | Jurusan |
| --- | --- | --- | --- |
| Aura Adams | aura@example.com | P | TI |
| John Doe | john@example.com | L | MI |

Rules:
- File harus `.xlsx` atau `.xls`.
- Maksimal file mengikuti konfigurasi multipart service, default 12MB.
- Maksimal 100 baris data.
- `batch` dipilih di form upload/API multipart dan berlaku untuk seluruh file.
- `jurusan` wajib diisi dengan ID jurusan aktif, contoh `TI`.
- `gender` bisa `L`, `P`, `Laki-laki`, `Perempuan`, `Male`, atau `Female`.
- Role sistem tidak diisi dari user-service. Auth-service otomatis memberi role default `USER`.
- Email duplikat di file masuk status `FAILED`.
- Email yang sudah aktif di database masuk status `SKIPPED`.
- Import bersifat parsial: row valid dibuat, row existing dilewati, row invalid masuk report.
