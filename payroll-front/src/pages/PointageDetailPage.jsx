import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { AttendanceTable }   from "../components/attendance/AttendanceTable";
import { AttendanceFilters } from "../components/attendance/AttendanceFilters";
import { getMonthDetail }    from "../services/attendance/Attendanceservice";
import { getCurrentUser }  from "../services/authService";
import "../styles/attendance/Attendance.css";

const MONTH_NAMES = [
  "Janvier","Février","Mars","Avril","Mai","Juin",
  "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
];

/**
 * Detail page for a single month's attendance.
 *
 * Auth: calls /api/me on mount using the same apiClient (withCredentials: true)
 * that every other page uses. Spring reads the JSESSIONID cookie, resolves
 * the CustomUserPrincipal from the Redis session, and CurrentUserController
 * returns { roles: string[], permissions: string[], ... }.
 *
 * canEdit is derived from roles here purely for UI rendering (show/hide
 * inputs, delete button, filter fields). The actual server-side enforcement
 * is done by @RequirePermission on the controller + PermissionAspect.
 */
export default function PointageDetailPage() {
  const { year: yearStr, month: monthStr } = useParams();
  const year  = Number(yearStr);
  const month = Number(monthStr);
  const navigate = useNavigate();

  const [currentUser, setCurrentUser] = useState(null);
  const [userLoading, setUserLoading] = useState(true);

  const [rows,    setRows]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ name: "", email: "", department: "" });

  // ── Load current user from /api/me (Redis session → CustomUserPrincipal) ──
  useEffect(() => {
    getCurrentUser()
      .then(user => {
        if (!user) navigate("/login");
        else setCurrentUser(user);
      })
      .catch(() => navigate("/login"))
      .finally(() => setUserLoading(false));
  }, [navigate]);

  // roles comes back as a Set serialized to an array by Jackson
  const roles   = currentUser?.roles ?? [];
  const canEdit = roles.includes("ADMIN") || roles.includes("HR_MANAGER");

  // ── Load attendance rows ──────────────────────────────────────────────────
  const load = useCallback(async () => {
    if (userLoading) return; // wait until we know the role before fetching
    setLoading(true);
    try {
      const data = await getMonthDetail(year, month, filters);
      setRows(data);
    } catch (e) {
      console.error("Failed to load month detail", e);
    } finally {
      setLoading(false);
    }
  }, [year, month, filters, userLoading]);

  useEffect(() => { load(); }, [load]);

  const monthName = MONTH_NAMES[month - 1] ?? "";

  // ── Derived stats ─────────────────────────────────────────────────────────
  const totalDays     = rows.reduce((s, r) => s + (r.nJours ?? 0), 0);
  const filledCount   = rows.filter(r => r.filled).length;
  const unfilledCount = rows.length - filledCount;
  const totalHours    = rows.reduce((s, r) => s + Number(r.hoursWorked ?? 0), 0);

  // ── Status badge ──────────────────────────────────────────────────────────
  let statusLabel = "EN COURS", statusKey = "en-cours";
  if (filledCount === rows.length && rows.length > 0) { statusLabel = "COMPLET"; statusKey = "complet"; }
  else if (filledCount === 0)                          { statusLabel = "VIDE";    statusKey = "vide"; }

  if (userLoading) {
    return (
      <div className="attendance-detail">
        <div className="table-loading"><div className="spinner" />Chargement…</div>
      </div>
    );
  }

  return (
    <div className="attendance-detail">
      {/* Header */}
      <div className="attendance-detail__header">
        <div>
          <h1 className="attendance-detail__title">Pointage — {monthName} {year}</h1>
          <p className="attendance-detail__subtitle">
            {canEdit ? "Mois ouvert pour la saisie." : "Consultation uniquement."}
          </p>
        </div>

        <div className="attendance-detail__actions">
          <span
            className={`status-pill status-pill--${statusKey}`}
            style={{ fontSize: "var(--fs-sm)", padding: "4px 14px" }}
          >
            {statusLabel}
          </span>
          <button className="btn btn--ghost" onClick={() => navigate(-1)}>
            ← Liste des mois
          </button>
          <button className="btn btn--primary" onClick={load}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <polyline points="23 4 23 10 17 10"/>
              <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/>
            </svg>
            Actualiser
          </button>
        </div>
      </div>

      {/* Filters */}
      <AttendanceFilters
        filters={filters}
        onChange={setFilters}
        canEdit={canEdit}
        totalCount={rows.length}
      />

      {/* Stats bar */}
      <div className="stats-bar">
        <div className="stat-card stat-card--blue">
          <p className="stat-card__label">Total heures (HS + H. nuit)</p>
          <p className="stat-card__value">{totalHours > 0 ? `${totalHours} Heures` : "0 Heures"}</p>
        </div>
        <div className="stat-card stat-card--purple">
          <p className="stat-card__label">Nombre jours</p>
          <p className="stat-card__value">{totalDays} Jours</p>
        </div>
        <div className="stat-card stat-card--green">
          <p className="stat-card__label">Salariés saisis</p>
          <p className="stat-card__value">{filledCount}</p>
        </div>
        <div className="stat-card stat-card--red">
          <p className="stat-card__label">Salariés non saisis</p>
          <p className="stat-card__value">{unfilledCount}</p>
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="table-loading">
          <div className="spinner" />Chargement des pointages…
        </div>
      ) : rows.length === 0 ? (
        <div className="table-empty">Aucun salarié trouvé pour ces filtres.</div>
      ) : (
        <AttendanceTable
          rows={rows}
          year={year}
          month={month}
          canEdit={canEdit}
          onRefresh={load}
        />
      )}
    </div>
  );
}