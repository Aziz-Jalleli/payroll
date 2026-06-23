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

CANONICAL_SCHEMA = {
    "employee_id": [
        "Matricule", "MATR N", "Matricule employé", "ID employé", "Numéro employé",
        "Employee ID", "Staff number", "رقم الموظف",
    ],
    "first_name": [
        "PRENOM", "Prénom", "First name", "Given name", "الاسم الأول",
    ],
    "last_name": [
        "NOM", "Nom de famille", "Last name", "Surname", "اسم العائلة",
    ],
    "full_name": [
        "Nom complet", "Nom et prénom", "Full name", "Employee name", "الاسم الكامل",
    ],
    "national_id": [
        "CIN", "Carte identité nationale", "Numéro CIN", "National ID", "رقم بطاقة التعريف",
    ],
    "email": [
        "Adresse email", "Email", "Adresse électronique", "Email address", "البريد الإلكتروني",
    ],
    "phone": [
        "Téléphone", "Numéro de téléphone", "Mobile", "GSM", "Phone number", "رقم الهاتف",
    ],
    "hire_date": [
        "Date d'embauche", "Date d'entrée", "Date de recrutement",
        "Hire date", "Start date", "تاريخ التوظيف",
    ],
    "affectation": [
        "AFFECTATION", "Affectation", "Affectation service", "Assignment", "التعيين",
    ],
    "regime": [
        "REGIME", "Régime", "Code régime", "Régime de travail", "Regime code", "نظام العمل",
    ],
    "department": [
        "DEPARTEMENT", "Département", "Department", "Division", "القسم",
    ],
    "service": [
        "SERVICE", "Service", "Unit", "Unité", "الوحدة",
    ],
    "section": [
        "SECTION", "Section", "Section de travail", "Work section", "القطاع",
    ],
    "position": [
        "Poste", "Fonction", "Job title", "Position", "المنصب",
    ],
    "gross_salary": [
        "Salaire brut", "Salaire brut mensuel", "Gross salary", "الراتب الإجمالي",
    ],
    "net_salary": [
        "Salaire net", "Net à payer", "Net salary", "الراتب الصافي",
    ],
    "base_salary": [
        "Salaire de base", "Traitement de base", "Base salary", "الراتب الأساسي",
    ],
    "bonus_gross": [
        "Prime Brut", "Prime brute", "Gross bonus", "Bonus brut", "المكافأة الإجمالية",
    ],
    "bonus_rappel": [
        "Rappel Prime en DT", "Rappel prime", "Bonus rappel", "Prime rappel", "استرجاع المكافأة",
    ],
    "gratification_note": [
        "NOTE GRATIFICATION", "Note gratification", "Gratification", "Gratification note", "ملاحظة المكافأة",
    ],
    "hours_worked": [
        "Nb heures de présence base paie", "Heures de présence", "Heures travaillées",
        "Hours worked", "Presence hours", "ساعات العمل",
    ],
    "days_worked": [
        "Nb de jours de présence", "Jours de présence", "Jours travaillés",
        "Days worked", "Working days", "أيام العمل",
    ],
    "advance_deduction": [
        "Retenue avance", "Avance sur salaire", "Advance deduction",
        "Salary advance", "خصم السلفة",
    ],
    "leave_days_base": [
        "NB jours base calcul congés", "Jours base congés", "Leave base days",
        "Congés base", "أيام قاعدة الإجازة",
    ],
    "leave_pay": [
        "Congés prix", "Prix des congés", "Leave pay", "Vacation pay", "أجر الإجازة",
    ],
    "public_holidays": [
        "Jour férié", "Jours fériés", "Public holidays", "Fêtes nationales", "أيام العطل الرسمية",
    ],
    "marital_status": [
        "Situation familiale", "État civil", "Marital status", "الحالة العائلية",
    ],
    "number_of_children": [
        "Nombre d'enfants", "Enfants", "Number of children", "عدد الأطفال",
    ],
}

print("Pre-computing schema embeddings...")
SCHEMA_KEYS = list(CANONICAL_SCHEMA.keys())
SCHEMA_ALIAS_EMBEDDINGS = {
    field: model.encode(aliases)
    for field, aliases in CANONICAL_SCHEMA.items()
}
print(f"Ready. {len(SCHEMA_KEYS)} fields indexed.")

AUTO_MAP_THRESHOLD    = 0.75
SUGGEST_THRESHOLD     = 0.55
AMBIGUOUS_GAP         = 0.08   # if top 2 scores are within this gap → ambiguous
TOP_SUGGESTIONS       = 3      # how many suggestions to return for ambiguous columns


class MappingRequest(BaseModel):
    headers: list[str]


class SuggestionOption(BaseModel):
    field: str
    confidence: float


class ColumnMapping(BaseModel):
    mappedTo: str | None
    confidence: float
    status: str          # AUTO_MAPPED | NEEDS_CONFIRMATION | AMBIGUOUS | UNMAPPED
    suggestions: list[SuggestionOption] = []   # top N options for ambiguous/confirmation


class MappingResponse(BaseModel):
    mappings: dict[str, ColumnMapping]
    unmappedColumns: list[str]
    missingRequiredFields: list[str]
    specialHandling: list[str]   # hints like "NOM+PRENOM will be merged into full_name"


@app.post("/map-columns", response_model=MappingResponse)
def map_columns(request: MappingRequest):
    if not request.headers:
        return MappingResponse(
            mappings={}, unmappedColumns=[],
            missingRequiredFields=[], specialHandling=[]
        )

    # Clean headers: strip quotes and whitespace
    cleaned_headers = [h.strip().strip('"') for h in request.headers]
    # Filter out empty headers
    valid_pairs = [(orig, clean) for orig, clean in zip(request.headers, cleaned_headers) if clean]

    header_embeddings = model.encode([c for _, c in valid_pairs])

    # Build score matrix (num_headers x num_fields)
    score_matrix = np.zeros((len(valid_pairs), len(SCHEMA_KEYS)))
    for field_idx, field in enumerate(SCHEMA_KEYS):
        alias_emb = SCHEMA_ALIAS_EMBEDDINGS[field]
        sims = cosine_similarity(header_embeddings, alias_emb)
        score_matrix[:, field_idx] = sims.max(axis=1)

    mappings: dict[str, ColumnMapping] = {}
    unmapped: list[str] = []
    used_fields: set[str] = set()
    special_handling: list[str] = []

    # Detect NOM + PRENOM pattern → special merge hint
    header_names = [c for _, c in valid_pairs]
    has_nom    = any(h in ("NOM", "Nom", "nom") for h in header_names)
    has_prenom = any(h in ("PRENOM", "Prénom", "prenom") for h in header_names)
    if has_nom and has_prenom:
        special_handling.append("NOM + PRENOM will be automatically merged into full_name during import")

    for i, (orig_header, clean_header) in enumerate(valid_pairs):
        scores = score_matrix[i]
        sorted_indices = np.argsort(scores)[::-1]

        # Build top-N suggestions (skip already used)
        top_suggestions: list[SuggestionOption] = []
        for idx in sorted_indices:
            if len(top_suggestions) >= TOP_SUGGESTIONS:
                break
            candidate = SCHEMA_KEYS[idx]
            top_suggestions.append(
                SuggestionOption(field=candidate, confidence=round(float(scores[idx]), 4))
            )

        # Best available (not used)
        best_field = None
        best_score = 0.0
        for idx in sorted_indices:
            candidate = SCHEMA_KEYS[idx]
            if candidate not in used_fields:
                best_field = candidate
                best_score = float(scores[idx])
                break

        if best_field is None or best_score < SUGGEST_THRESHOLD:
            unmapped.append(orig_header)
            mappings[orig_header] = ColumnMapping(
                mappedTo=None,
                confidence=round(best_score, 4),
                status="UNMAPPED",
                suggestions=top_suggestions,
            )
            continue

        # Check ambiguity: top 2 available scores within AMBIGUOUS_GAP
        available_scores = [
            float(scores[idx]) for idx in sorted_indices
            if SCHEMA_KEYS[idx] not in used_fields
        ]
        is_ambiguous = (
            len(available_scores) >= 2
            and (available_scores[0] - available_scores[1]) < AMBIGUOUS_GAP
            and available_scores[0] < AUTO_MAP_THRESHOLD
        )

        if best_score >= AUTO_MAP_THRESHOLD:
            used_fields.add(best_field)
            mappings[orig_header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="AUTO_MAPPED",
                suggestions=top_suggestions,
            )
        elif is_ambiguous:
            # Don't lock in a field — let user choose
            mappings[orig_header] = ColumnMapping(
                mappedTo=best_field,      # pre-select best guess
                confidence=round(best_score, 4),
                status="AMBIGUOUS",
                suggestions=top_suggestions,
            )
        else:
            mappings[orig_header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="NEEDS_CONFIRMATION",
                suggestions=top_suggestions,
            )

    # Add empty-header placeholders
    for h in request.headers:
        clean = h.strip().strip('"')
        if not clean and h not in mappings:
            mappings[h] = ColumnMapping(
                mappedTo=None, confidence=0.0,
                status="UNMAPPED", suggestions=[]
            )

    required = {"full_name", "national_id"}
    # full_name satisfied if NOM+PRENOM both present
    mapped_fields = {m.mappedTo for m in mappings.values() if m.mappedTo}
    if has_nom and has_prenom:
        mapped_fields.add("full_name")

    missing_required = list(required - mapped_fields)

    return MappingResponse(
        mappings=mappings,
        unmappedColumns=unmapped,
        missingRequiredFields=missing_required,
        specialHandling=special_handling,
    )


@app.get("/health")
def health():
    return {"status": "ok", "model": "paraphrase-multilingual-MiniLM-L12-v2"}


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)