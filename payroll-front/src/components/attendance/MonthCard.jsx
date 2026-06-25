import React from "react";

const MONTH_NAMES = [
  "Janvier","Février","Mars","Avril","Mai","Juin",
  "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
];

/**
 * Single month card shown in the Pointage overview grid.
 * @param {{ summary: MonthSummaryDto, onClick: () => void }} props
 */
export function MonthCard({ summary, onClick }) {
  const { month, totalEmployees, filledCount, status, currentMonth } = summary;
  const name = MONTH_NAMES[month - 1];
  const statusKey = status.toLowerCase().replace("_", "-"); // e.g. "en-cours"
  const pct = totalEmployees > 0 ? Math.round((filledCount / totalEmployees) * 100) : 0;

  const statusLabel = {
    "complet":  "COMPLET",
    "en-cours": "EN COURS",
    "cloture":  "CLÔTURÉ",
    "vide":     "VIDE",
  }[statusKey] ?? status;

  return (
    <div
      className={`month-card${currentMonth ? " month-card--current" : ""}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={e => e.key === "Enter" && onClick()}
      aria-label={`${name} — ${statusLabel}`}
    >
      {currentMonth && (
        <span className="month-card__badge-current">CE MOIS</span>
      )}

      <div className="month-card__name">
        {/* calendar icon */}
        <svg className="month-card__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
          <line x1="16" y1="2" x2="16" y2="6"/>
          <line x1="8"  y1="2" x2="8"  y2="6"/>
          <line x1="3"  y1="10" x2="21" y2="10"/>
        </svg>
        {name}
      </div>

      <div className="month-card__meta">
        <span>{filledCount}/{totalEmployees} salarié{totalEmployees !== 1 ? "s" : ""}</span>
        <span className={`status-pill status-pill--${statusKey}`}>{statusLabel}</span>
      </div>

      <div className="month-card__progress-bar">
        <div
          className={`month-card__progress-fill month-card__progress-fill--${statusKey}`}
          style={{ width: `${pct}%` }}
        />
      </div>

      {currentMonth && (
        <div style={{ marginTop: "8px", display: "flex", alignItems: "center", gap: "6px" }}>
          {/* checkmark icon when complete */}
          {status === "COMPLET" && (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2E7D5B" strokeWidth="2.5">
              <path d="M20 6L9 17l-5-5"/>
            </svg>
          )}
        </div>
      )}
    </div>
  );
}