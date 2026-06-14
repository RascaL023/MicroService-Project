# User Import Excel Format

Gunakan sheet pertama dengan header di baris pertama.

| name | email | batch | gender |
| --- | --- | --- | --- |
| Aura Adams | aura@example.com | 25 | P |
| John Doe | john@example.com | 24 | L |

Rules:
- File harus `.xlsx` atau `.xls`.
- Maksimal file 5MB.
- Maksimal 1000 baris data.
- `batch` harus ID batch yang aktif.
- `gender` bisa `L`, `P`, `Laki-laki`, `Perempuan`, `Male`, atau `Female`.
- Role sistem tidak diisi dari user-service. Auth-service otomatis memberi role default `USER`.
- Email duplikat di database atau di file akan ditolak per baris.
- Import bersifat parsial: baris valid tetap dibuat, baris invalid masuk daftar `errors`.
