# Consumption Reports API

Base URL: `/consumption-reports`

API pentru generarea și descărcarea rapoartelor Excel de consum pentru contractele de închiriere.

---

## Endpoints

### 1. Generate Yearly Report
**GET** `/consumption-reports/rental/{rentalAgreementId}/year/{year}`

Generează și descarcă raport Excel pentru un contract și an specific.

#### Path Parameters
- `rentalAgreementId` (Long, **required**) - ID-ul contractului de închiriere
- `year` (Integer, **required**) - Anul pentru care se generează raportul (ex: 2025)

#### Response (200 OK)
- **Content-Type:** `application/octet-stream`
- **Content-Disposition:** `attachment; filename="<tenant>-<spatiu>-<an>.xlsx"`
- **Body:** Binary Excel file

#### Exemplu
```http
GET /consumption-reports/rental/2/year/2025
```

**Descarcă:** `SC-Auto-Adria-SRL-A5_3-2025.xlsx`

**Conținut Excel:**
- Header: Tenant info, contract, spațiu
- Tabel consumuri lunare per tip contor (E-ON, APA, GAZ)
- Calcule automate: TOTAL, Cota întreținere 3%, TOTAL FINAL

---

### 2. Generate Multi-Year Report
**GET** `/consumption-reports/rental/{rentalAgreementId}/years?start={startYear}&end={endYear}`

Generează raport Excel multi-anual cu o fișă (sheet) per an.

#### Path Parameters
- `rentalAgreementId` (Long, **required**) - ID-ul contractului

#### Query Parameters
- `start` (Integer, optional) - An început (default: anul curent)
- `end` (Integer, optional) - An final (default: anul curent)

#### Response (200 OK)
- **Content-Type:** `application/octet-stream`
- Excel cu mai multe sheets (câte unul per an)

#### Exemplu
```http
GET /consumption-reports/rental/2/years?start=2023&end=2025
```

**Descarcă:** `SC-Auto-Adria-SRL-A5_3-2023-2025.xlsx`

**Conținut:** 3 sheets: "ANUL 2023", "ANUL 2024", "ANUL 2025"

---

### 3. List Active Rental Agreements
**GET** `/consumption-reports/all-active/year/{year}`

Listează toate contractele active pentru care se pot genera rapoarte.

#### Path Parameters
- `year` (Integer, **required**) - Anul de referință

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Found 5 active rental agreements",
  "data": [
    {
      "rentalAgreementId": 2,
      "tenantName": "SC Auto Adria SRL",
      "spaceName": "A5/3"
    },
    {
      "rentalAgreementId": 3,
      "tenantName": "AIRSOFT IASI S.R.L.",
      "spaceName": "A1"
    }
  ]
}
```

---

## Workflow de Utilizare

### Frontend - Download Raport

```typescript
async function downloadConsumptionReport(
  rentalAgreementId: number, 
  year: number
) {
  const response = await fetch(
    `/consumption-reports/rental/${rentalAgreementId}/year/${year}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );
  
  if (!response.ok) {
    throw new Error('Failed to generate report');
  }
  
  // Download file
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `raport-${year}.xlsx`;
  a.click();
  window.URL.revokeObjectURL(url);
}
```

### Frontend - List și Select Contracte

```typescript
async function showReportSelector(year: number) {
  // Obține lista contractelor active
  const response = await fetch(
    `/consumption-reports/all-active/year/${year}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );
  
  const { data: contracts } = await response.json();
  
  // Afișează în UI pentru selecție
  contracts.forEach(contract => {
    console.log(
      `${contract.tenantName} - ${contract.spaceName} (ID: ${contract.rentalAgreementId})`
    );
  });
  
  // User selectează un contract → download raport
  await downloadConsumptionReport(selectedContract.rentalAgreementId, year);
}
```

---

## Structura Raportului Excel

### Header (rânduri 1-4)
- Rând 1: Numele clădirii/companiei
- Rând 2: CUI + Informații tenant (Chirias, Nume tenant)
- Rând 3: Contract nr. + Data început
- Rând 4: Informații spațiu

### Titlu (rânduri 5-7)
- "ANEXA FACTURA"
- "cu serviciile oferite ce includ si utilit atile"

### Tabel Consum (rânduri 9+)

**Header:** | - | - | IAN | FEB | MAR | APR | MAI | IUN | IUL | AUG | SEP | OCT | NOI | DEC |

**Rânduri per tip contor:**
- **E-ON** (electricitate)
  - Row 1: kw consumați
  - Row 2: lei cost

- **APA**
  - Row 1: mc consumați
  - Row 2: lei cost

- **GAZ**
  - Row 1: mc consumați
  - Row 2: lei cost

**Rânduri servicii:**
- SERV. VIDANJ (mc / lei)
- SERV. SALUBR.
- COTA AB. ALARMA
- MANIP. PALEȚI
- Deszăpezire
- DIVERSE

**Totaluri:**
- **TOTAL** - suma tuturor costurilor pe coloană
- **Cota întretin 3%** - 3% din total (calculat automat)
- **TOTAL FINAL** - total + cotă (calculat automat)

---

## Note Importante

1. **Rapoartele se generează on-demand** - nu sunt pre-generate
2. **Datele sunt extrase din contoarele spațiului închiriat**
3. **Calculele sunt automate** - consum, cost, totaluri
4. **Format Excel nativ** (.xlsx) cu formule funcționale
5. **Un contract activ** = contract cu `endDate` null sau în viitor

---

## Erori Posibile

### 404 Not Found
```json
{
  "success": false,
  "message": "Rental agreement not found"
}
```
**Cauză:** ID contract invalid

### 400 Bad Request
```json
{
  "success": false,
  "message": "Start year cannot be after end year"
}
```
**Cauză:** Parametri invalizi pentru raport multi-anual

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Failed to generate Excel report: ..."
}
```
**Cauză:** Eroare la generarea Excel-ului

---

## Vezi și

- [Index Counters API](./06-index-counters.md) - Management contoare și citiri
- [Consumption Statistics](./13-consumption-statistics.md) - Statistici de consum
- [Tenant Rental Agreements](./11-tenant-rental-agreements.md) - Management contracte

