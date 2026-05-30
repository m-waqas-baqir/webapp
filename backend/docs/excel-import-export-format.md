# Excel bulk import & export

Workbooks must be **`.xlsx`**. The reader uses the **“Data Entry”** sheet if present, otherwise the first sheet. The **header row** is auto-detected (first row that contains all required column headers), so an instruction row above headers is supported. Header names are matched case-insensitively; spaces are ignored (e.g. `plot number` → `plotnumber`).

**API (base `/api/v1`)**

| Module   | Import (multipart `file`)     | Template (GET, binary)   | Export (GET, binary)   |
|----------|--------------------------------|--------------------------|------------------------|
| Plots    | `POST /plots/import`          | `GET /plots/template`     | `GET /plots/export`     |
| Khayabans| `POST /khayabans/import`      | `GET /khayabans/template` | `GET /khayabans/export` |
| Rentals  | `POST /rental-properties/import` | `GET /rental-properties/template` | `GET /rental-properties/export` |

**RBAC:** `DIRECTOR` and `ADMIN` may import, download templates, and export. `AGENT` may download templates and export data (no bulk upload).

Exports use **names** (phase, khayaban, owner, agent), not internal IDs.

---

## Plots import columns

| Column        | Description |
|---------------|-------------|
| plotNumber    | Required; unique per khayaban |
| size          | Required |
| price         | Required |
| status        | Valid plot status enum value |
| phaseName     | Must match an existing phase (by name) |
| khayabanName  | Must exist under that phase |
| ownerName     | Must match an owner record |
| agentName     | Must match a user with agent role |

---

## Khayabans import columns

| Column    | Description |
|-----------|-------------|
| name      | Required |
| phaseName | Must match an existing phase |

---

## Rentals import columns

| Column     | Description |
|------------|-------------|
| title      | Required |
| type       | `RESIDENTIAL` or `COMMERCIAL` |
| address    | Required |
| rentAmount | Required |
| status     | Valid rental property status |
| ownerName  | Must match an owner |
| agentName  | Must match a user with agent role |

---

## Import response

JSON envelope `ApiResponse<BulkImportResult>`:

```json
{
  "success": true,
  "data": {
    "successCount": 10,
    "failureCount": 2,
    "errors": [
      { "row": 5, "message": "Phase not found" }
    ]
  }
}
```

`row` is the **1-based Excel row index** (including the header row), per backend convention.

Invalid rows are **not** inserted; valid rows commit independently (partial success).

---

## Downloadable templates (recommended)

Pre-built **`.xlsx`** files from the **template** endpoints include:

- **Data Entry** — instruction row, bold headers (required columns on green background, optional on blue), numeric formats for size/price/rent, a **sample row** with `plotNumber` / `name` / `title` = **EXAMPLE** (remove before real import; the server **skips** that marker row if left in place).
- **Reference Data** — current **phase**, **khayaban**, **owner**, and **agent** names for list-based validation in Excel, plus inline lists for **status** (and **type** for rentals).

Imports are validated against the same **reference catalog** in the app: values not matching known phases, phase/khayaban pairs, owners, agents, or enum values are rejected with a message such as `Invalid phaseName "…" (not in current reference data)`.
