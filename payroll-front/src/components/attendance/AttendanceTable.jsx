import React, { useState, useCallback } from "react";
import { upsertRecord, deleteRecord } from "../../services/attendance/Attendanceservice";

/**
 * Editable attendance table for HR_MANAGER / ADMIN.
 * Read-only for EMPLOYEE_VIEWER.
 *
 * Props:
 *  - rows:        AttendanceRecordDto[]
 *  - year, month: number
 *  - canEdit:     boolean
 *  - onRefresh:   () => void
 */
export function AttendanceTable({ rows, year, month, canEdit, onRefresh }) {
  // local edits before save: { [employeeId]: { field: value } }
  const [pending, setPending] = useState({});
  const [saving, setSaving]   = useState({});

  const getValue = (row, field) => {
    const local = pending[row.employeeId]?.[field];
    if (local !== undefined) return local;
    const raw = row[field];
    return raw !== null && raw !== undefined ? String(raw) : "";
  };

  const handleChange = (employeeId, field, value) => {
    setPending(prev => ({
      ...prev,
      [employeeId]: { ...(prev[employeeId] ?? {}), [field]: value },
    }));
  };

  const handleBlur = useCallback(async (row) => {
    const changes = pending[row.employeeId];
    if (!changes || Object.keys(changes).length === 0) return;

    setSaving(prev => ({ ...prev, [row.employeeId]: true }));
    try {
      const toNum = v => (v === "" || v === undefined || v === null) ? null : Number(v);
      await upsertRecord({
        employeeId:      row.employeeId,
        year,
        month,
        absences:        toNum(getValue(row, "absences")),
        conges:          toNum(getValue(row, "conges")),
        hSupp75:         toNum(getValue(row, "hSupp75")),
        hSupp100:        toNum(getValue(row, "hSupp100")),
        hSupp25:         toNum(getValue(row, "hSupp25")),
        hNuit1:          toNum(getValue(row, "hNuit1")),
        hNuit2:          toNum(getValue(row, "hNuit2")),
        avances:         toNum(getValue(row, "avances")),
        jFeries:         toNum(getValue(row, "jFeries")),
        jfTravaille:     toNum(getValue(row, "jfTravaille")),
        hoursWorked:     toNum(getValue(row, "hoursWorked")),
        daysWorked:      toNum(getValue(row, "daysWorked")),
        advanceDeduction:toNum(getValue(row, "advanceDeduction")),
        regime:          getValue(row, "regime") || null,
        affectation:     getValue(row, "affectation") || null,
        service:         getValue(row, "service") || null,
        section:         getValue(row, "section") || null,
      });
      // clear pending for this employee after successful save
      setPending(prev => {
        const next = { ...prev };
        delete next[row.employeeId];
        return next;
      });
      onRefresh();
    } catch (err) {
      console.error("Failed to save attendance record", err);
    } finally {
      setSaving(prev => ({ ...prev, [row.employeeId]: false }));
    }
  }, [pending, year, month, onRefresh]);

  const handleDelete = async (row) => {
    if (!window.confirm(`Supprimer le pointage de ${row.employeeFullName} pour ce mois ?`)) return;
    try {
      await deleteRecord(row.employeeId, year, month);
      setPending(prev => {
        const next = { ...prev };
        delete next[row.employeeId];
        return next;
      });
      onRefresh();
    } catch (err) {
      console.error("Failed to delete record", err);
    }
  };

  const columns = [
    { key: "absences",    label: "ABSENCES",     cls: "" },
    { key: "conges",      label: "CONGÉS",       cls: "" },
    { key: "hSupp75",     label: "H.SUPP 75%",   cls: "th-supp75" },
    { key: "hSupp100",    label: "H.SUPP 100%",  cls: "th-supp100" },
    { key: "hSupp25",     label: "H.SUPP 25%",   cls: "th-supp25" },
    { key: "hNuit1",      label: "H. NUIT 1 (25%)", cls: "th-nuit1" },
    { key: "hNuit2",      label: "H. NUIT 2 (25%)", cls: "th-nuit2" },
    { key: "avances",     label: "AVANCES",      cls: "" },
    { key: "jFeries",     label: "J. FÉRIÉS",    cls: "" },
    { key: "jfTravaille", label: "J.F. TRAVAILLÉ", cls: "" },
  ];

  return (
    <div className="table-wrapper">
      <table className="attendance-table">
        <thead>
          <tr>
            <th style={{ width: 36 }}>#</th>
            <th className="col-left" style={{ minWidth: 160 }}>SALARIÉ</th>
            <th>N° JOURS</th>
            {columns.map(c => (
              <th key={c.key} className={c.cls}>{c.label}</th>
            ))}
            {canEdit && <th style={{ width: 40 }}></th>}
          </tr>
        </thead>

        <tbody>
          {rows.map((row, idx) => {
            const isSaving = saving[row.employeeId];
            return (
              <tr key={row.employeeId}>
                {/* Row number + add button (only for editors) */}
                <td className="row-index-cell">
                  <span className="row-index-cell__num">{idx + 1}</span>
                  {canEdit && (
                    <button
                      className="row-add-btn"
                      title="Saisir / modifier le pointage"
                      onClick={() => {
                        // focus first editable input in this row
                        document
                          .querySelector(`[data-emp="${row.employeeId}"] input`)
                          ?.focus();
                      }}
                    >
                      +
                    </button>
                  )}
                </td>

                {/* Employee name */}
                <td className="employee-cell" data-emp={row.employeeId}>
                  <span className="employee-cell__name">{row.employeeFullName}</span>
                  <span className="employee-cell__code">{row.employeeCode}</span>
                </td>

                {/* Working days (read-only) */}
                <td style={{ color: "var(--color-text-secondary)", fontWeight: 500 }}>
                  {row.nJours ?? "—"}
                </td>

                {/* Editable columns */}
                {columns.map(c => (
                  <td key={c.key}>
                    <input
                      className="cell-input"
                      type="number"
                      min="0"
                      step="0.5"
                      value={getValue(row, c.key)}
                      readOnly={!canEdit || isSaving}
                      onChange={e => canEdit && handleChange(row.employeeId, c.key, e.target.value)}
                      onBlur={() => canEdit && handleBlur(row)}
                      placeholder="0"
                      aria-label={`${c.label} — ${row.employeeFullName}`}
                    />
                  </td>
                ))}

                {/* Delete button */}
                {canEdit && (
                  <td>
                    {row.filled && (
                      <button
                        className="delete-btn"
                        onClick={() => handleDelete(row)}
                        title="Supprimer ce pointage"
                        aria-label={`Supprimer le pointage de ${row.employeeFullName}`}
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <polyline points="3 6 5 6 21 6"/>
                          <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
                          <path d="M10 11v6M14 11v6"/>
                          <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
                        </svg>
                      </button>
                    )}
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}