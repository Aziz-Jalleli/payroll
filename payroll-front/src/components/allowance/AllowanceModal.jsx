// src/components/AllowanceModal.jsx
import React, { useEffect, useState } from 'react';

// Must match Allowance.AllowanceType enum on the backend
const ALLOWANCE_TYPES = [
  { value: 'TRANSPORT',      label: 'Transport' },
  { value: 'HOUSING',        label: 'Logement' },
  { value: 'MEAL',           label: 'Panier / Repas' },
  { value: 'PHONE',          label: 'Téléphone' },
  { value: 'REPRESENTATION', label: 'Représentation' },
  { value: 'OTHER',          label: 'Autre' },
];

const EMPTY_FORM = {
  employeeId: '',
  type: '',
  amount: '',
  isRecurring: true,
  isTaxable: true,
};

/**
 * Modal for creating or updating an Allowance.
 *
 * Props:
 *  - isOpen        {boolean}
 *  - onClose       {() => void}
 *  - onSubmit      {(formData) => Promise<void>}
 *  - initialData   {AllowanceDto | null}   null = create mode
 *  - employees     {Array<{id, fullName, employeeId}>}
 *  - isSubmitting  {boolean}
 */
export const AllowanceModal = ({
  isOpen,
  onClose,
  onSubmit,
  initialData,
  employees = [],
  isSubmitting,
}) => {
  const [form, setForm]     = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const isEdit = !!initialData;

  useEffect(() => {
    if (!isOpen) return;
    if (initialData) {
      setForm({
        employeeId:  initialData.employeeId?.toString() ?? '',
        type:        initialData.type ?? '',
        amount:      initialData.amount?.toString() ?? '',
        isRecurring: initialData.isRecurring ?? true,
        isTaxable:   initialData.isTaxable   ?? true,
      });
    } else {
      setForm(EMPTY_FORM);
    }
    setErrors({});
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const validate = () => {
    const errs = {};
    if (!form.employeeId)                      errs.employeeId = "L'employé est obligatoire";
    if (!form.type)                            errs.type       = 'Le type est obligatoire';
    if (!form.amount || isNaN(parseFloat(form.amount)) || parseFloat(form.amount) <= 0)
                                               errs.amount     = 'Le montant doit être un nombre positif';
    return errs;
  };

  const handleChange = (e) => {
    const { name, type, checked, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    await onSubmit({
      employeeId:  parseInt(form.employeeId, 10),
      type:        form.type,
      amount:      parseFloat(form.amount),
      isRecurring: form.isRecurring,
      isTaxable:   form.isTaxable,
    });
  };

  const TOGGLES = [
    { key: 'isRecurring', label: 'Récurrente' },
    { key: 'isTaxable',   label: 'Imposable' },
  ];

  return (
    <>
      <div className="backdrop" onClick={!isSubmitting ? onClose : undefined} />
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">

        <div className="modal__header">
          <h2 id="modal-title" className="modal__title">
            {isEdit ? "Modifier l'allocation" : 'Ajouter une allocation'}
          </h2>
          <button className="modal__close" onClick={onClose} disabled={isSubmitting}
                  aria-label="Fermer">×</button>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="modal__body">

            {/* Employee */}
            <div className="form-group">
              <label className="form-label" htmlFor="employeeId">
                Employé <span className="required">*</span>
              </label>
              <select
                id="employeeId"
                name="employeeId"
                className={`form-select${errors.employeeId ? ' form-input--error' : ''}`}
                value={form.employeeId}
                onChange={handleChange}
              >
                <option value="">— Sélectionner un employé —</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.fullName} {emp.employeeCode ? `(${emp.employeeCode})` : ''}
                  </option>
                ))}
              </select>
              {errors.employeeId && <span className="form-error">{errors.employeeId}</span>}
            </div>

            {/* Type */}
            <div className="form-group">
              <label className="form-label" htmlFor="type">
                Type <span className="required">*</span>
              </label>
              <select
                id="type"
                name="type"
                className={`form-select${errors.type ? ' form-input--error' : ''}`}
                value={form.type}
                onChange={handleChange}
              >
                <option value="">— Sélectionner un type —</option>
                {ALLOWANCE_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
              {errors.type && <span className="form-error">{errors.type}</span>}
            </div>

            {/* Amount */}
            <div className="form-group">
              <label className="form-label" htmlFor="amount">
                Montant (DT) <span className="required">*</span>
              </label>
              <input
                id="amount"
                name="amount"
                type="number"
                step="0.001"
                min="0.001"
                className={`form-input${errors.amount ? ' form-input--error' : ''}`}
                value={form.amount}
                onChange={handleChange}
                placeholder="0.000"
              />
              {errors.amount && <span className="form-error">{errors.amount}</span>}
            </div>

            {/* Toggles */}
            <div className="form-group">
              <span className="form-label">Attributs</span>
              <div className="toggle-row">
                {TOGGLES.map(({ key, label }) => (
                  <label key={key} className="toggle">
                    <div className="toggle__track">
                      <input
                        type="checkbox"
                        name={key}
                        checked={form[key]}
                        onChange={handleChange}
                        className="toggle__input"
                      />
                      <span className="toggle__thumb" />
                    </div>
                    <span className="toggle__label">{label}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          <div className="modal__footer">
            <button type="button" className="btn btn--secondary"
                    onClick={onClose} disabled={isSubmitting}>
              Annuler
            </button>
            <button type="submit" className="btn btn--primary" disabled={isSubmitting}>
              {isSubmitting
                ? (isEdit ? 'Modification…' : 'Création…')
                : (isEdit ? 'Modifier' : 'Ajouter')}
            </button>
          </div>
        </form>
      </div>

      <style jsx>{`
        .backdrop {
          position: fixed; inset: 0;
          background: rgba(27, 36, 48, 0.4);
          z-index: 200;
          animation: fadeIn var(--duration-base) var(--ease-standard);
        }
        .modal {
          position: fixed; top: 50%; left: 50%;
          transform: translate(-50%, -50%);
          z-index: 201;
          background: var(--color-bg-elevated);
          border-radius: var(--radius-lg);
          box-shadow: var(--shadow-card);
          width: 100%; max-width: 500px;
          max-height: 90vh;
          display: flex; flex-direction: column;
          animation: slideUp var(--duration-base) var(--ease-standard);
        }
        @keyframes fadeIn  { from { opacity: 0; } to { opacity: 1; } }
        @keyframes slideUp {
          from { transform: translate(-50%, -46%); opacity: 0; }
          to   { transform: translate(-50%, -50%); opacity: 1; }
        }

        .modal__header {
          display: flex; align-items: center; justify-content: space-between;
          padding: var(--space-5) var(--space-6);
          border-bottom: 1px solid var(--color-border);
        }
        .modal__title {
          font-size: var(--fs-lg); font-weight: var(--fw-semibold);
          color: var(--color-text-primary); margin: 0;
        }
        .modal__close {
          background: none; border: none; font-size: 1.4rem; line-height: 1;
          color: var(--color-text-muted); cursor: pointer;
          padding: var(--space-1); border-radius: var(--radius-sm);
          transition: color var(--duration-fast) var(--ease-standard),
                      background var(--duration-fast) var(--ease-standard);
        }
        .modal__close:hover:not(:disabled) {
          color: var(--color-text-primary); background: var(--color-slate-light);
        }

        .modal__body {
          padding: var(--space-5) var(--space-6);
          overflow-y: auto;
          display: flex; flex-direction: column; gap: var(--space-4);
        }
        .modal__footer {
          padding: var(--space-4) var(--space-6);
          border-top: 1px solid var(--color-border);
          display: flex; justify-content: flex-end; gap: var(--space-3);
        }

        .form-group { display: flex; flex-direction: column; gap: var(--space-2); }
        .form-label {
          font-size: var(--fs-sm); font-weight: var(--fw-medium);
          color: var(--color-text-primary);
        }
        .required { color: var(--color-error); }

        .form-input, .form-select {
          padding: var(--space-3) var(--space-4);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-md);
          font-size: var(--fs-base); font-family: var(--font-ui);
          color: var(--color-text-primary); background: var(--color-bg);
          outline: none;
          transition: border-color var(--duration-fast) var(--ease-standard),
                      box-shadow   var(--duration-fast) var(--ease-standard);
        }
        .form-select { appearance: none; cursor: pointer; }
        .form-input:focus, .form-select:focus {
          border-color: var(--color-slate); box-shadow: var(--shadow-focus);
        }
        .form-input--error { border-color: var(--color-error); }
        .form-input--error:focus { box-shadow: 0 0 0 3px rgba(197,57,46,0.2); }
        .form-error { font-size: var(--fs-xs); color: var(--color-error); }

        /* Toggles */
        .toggle-row { display: flex; gap: var(--space-6); flex-wrap: wrap; }
        .toggle { display: flex; align-items: center; gap: var(--space-3); cursor: pointer; user-select: none; }
        .toggle__track { position: relative; width: 40px; height: 22px; flex-shrink: 0; }
        .toggle__input { position: absolute; opacity: 0; width: 0; height: 0; }
        .toggle__thumb {
          position: absolute; inset: 0;
          background: var(--color-border-strong);
          border-radius: 99px;
          transition: background var(--duration-fast) var(--ease-standard);
        }
        .toggle__thumb::after {
          content: ''; position: absolute;
          width: 16px; height: 16px; border-radius: 50%; background: white;
          top: 3px; left: 3px;
          transition: transform var(--duration-fast) var(--ease-standard);
          box-shadow: 0 1px 3px rgba(0,0,0,0.2);
        }
        .toggle__input:checked + .toggle__thumb { background: var(--color-success); }
        .toggle__input:checked + .toggle__thumb::after { transform: translateX(18px); }
        .toggle__input:focus-visible + .toggle__thumb { box-shadow: var(--shadow-focus); }
        .toggle__label { font-size: var(--fs-sm); color: var(--color-text-secondary); }

        /* Buttons */
        .btn {
          padding: var(--space-3) var(--space-5);
          border-radius: var(--radius-md);
          font-size: var(--fs-sm); font-weight: var(--fw-semibold);
          font-family: var(--font-ui); cursor: pointer; border: none;
          transition: background var(--duration-fast) var(--ease-standard),
                      opacity var(--duration-fast) var(--ease-standard);
        }
        .btn:disabled { opacity: 0.6; cursor: not-allowed; }
        .btn--primary  { background: var(--color-navy); color: var(--color-text-inverse); }
        .btn--primary:hover:not(:disabled)  { background: var(--color-navy-soft); }
        .btn--secondary { background: var(--color-slate-light); color: var(--color-text-primary); }
        .btn--secondary:hover:not(:disabled) { background: var(--color-border); }
      `}</style>
    </>
  );
};