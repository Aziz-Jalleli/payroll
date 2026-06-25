import "../../styles/role/RoleRowActions.css";

function RoleRowActions({ onDelete, disabled }) {
  return (
    <div className="role-row-actions">
      <button
        type="button"
        className="role-row-actions__delete"
        onClick={onDelete}
        disabled={disabled}
      >
        Supprimer
      </button>
    </div>
  );
}

export default RoleRowActions;
