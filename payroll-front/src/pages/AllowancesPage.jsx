// src/pages/AllowancesPage.jsx
import React, { useCallback, useEffect, useState } from 'react';
import { AllowanceModal }    from '../components/allowance/AllowanceModal';
import { DeleteConfirmModal } from '../components/allowance/DeleteConfirmModal';
import {
  fetchAllowances,
  createAllowance,
  updateAllowance,
  deleteAllowance,
} from '../services/allowance/allowanceService';
import { fetchEmployees } from '../services/employee/listemployees';
import Toast from '../components/usermanagement/Toast';

// ─── Constants ────────────────────────────────────────────────────────────────

const TYPE_LABELS = {
  TRANSPORT:      'Transport',
  HOUSING:        'Logement',
  MEAL:           'Panier / Repas',
  PHONE:          'Téléphone',
  REPRESENTATION: 'Représentation',
  OTHER:          'Autre',
};

const TYPE_COLORS = {
  TRANSPORT:      { bg: '#EBF5FF', color: '#1D6FA4' },
  HOUSING:        { bg: '#F0F4FF', color: '#3B4AC4' },
  MEAL:           { bg: '#FFF7E6', color: '#B47B12' },
  PHONE:          { bg: '#F3EFFF', color: '#6B3FA0' },
  REPRESENTATION: { bg: '#E6F9F4', color: '#1A7E5A' },
  OTHER:          { bg: '#F5F5F5', color: '#555' },
};

// ─── Sub-components ───────────────────────────────────────────────────────────

const CheckIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
    <circle cx="8" cy="8" r="7.5" stroke="#2E7D5B" strokeWidth="1" />
    <path d="M4.5 8l2.5 2.5 4.5-5" stroke="#2E7D5B" strokeWidth="1.6"
          strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const CrossIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
    <circle cx="8" cy="8" r="7.5" stroke="#C5392E" strokeWidth="1" />
    <path d="M5 5l6 6M11 5l-6 6" stroke="#C5392E" strokeWidth="1.6" strokeLinecap="round" />
  </svg>
);

const BoolCell = ({ value }) => (
  <td className="td td--center">{value ? <CheckIcon /> : <CrossIcon />}</td>
);

const TypeBadge = ({ type }) => {
  const style = TYPE_COLORS[type] ?? TYPE_COLORS.OTHER;
  return (
    <span className="type-badge" style={{ background: style.bg, color: style.color }}>
      {TYPE_LABELS[type] ?? type}
    </span>
  );
};

const formatDT = (amount) =>
  amount != null
    ? `${parseFloat(amount).toLocaleString('fr-TN', {
        minimumFractionDigits: 3,
        maximumFractionDigits: 3,
      })} DT`
    : '—';

function EditIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M11.333 2a1.886 1.886 0 0 1 2.667 2.667L4.667 14H2v-2.667L11.333 2z"
            stroke="currentColor" strokeWidth="1.4"
            strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M2 4h12M5.333 4V2.667a.667.667 0 0 1 .667-.667h4a.667.667 0 0 1 .667.667V4m1.333 0v9.333A1.333 1.333 0 0 1 10.667 14H5.333A1.333 1.333 0 0 1 4 13.333V4h8z"
            stroke="currentColor" strokeWidth="1.4"
            strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function AllowancesPage() {
  const [toast, setToast] = useState(null);

  const [allowances, setAllowances] = useState([]);
  const [employees,  setEmployees]  = useState([]);   // for modal dropdown
  const [loading,    setLoading]    = useState(true);

  // Filters
  const [filterName, setFilterName] = useState('');
  const [filterType, setFilterType] = useState('');

  // Modal
  const [modalOpen,  setModalOpen]  = useState(false);
  const [editTarget, setEditTarget] = useState(null); // null = create
  const [submitting, setSubmitting] = useState(false);

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState(null); // { id, label }
  const [deleting,     setDeleting]     = useState(false);
  function showToast(message, tone = "error") {
    setToast({ message, tone });
  }

  // ── Load data ─────────────────────────────────────────────────────────────

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const [allowanceData, employeeData] = await Promise.all([
        fetchAllowances(),
        fetchEmployees(),
      ]);

      setAllowances(allowanceData);

      setEmployees(
        employeeData.map((emp) => ({
          id: emp.id,
          fullName: emp.fullName,
          employeeCode: emp.employeeId,
        }))
      );
    } catch (err) {
      showToast(err.message ?? 'Impossible de charger les données');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ── Derived: filtered rows ─────────────────────────────────────────────────

  const filtered = allowances.filter((a) => {
    const nameMatch = !filterName ||
      a.employeeFullName?.toLowerCase().includes(filterName.toLowerCase());
    const typeMatch = !filterType || a.type === filterType;
    return nameMatch && typeMatch;
  });

  // ── CRUD handlers ──────────────────────────────────────────────────────────

  const openCreate = () => { setEditTarget(null); setModalOpen(true); };
  const openEdit   = (a)  => { setEditTarget(a);  setModalOpen(true); };
  const closeModal = () => { if (!submitting) setModalOpen(false); };

  const handleSubmit = async (formData) => {
    setSubmitting(true);
    try {
      if (editTarget) {
        const updated = await updateAllowance(editTarget.id, formData);
        setAllowances((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
        showToast('success', 'Prime modifiée avec succès');
      } else {
        const created = await createAllowance(formData);
        setAllowances((prev) => [...prev, created]);
        // Update employee dropdown if this is a new employee
        setEmployees((prev) =>
          prev.some((e) => e.id === created.employeeId)
            ? prev
            : [...prev, {
                id:          created.employeeId,
                fullName:    created.employeeFullName,
                employeeCode: created.employeeCode,
              }]
        );
        showToast('success', 'Prime créée avec succès');
      }
      setModalOpen(false);
    } catch (err) {
      showToast('error', err.message ?? 'Une erreur est survenue');
    } finally {
      setSubmitting(false);
    }
  };

  const openDelete  = (a)   => setDeleteTarget({ id: a.id, label: `${TYPE_LABELS[a.type] ?? a.type} — ${a.employeeFullName}` });
  const closeDelete = ()    => { if (!deleting) setDeleteTarget(null); };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteAllowance(deleteTarget.id);
      setAllowances((prev) => prev.filter((a) => a.id !== deleteTarget.id));
      showToast('success', 'Prime supprimée');
      setDeleteTarget(null);
    } catch (err) {
      showToast('error', err.message ?? 'Impossible de supprimer la prime');
    } finally {
      setDeleting(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="page">
      {/* Header */}
      <div className="page__header">
        <h1 className="page__title">Primes</h1>
        <button className="btn-add" onClick={openCreate}>
          <span className="btn-add__plus">⊕</span>
          Ajouter une prime
        </button>
      </div>

      {/* Card */}
      <div className="card">

        {/* Card header + filters */}
        <div className="card__top">
          <h2 className="card__title">Liste des primes</h2>

          <div className="filters">
            <input
              type="text"
              className="filter-input"
              placeholder="Rechercher un employé…"
              value={filterName}
              onChange={(e) => setFilterName(e.target.value)}
            />
            <select
              className="filter-select"
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
            >
              <option value="">Tous les types</option>
              {Object.entries(TYPE_LABELS).map(([val, lbl]) => (
                <option key={val} value={val}>{lbl}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Table */}
        {loading ? (
          <div className="state">Chargement…</div>
        ) : filtered.length === 0 ? (
          <div className="state state--empty">
            {allowances.length === 0
              ? "Aucune allocation enregistrée. Ajoutez-en une pour commencer."
              : "Aucun résultat pour ces filtres."}
          </div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Employé</th>
                  <th className="th">Type</th>
                  <th className="th th--right">Montant</th>
                  <th className="th th--center">Récurrente</th>
                  <th className="th th--center">Imposable</th>
                  <th className="th th--actions" />
                </tr>
              </thead>
              <tbody>
                {filtered.map((a) => (
                  <tr key={a.id} className="tr">
                    <td className="td">
                      <div className="employee-cell">
                        <span className="employee-name">{a.employeeFullName}</span>
                        {a.employeeCode && (
                          <span className="employee-code">{a.employeeCode}</span>
                        )}
                      </div>
                    </td>
                    <td className="td"><TypeBadge type={a.type} /></td>
                    <td className="td td--right td--amount">{formatDT(a.amount)}</td>
                    <BoolCell value={a.isRecurring} />
                    <BoolCell value={a.isTaxable} />
                    <td className="td td--actions">
                      <div className="row-actions">
                        <button
                          className="icon-btn icon-btn--edit"
                          onClick={() => openEdit(a)}
                          title="Modifier"
                          aria-label={`Modifier allocation ${a.id}`}
                        >
                          <EditIcon />
                        </button>
                        <button
                          className="icon-btn icon-btn--delete"
                          onClick={() => openDelete(a)}
                          title="Supprimer"
                          aria-label={`Supprimer allocation ${a.id}`}
                        >
                          <TrashIcon />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modals */}
      <AllowanceModal
        isOpen={modalOpen}
        onClose={closeModal}
        onSubmit={handleSubmit}
        initialData={editTarget}
        employees={employees}
        isSubmitting={submitting}
      />

      <DeleteConfirmModal
        isOpen={!!deleteTarget}
        onClose={closeDelete}
        onConfirm={handleDelete}
        title="Supprimer l'allocation"
        message={
          <>
            Êtes-vous sûr de vouloir supprimer <strong>{deleteTarget?.label}</strong> ?
            Cette action est irréversible.
          </>
        }
        isDeleting={deleting}
      />

      {/* Styles */}
      <style jsx>{`
        .page {
          padding: var(--space-6);
          max-width: 1100px;
          margin: 0 auto;
        }

        .page__header {
          display: flex; align-items: center; justify-content: space-between;
          margin-bottom: var(--space-5);
        }
        .page__title {
          font-size: var(--fs-xl); font-weight: var(--fw-bold);
          color: var(--color-text-primary); margin: 0;
        }

        .btn-add {
          display: flex; align-items: center; gap: var(--space-2);
          padding: var(--space-3) var(--space-5);
          background: var(--color-navy); color: var(--color-text-inverse);
          border: none; border-radius: var(--radius-md);
          font-size: var(--fs-sm); font-weight: var(--fw-semibold);
          font-family: var(--font-ui); cursor: pointer;
          transition: background var(--duration-fast) var(--ease-standard);
        }
        .btn-add:hover { background: var(--color-navy-soft); }
        .btn-add__plus { font-size: 1.1rem; }

        .card {
          background: var(--color-bg-elevated);
          border-radius: var(--radius-lg);
          box-shadow: var(--shadow-card);
          border: 1px solid var(--color-border);
          overflow: hidden;
        }
        .card__top {
          display: flex; align-items: center; justify-content: space-between;
          flex-wrap: wrap; gap: var(--space-3);
          padding: var(--space-5);
          border-bottom: 1px solid var(--color-border);
        }
        .card__title {
          font-size: var(--fs-base); font-weight: var(--fw-semibold);
          color: var(--color-text-primary); margin: 0;
        }

        .filters { display: flex; gap: var(--space-3); flex-wrap: wrap; }
        .filter-input, .filter-select {
          padding: var(--space-2) var(--space-3);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-md);
          font-size: var(--fs-sm); font-family: var(--font-ui);
          color: var(--color-text-primary); background: var(--color-bg);
          outline: none;
          transition: border-color var(--duration-fast) var(--ease-standard),
                      box-shadow   var(--duration-fast) var(--ease-standard);
        }
        .filter-input:focus, .filter-select:focus {
          border-color: var(--color-slate); box-shadow: var(--shadow-focus);
        }
        .filter-input   { min-width: 220px; }
        .filter-select  { min-width: 160px; cursor: pointer; }

        .state {
          padding: var(--space-8) var(--space-4);
          text-align: center; font-size: var(--fs-sm);
          color: var(--color-text-muted);
        }
        .state--empty { color: var(--color-text-secondary); }

        .table-wrap { overflow-x: auto; }
        .table { width: 100%; border-collapse: collapse; font-size: var(--fs-sm); }

        .th {
          padding: var(--space-3) var(--space-4);
          text-align: left;
          font-size: var(--fs-xs); font-weight: var(--fw-semibold);
          color: var(--color-text-secondary);
          text-transform: uppercase; letter-spacing: 0.04em;
          border-bottom: 1px solid var(--color-border);
          white-space: nowrap;
        }
        .th--center  { text-align: center; }
        .th--right   { text-align: right; }
        .th--actions { width: 72px; }

        .tr {
          border-bottom: 1px solid var(--color-border);
          transition: background var(--duration-fast) var(--ease-standard);
        }
        .tr:last-child { border-bottom: none; }
        .tr:hover      { background: var(--color-bg); }

        .td {
          padding: var(--space-3) var(--space-4);
          color: var(--color-text-primary); vertical-align: middle;
        }
        .td--center  { text-align: center; }
        .td--right   { text-align: right; }
        .td--amount  { color: var(--color-slate); font-weight: var(--fw-medium); }
        .td--actions { width: 72px; }

        .employee-cell { display: flex; flex-direction: column; gap: 2px; }
        .employee-name { font-weight: var(--fw-medium); color: var(--color-text-primary); }
        .employee-code { font-size: var(--fs-xs); color: var(--color-text-muted); }

        .type-badge {
          display: inline-block;
          padding: 3px 10px;
          border-radius: 99px;
          font-size: var(--fs-xs); font-weight: var(--fw-semibold);
          white-space: nowrap;
        }

        .row-actions { display: flex; align-items: center; gap: var(--space-1); }
        .icon-btn {
          display: flex; align-items: center; justify-content: center;
          width: 30px; height: 30px;
          border: none; border-radius: var(--radius-sm);
          background: transparent; cursor: pointer;
          transition: background var(--duration-fast) var(--ease-standard),
                      color    var(--duration-fast) var(--ease-standard);
        }
        .icon-btn--edit   { color: var(--color-slate); }
        .icon-btn--edit:hover   { background: var(--color-slate-light); color: var(--color-navy); }
        .icon-btn--delete { color: var(--color-text-muted); }
        .icon-btn--delete:hover { background: var(--color-error-bg); color: var(--color-error); }
        .icon-btn:focus-visible { outline: none; box-shadow: var(--shadow-focus); }

        @media (max-width: 768px) {
          .page { padding: var(--space-4); }
          .page__title { font-size: var(--fs-lg); }
          .card__top { flex-direction: column; align-items: stretch; }
          .filters { flex-direction: column; }
          .filter-input, .filter-select { min-width: unset; width: 100%; }
        }
      `}</style>
    </div>
  );
}