import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { MonthCard } from "../components/attendance/MonthCard";
import { getYearSummary } from "../services/attendance/Attendanceservice";
import "../styles/attendance/Attendance.css";

const CURRENT_YEAR = new Date().getFullYear();

/**
 * Overview page: 12-month grid for the selected year.
 * Matches "Image 1" in the spec.
 */
export default function PointageOverviewPage() {
  const [year, setYear]         = useState(CURRENT_YEAR);
  const [summaries, setSummaries] = useState([]);
  const [loading, setLoading]   = useState(true);
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getYearSummary(year);
      setSummaries(data);
    } catch (e) {
      console.error("Failed to load year summary", e);
    } finally {
      setLoading(false);
    }
  }, [year]);

  useEffect(() => { load(); }, [load]);

  const years = Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - 2 + i);

  return (
    <div className="pointage-page">
      {/* Header */}
      <div className="pointage-page__header">
        <div>
          <h1 className="pointage-page__title">Pointage</h1>
          <p className="pointage-page__subtitle">
            Sélectionnez <span>un mois</span> pour saisir ou consulter les pointages.
          </p>
        </div>

        <div className="year-select-wrapper">
          <label htmlFor="year-select">Année</label>
          <select
            id="year-select"
            className="year-select"
            value={year}
            onChange={e => setYear(Number(e.target.value))}
          >
            {years.map(y => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Legend */}
      <div className="legend-card">
        <p className="legend-card__title">Légende</p>
        <div className="legend-items">
          {[
            ["complet",  "Complet"],
            ["en-cours", "En cours"],
            ["cloture",  "Clôturé"],
            ["vide",     "Vide"],
          ].map(([key, label]) => (
            <div key={key} className="legend-item">
              <span className={`legend-dot legend-dot--${key}`} />
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Month grid */}
      {loading ? (
        <div className="table-loading">
          <div className="spinner" />
          Chargement…
        </div>
      ) : (
        <div className="months-grid">
          {summaries.map(s => (
            <MonthCard
              key={s.month}
              summary={s}
              onClick={() => navigate(`/pointage/${year}/${s.month}`)}
            />
          ))}
        </div>
      )}
    </div>
  );
}