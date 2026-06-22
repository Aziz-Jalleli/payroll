from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import uvicorn

app = FastAPI(title="Payroll CSV Mapping Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

print("Loading sentence-transformers model...")
model = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")
print("Model loaded.")

# ─────────────────────────────────────────────
# Canonical schema — FR / EN / AR aliases
# Covers both simple CSVs and complex payroll exports
# ─────────────────────────────────────────────
CANONICAL_SCHEMA = {
    # ── Identity ──────────────────────────────
    "employee_id": [
        "MATR N", "Matricule", "Matricule employé", "Numéro employé",
        "MATR", "Mat", "N matricule", "Employee ID", "Staff number",
        "رقم الموظف",
    ],
    "first_name": [
        "PRENOM", "Prénom", "First name", "Given name",
        "الاسم الأول",
    ],
    "last_name": [
        "NOM", "Nom", "Nom de famille", "Last name", "Surname",
        "اسم العائلة",
    ],
    "full_name": [
        "Nom complet", "Nom et prénom", "Prénom Nom", "Nom de l'employé",
        "Full name", "Employee name", "الاسم الكامل",
    ],
    "national_id": [
        "CIN", "Carte identité nationale", "Numéro CIN",
        "National ID", "رقم بطاقة التعريف",
    ],
    "email": [
        "Adresse email", "Email", "Adresse électronique", "Courriel",
        "Email address", "البريد الإلكتروني",
    ],
    "phone": [
        "Téléphone", "Numéro de téléphone", "Mobile", "GSM",
        "Phone number", "رقم الهاتف",
    ],
    "hire_date": [
        "Date d'embauche", "Date d'entrée", "Date de recrutement",
        "Hire date", "Start date", "تاريخ التوظيف",
    ],

    # ── Organisation ──────────────────────────
    "department": [
        "DEPARTEMENT", "Département", "Service", "Direction",
        "Department", "القسم",
    ],
    "service": [
        "SERVICE", "Sous-service", "Sub-service", "Unit",
        "الوحدة الفرعية",
    ],
    "section": [
        "SECTION", "Section", "Team", "Équipe",
        "الفريق",
    ],
    "affectation": [
        "AFFECTATION", "Affectation", "Assignment", "Site",
        "موقع العمل",
    ],
    "position": [
        "Poste", "Fonction", "Titre", "Grade",
        "Position", "Job title", "المنصب",
    ],
    "regime": [
        "REGIME", "Régime", "Regime", "Code régime", "Type contrat",
        "نوع العقد",
    ],

    # ── Salary ────────────────────────────────
    "gross_salary": [
        "Salaire brut", "Rémunération brute", "Salaire brut mensuel",
        "Gross salary", "Gross pay", "الراتب الإجمالي",
    ],
    "net_salary": [
        "Salaire net", "Net à payer", "Net mensuel",
        "Net salary", "الراتب الصافي",
    ],
    "base_salary": [
        "Salaire de base", "Traitement de base",
        "Base salary", "الراتب الأساسي",
    ],

    # ── Bonuses & Premiums ────────────────────
    "bonus_gross": [
        "Prime Brut", "Prime brute", "Montant prime", "Prime mensuelle",
        "Gross bonus", "Bonus amount", "العلاوة الإجمالية",
    ],
    "bonus_rappel": [
        "Rappel Prime en DT", "Rappel prime", "Rappel", "Prime rappel",
        "Bonus arrears", "Back pay bonus", "استرداد العلاوة",
    ],
    "gratification_note": [
        "NOTE GRATIFICATION", "Note gratification", "Gratification",
        "Performance note", "ملاحظة المكافأة",
    ],

    # ── Attendance ────────────────────────────
    "hours_worked": [
        "Nb heures de présence base paie", "Heures de présence",
        "Heures travaillées", "Nb heures", "Hours worked", "ساعات العمل",
    ],
    "days_worked": [
        "Nb de jours de présence", "Jours de présence",
        "Jours travaillés", "Days worked", "أيام العمل",
    ],
    "public_holidays": [
        "Jour férié", "Jours fériés", "Férié", "Jours fériés travaillés",
        "Public holidays", "أيام العطل الرسمية",
    ],

    # ── Leave ─────────────────────────────────
    "leave_days_base": [
        "NB jours base calcul congés", "Jours base congés",
        "Base jours congé", "Leave base days", "أيام أساس الإجازة",
    ],
    "leave_pay": [
        "Congés prix", "Indemnité congés", "Paiement congés",
        "Leave pay", "Leave amount", "بدل الإجازة",
    ],

    # ── Deductions ────────────────────────────
    "advance_deduction": [
        "Retenue avance", "Avance sur salaire", "Retenue",
        "Advance deduction", "Salary advance", "خصم السلفة",
    ],

    # ── Family ────────────────────────────────
    "marital_status": [
        "Situation familiale", "État civil",
        "Marital status", "الحالة العائلية",
    ],
    "number_of_children": [
        "Nombre d'enfants", "Enfants", "Nb enfants",
        "Number of children", "عدد الأطفال",
    ],
}

# ─────────────────────────────────────────────
# Fields that should be IGNORED (totals rows,
# empty columns, unknown footers)
# ─────────────────────────────────────────────
IGNORE_PATTERNS = ["", " ", '""']

# Pre-compute embeddings
print("Pre-computing schema embeddings...")
SCHEMA_KEYS = list(CANONICAL_SCHEMA.keys())
SCHEMA_ALIAS_EMBEDDINGS = {
    field: model.encode(aliases)
    for field, aliases in CANONICAL_SCHEMA.items()
}
print(f"Ready. {len(SCHEMA_KEYS)} fields indexed.")

# Thresholds
AUTO_MAP_THRESHOLD    = 0.75
SUGGEST_THRESHOLD     = 0.50   # lower = more ambiguous suggestions shown
AMBIGUOUS_GAP         = 0.08   # if top-2 scores are within this gap → ambiguous


class MappingRequest(BaseModel):
    headers: list[str]


class ColumnMapping(BaseModel):
    mappedTo: str | None
    confidence: float
    status: str        # AUTO_MAPPED | NEEDS_CONFIRMATION | AMBIGUOUS | UNMAPPED | IGNORED
    alternatives: list[dict] = []   # top-3 alternatives shown for AMBIGUOUS/NEEDS_CONFIRMATION


class MappingResponse(BaseModel):
    mappings: dict[str, ColumnMapping]
    unmappedColumns: list[str]
    ambiguousColumns: list[str]
    missingRequiredFields: list[str]
    specialHandling: list[str]   # e.g. ["NOM+PRENOM will be merged into full_name"]


@app.post("/map-columns", response_model=MappingResponse)
def map_columns(request: MappingRequest):
    if not request.headers:
        return MappingResponse(
            mappings={}, unmappedColumns=[], ambiguousColumns=[],
            missingRequiredFields=[], specialHandling=[]
        )

    # Clean headers — strip surrounding quotes and whitespace
    cleaned_headers = [h.strip().strip('"').strip() for h in request.headers]

    # Detect ignored columns (empty or unnamed)
    ignored = {h for h in cleaned_headers if h in IGNORE_PATTERNS or h == ""}

    # Embed all non-ignored headers
    active_headers = [h for h in cleaned_headers if h not in ignored]
    if not active_headers:
        return MappingResponse(
            mappings={}, unmappedColumns=[], ambiguousColumns=[],
            missingRequiredFields=[], specialHandling=[]
        )

    header_embeddings = model.encode(active_headers)

    # Build score matrix (num_headers x num_fields)
    score_matrix = np.zeros((len(active_headers), len(SCHEMA_KEYS)))
    for field_idx, field in enumerate(SCHEMA_KEYS):
        alias_embs = SCHEMA_ALIAS_EMBEDDINGS[field]
        sims = cosine_similarity(header_embeddings, alias_embs)
        score_matrix[:, field_idx] = sims.max(axis=1)

    mappings: dict[str, ColumnMapping] = {}
    unmapped: list[str] = []
    ambiguous: list[str] = []
    used_fields: set[str] = set()
    special_handling: list[str] = []

    # Add ignored columns to mappings
    for h in cleaned_headers:
        if h in ignored:
            mappings[h] = ColumnMapping(
                mappedTo=None, confidence=0.0,
                status="IGNORED", alternatives=[]
            )

    for i, header in enumerate(active_headers):
        scores = score_matrix[i]
        sorted_indices = np.argsort(scores)[::-1]

        # Top-5 candidates (for alternatives display)
        top5 = [
            {"field": SCHEMA_KEYS[idx], "confidence": round(float(scores[idx]), 4)}
            for idx in sorted_indices[:5]
        ]

        # Best unused candidate
        best_score = 0.0
        best_field = None
        second_score = 0.0

        for rank, idx in enumerate(sorted_indices):
            candidate = SCHEMA_KEYS[idx]
            if candidate not in used_fields:
                if best_field is None:
                    best_score = float(scores[idx])
                    best_field = candidate
                elif second_score == 0.0:
                    second_score = float(scores[idx])
                    break

        if best_field is None or best_score < SUGGEST_THRESHOLD:
            unmapped.append(header)
            mappings[header] = ColumnMapping(
                mappedTo=None,
                confidence=round(best_score, 4),
                status="UNMAPPED",
                alternatives=top5[:3],
            )
            continue

        # Check if ambiguous: top-2 scores are very close
        is_ambiguous = (
            second_score > 0
            and (best_score - second_score) < AMBIGUOUS_GAP
            and best_score < AUTO_MAP_THRESHOLD
        )

        if is_ambiguous:
            ambiguous.append(header)
            mappings[header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="AMBIGUOUS",
                alternatives=top5[:3],
            )
        elif best_score >= AUTO_MAP_THRESHOLD:
            used_fields.add(best_field)
            mappings[header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="AUTO_MAPPED",
                alternatives=[],
            )
        else:
            # NEEDS_CONFIRMATION — show alternatives so user can pick
            mappings[header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="NEEDS_CONFIRMATION",
                alternatives=top5[:3],
            )

    # ── Special handling detection ────────────────────────────
    has_nom    = any(h == "NOM"    for h in active_headers)
    has_prenom = any(h == "PRENOM" for h in active_headers)
    has_full   = any(mappings.get(h, ColumnMapping(mappedTo=None,confidence=0,status="",alternatives=[])).mappedTo == "full_name"
                     for h in active_headers)

    if has_nom and has_prenom and not has_full:
        special_handling.append(
            "NOM + PRENOM will be automatically merged into full_name during import"
        )

    # ── Required fields check ─────────────────────────────────
    required = {"full_name", "national_id"}
    mapped_fields = {m.mappedTo for m in mappings.values() if m.mappedTo}

    # full_name can be satisfied by NOM+PRENOM merge
    if has_nom and has_prenom:
        mapped_fields.add("full_name")

    missing_required = list(required - mapped_fields)

    return MappingResponse(
        mappings=mappings,
        unmappedColumns=unmapped,
        ambiguousColumns=ambiguous,
        missingRequiredFields=missing_required,
        specialHandling=special_handling,
    )


@app.get("/health")
def health():
    return {"status": "ok", "model": "paraphrase-multilingual-MiniLM-L12-v2"}


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
