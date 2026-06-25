import "../../styles/permission/PermissionRowActions.css";

function PermissionRowActions({ onDelete, disabled }) {
  return (
    <div className="permission-row-actions">
      <button
        type="button"
        className="permission-row-actions__delete"
        onClick={onDelete}
        disabled={disabled}
      >
        Supprimer
      </button>
    </div>
  );
}

export default PermissionRowActions;
