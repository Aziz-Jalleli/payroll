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

# Each field maps to a LIST of representative phrases in FR/EN/AR
# We embed all aliases and take the MAX similarity score per field
CANONICAL_SCHEMA = {
    "employee_id": [
        "Matricule", "Matricule employé", "ID employé", "Numéro employé",
        "Employee ID", "Staff number", "Employee number", "رقم الموظف",
    ],
    "full_name": [
        "Nom complet", "Nom et prénom", "Prénom Nom", "Nom de l'employé",
        "Full name", "Employee name", "الاسم الكامل",
    ],
    "national_id": [
        "CIN", "Carte identité nationale", "Numéro CIN", "Identité nationale",
        "National ID", "Identity number", "رقم بطاقة التعريف",
    ],
    "email": [
        "Adresse email", "Email", "Adresse électronique", "Courriel",
        "Email address", "البريد الإلكتروني",
    ],
    "phone": [
        "Téléphone", "Numéro de téléphone", "Mobile", "GSM", "Portable",
        "Phone number", "رقم الهاتف",
    ],
    "hire_date": [
        "Date d'embauche", "Date d'entrée", "Date de recrutement", "Date début",
        "Hire date", "Start date", "تاريخ التوظيف",
    ],
    "department": [
        "Département", "Service", "Direction", "Division",
        "Department", "Team", "القسم",
    ],
    "position": [
        "Poste", "Fonction", "Titre", "Grade", "Intitulé du poste",
        "Position", "Job title", "المنصب",
    ],
    "gross_salary": [
        "Salaire brut", "Rémunération brute", "Salaire brut mensuel", "Brut",
        "Gross salary", "Gross pay", "الراتب الإجمالي",
    ],
    "net_salary": [
        "Salaire net", "Net à payer", "Net mensuel",
        "Net salary", "Net pay", "الراتب الصافي",
    ],
    "marital_status": [
        "Situation familiale", "État civil", "Statut marital",
        "Marital status", "Civil status", "الحالة العائلية",
    ],
    "number_of_children": [
        "Nombre d'enfants", "Enfants", "Enfants à charge", "Nb enfants",
        "Number of children", "Children", "عدد الأطفال",
    ],
    "base_salary": [
        "Salaire de base", "Traitement de base",
        "Base salary", "Basic salary", "الراتب الأساسي",
    ],
}

# Pre-compute embeddings for every alias at startup
print("Pre-computing schema embeddings...")
SCHEMA_KEYS = list(CANONICAL_SCHEMA.keys())

# For each field: embed all aliases, store as matrix
SCHEMA_ALIAS_EMBEDDINGS = {}
for field, aliases in CANONICAL_SCHEMA.items():
    SCHEMA_ALIAS_EMBEDDINGS[field] = model.encode(aliases)

print(f"Ready. {len(SCHEMA_KEYS)} fields indexed.")

AUTO_MAP_THRESHOLD = 0.75   # lowered — short French terms score lower
SUGGEST_THRESHOLD  = 0.55


class MappingRequest(BaseModel):
    headers: list[str]


class ColumnMapping(BaseModel):
    mappedTo: str | None
    confidence: float
    status: str


class MappingResponse(BaseModel):
    mappings: dict[str, ColumnMapping]
    unmappedColumns: list[str]
    missingRequiredFields: list[str]


@app.post("/map-columns", response_model=MappingResponse)
def map_columns(request: MappingRequest):
    if not request.headers:
        return MappingResponse(mappings={}, unmappedColumns=[], missingRequiredFields=[])

    # Embed CSV headers
    header_embeddings = model.encode(request.headers)

    mappings: dict[str, ColumnMapping] = {}
    unmapped: list[str] = []
    used_fields: set[str] = set()

    # Build a score matrix: (num_headers x num_fields)
    # For each field take max similarity across all its aliases
    score_matrix = np.zeros((len(request.headers), len(SCHEMA_KEYS)))

    for field_idx, field in enumerate(SCHEMA_KEYS):
        alias_embeddings = SCHEMA_ALIAS_EMBEDDINGS[field]
        # cosine_similarity returns (num_headers x num_aliases)
        sims = cosine_similarity(header_embeddings, alias_embeddings)
        # Take max across aliases for each header
        score_matrix[:, field_idx] = sims.max(axis=1)

    for i, header in enumerate(request.headers):
        scores = score_matrix[i]

        # Sort fields by score descending, skip already-used ones
        sorted_indices = np.argsort(scores)[::-1]
        best_score = 0.0
        best_field = None

        for idx in sorted_indices:
            candidate = SCHEMA_KEYS[idx]
            if candidate not in used_fields:
                best_score = float(scores[idx])
                best_field = candidate
                break

        if best_field is None:
            unmapped.append(header)
            mappings[header] = ColumnMapping(mappedTo=None, confidence=0.0, status="UNMAPPED")
            continue

        if best_score >= AUTO_MAP_THRESHOLD:
            used_fields.add(best_field)
            mappings[header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="AUTO_MAPPED",
            )
        elif best_score >= SUGGEST_THRESHOLD:
            mappings[header] = ColumnMapping(
                mappedTo=best_field,
                confidence=round(best_score, 4),
                status="NEEDS_CONFIRMATION",
            )
        else:
            unmapped.append(header)
            mappings[header] = ColumnMapping(
                mappedTo=None,
                confidence=round(best_score, 4),
                status="UNMAPPED",
            )

    required = {"full_name", "national_id"}
    mapped_fields = {m.mappedTo for m in mappings.values() if m.mappedTo}
    missing_required = list(required - mapped_fields)

    return MappingResponse(
        mappings=mappings,
        unmappedColumns=unmapped,
        missingRequiredFields=missing_required,
    )


@app.get("/health")
def health():
    return {"status": "ok", "model": "paraphrase-multilingual-MiniLM-L12-v2"}


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)