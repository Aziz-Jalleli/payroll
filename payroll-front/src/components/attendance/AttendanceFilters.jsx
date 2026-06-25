import React from "react";

/**
 * Filter bar for the attendance detail page.
 * In read-only mode (canEdit=false) only a single name search is shown.
 *
 * Props:
 *  - filters:   { name, email, department }
 *  - onChange:  (filters) => void
 *  - canEdit:   boolean  (HR/ADMIN see all filters; viewers see none or just name)
 *  - totalCount: number
 */
export function AttendanceFilters({ filters, onChange, canEdit, totalCount }) {
  const set = (key, value) => onChange({ ...filters, [key]: value });

  return (
    <div className="filters-card">
      <h2 className="filters-card__title">Filtres et saisie</h2>

      <div className="filters-row">
        {/* Name/matricule search — available to everyone */}
        <div className="search-input-wrapper">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            className="search-input"
            type="text"
            placeholder="Recherche par nom, prénom, matricule..."
            value={filters.name ?? ""}
            onChange={e => set("name", e.target.value)}
            aria-label="Rechercher un salarié"
          />
        </div>

        {/* Extra filters — HR/ADMIN only */}
        {canEdit && (
          <>
            <input
              className="search-input"
              style={{ flex: "0 0 180px", paddingLeft: "var(--space-3)" }}
              type="text"
              placeholder="Email..."
              value={filters.email ?? ""}
              onChange={e => set("email", e.target.value)}
              aria-label="Filtrer par email"
            />
            <input
              className="search-input"
              style={{ flex: "0 0 160px", paddingLeft: "var(--space-3)" }}
              type="text"
              placeholder="Département..."
              value={filters.department ?? ""}
              onChange={e => set("department", e.target.value)}
              aria-label="Filtrer par département"
            />
          </>
        )}

        <span className="employees-count">{totalCount} salarié(s)</span>
      </div>
    </div>
  );
}