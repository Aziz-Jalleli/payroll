import { useState } from "react";
import "../../styles/usermanagement/RoleSelect.css";

/**
 * Renders the user's current role as a pill (matching the visual design),
 * but the pill itself is a <select> styled to look like a badge - clicking
 * it opens the native dropdown so the admin can reassign the role inline,
 * without a separate edit mode or modal.
 */
function RoleSelect({ currentRole, roleOptions, onChange, disabled }) {
  const [isSaving, setIsSaving] = useState(false);

  async function handleChange(event) {
    const newRole = event.target.value;
    if (newRole === currentRole) return;
    setIsSaving(true);
    try {
      await onChange(newRole);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <select
      className="role-select"
      value={currentRole || ""}
      onChange={handleChange}
      disabled={disabled || isSaving}
      aria-label="Rôle de l'utilisateur"
    >
      {!currentRole && <option value="">Aucun rôle</option>}
      {roleOptions.map((role) => (
        <option key={role.name} value={role.name}>
          {role.name.toLowerCase()}
        </option>
      ))}
    </select>
  );
}

export default RoleSelect;
