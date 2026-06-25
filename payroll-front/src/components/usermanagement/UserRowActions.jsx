import "../../styles/usermanagement/UserRowActions.css";

function UserRowActions({ enabled, onToggleActive, onDelete, disabled }) {
  return (
    <div className="user-row-actions">
      <button
        type="button"
        className="user-row-actions__toggle"
        onClick={onToggleActive}
        disabled={disabled}
      >
        {enabled ? "Désactiver" : "Activer"}
      </button>
      <button
        type="button"
        className="user-row-actions__delete"
        onClick={onDelete}
        disabled={disabled}
      >
        Supprimer
      </button>
    </div>
  );
}

export default UserRowActions;
