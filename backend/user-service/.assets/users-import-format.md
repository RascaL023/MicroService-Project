# User Import Excel Format

Gunakan sheet pertama dengan header di baris pertama.

| name | email | gender |
| --- | --- | --- |
| Aura Adams | aura@example.com | P |
| John Doe | john@example.com | L |

Rules:
- File harus `.xlsx` atau `.xls`.
- Maksimal file 5MB.
- Maksimal 1000 baris data.
- `batch` dipilih di form upload/API multipart dan berlaku untuk seluruh file.
- `gender` bisa `L`, `P`, `Laki-laki`, `Perempuan`, `Male`, atau `Female`.
- Role sistem tidak diisi dari user-service. Auth-service otomatis memberi role default `USER`.
- Email duplikat di file masuk status `FAILED`.
- Email yang sudah aktif di database masuk status `SKIPPED`.
- Import bersifat parsial: row valid dibuat, row existing dilewati, row invalid masuk report.
