import { useState } from "react";
import "../../styles/permission/RoleAssignment.css";

/**
 * For a given permission, shows the roles that currently have it as
 * removable chips, plus a dropdown of roles that don't yet have it so the
 * admin can add it to another role. assignedRoleNames/availableRoles are
 * derived in the parent from the full roles list (each RoleDto carries its
 * own permissions set) rather than fetched per-permission.
 */
function RoleAssignment({ assignedRoles, availableRoles, disabled, onAssign, onUnassign }) {
  const [isBusy, setIsBusy] = useState(false);
  const [selectValue, setSelectValue] = useState("");

  async function handleAdd(event) {
    const roleId = Number(event.target.value);
    setSelectValue("");
    if (!roleId) return;
    setIsBusy(true);
    try {
      await onAssign(roleId);
    } finally {
      setIsBusy(false);
    }
  }

  async function handleRemove(roleId) {
    setIsBusy(true);
    try {
      await onUnassign(roleId);
    } finally {
      setIsBusy(false);
    }
  }

  return (
    <div className="role-assignment">
      <div className="role-assignment__chips">
        {assignedRoles.length === 0 ? (
          <span className="role-assignment__empty">Aucun rôle</span>
        ) : (
          assignedRoles.map((role) => (
            <span key={role.id} className="role-assignment__chip">
              {role.name.toLowerCase()}
              <button
                type="button"
                className="role-assignment__chip-remove"
                onClick={() => handleRemove(role.id)}
                disabled={disabled || isBusy}
                aria-label={`Retirer le rôle ${role.name}`}
              >
                ×
              </button>
            </span>
          ))
        )}
      </div>

      {availableRoles.length > 0 && (
        <select
          className="role-assignment__add-select"
          value={selectValue}
          onChange={handleAdd}
          disabled={disabled || isBusy}
          aria-label="Ajouter à un rôle"
        >
          <option value="">+ Ajouter à un rôle</option>
          {availableRoles.map((role) => (
            <option key={role.id} value={role.id}>
              {role.name.toLowerCase()}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}

export default RoleAssignment;
