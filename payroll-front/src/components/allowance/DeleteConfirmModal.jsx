// src/components/DeleteConfirmModal.jsx
import React from 'react';

/**
 * Reusable destructive-action confirmation dialog.
 *
 * Props:
 *  - isOpen      {boolean}
 *  - onClose     {() => void}
 *  - onConfirm   {() => void}
 *  - title       {string}
 *  - message     {ReactNode}
 *  - isDeleting  {boolean}
 */
export const DeleteConfirmModal = ({ isOpen, onClose, onConfirm, title, message, isDeleting }) => {
  if (!isOpen) return null;

  return (
    <>
      <div className="backdrop" onClick={!isDeleting ? onClose : undefined} />
      <div className="dialog" role="alertdialog" aria-modal="true">
        <div className="dialog__icon">🗑️</div>
        <h3 className="dialog__title">{title ?? 'Confirmer la suppression'}</h3>
        <p className="dialog__body">{message}</p>
        <div className="dialog__actions">
          <button className="btn btn--secondary" onClick={onClose} disabled={isDeleting}>
            Annuler
          </button>
          <button className="btn btn--danger" onClick={onConfirm} disabled={isDeleting}>
            {isDeleting ? 'Suppression…' : 'Supprimer'}
          </button>
        </div>
      </div>

      <style jsx>{`
        .backdrop {
          position: fixed; inset: 0;
          background: rgba(27,36,48,0.4); z-index: 200;
        }
        .dialog {
          position: fixed; top: 50%; left: 50%;
          transform: translate(-50%, -50%);
          z-index: 201;
          background: var(--color-bg-elevated);
          border-radius: var(--radius-lg);
          box-shadow: var(--shadow-card);
          padding: var(--space-6);
          width: 100%; max-width: 400px;
          text-align: center;
          animation: pop var(--duration-base) var(--ease-standard);
        }
        @keyframes pop {
          from { transform: translate(-50%,-48%) scale(.96); opacity: 0; }
          to   { transform: translate(-50%,-50%) scale(1);   opacity: 1; }
        }
        .dialog__icon  { font-size: 2rem; margin-bottom: var(--space-3); }
        .dialog__title {
          font-size: var(--fs-lg); font-weight: var(--fw-semibold);
          color: var(--color-text-primary); margin: 0 0 var(--space-3);
        }
        .dialog__body {
          font-size: var(--fs-sm); color: var(--color-text-secondary);
          line-height: var(--lh-base); margin: 0 0 var(--space-5);
        }
        .dialog__actions { display: flex; gap: var(--space-3); justify-content: center; }

        .btn {
          padding: var(--space-3) var(--space-5);
          border-radius: var(--radius-md);
          font-size: var(--fs-sm); font-weight: var(--fw-semibold);
          font-family: var(--font-ui); cursor: pointer; border: none;
          transition: background var(--duration-fast) var(--ease-standard);
        }
        .btn:disabled { opacity: 0.6; cursor: not-allowed; }
        .btn--secondary { background: var(--color-slate-light); color: var(--color-text-primary); }
        .btn--secondary:hover:not(:disabled) { background: var(--color-border); }
        .btn--danger    { background: var(--color-error); color: #fff; }
        .btn--danger:hover:not(:disabled)    { background: #a82e24; }
      `}</style>
    </>
  );
};