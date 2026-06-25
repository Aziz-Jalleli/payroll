import { useState } from "react";
import "../../styles/role/EditableRoleFields.css";

/**
 * Click-to-edit for a role's name and description together. They're
 * edited as one unit (not two independent EditableName instances) because
 * the backend's PUT /api/admin/roles/{id} takes both fields in a single
 * UpdateRoleRequest body - editing them separately would mean each save
 * has to resend the other field's current value, risking a silently
 * dropped edit if both are touched in quick succession.
 */
function EditableRoleFields({ name, description, onSave, disabled }) {
  const [isEditing, setIsEditing] = useState(false);
  const [draftName, setDraftName] = useState(name);
  const [draftDescription, setDraftDescription] = useState(description || "");
  const [isSaving, setIsSaving] = useState(false);

  function startEditing() {
    if (disabled) return;
    setDraftName(name);
    setDraftDescription(description || "");
    setIsEditing(true);
  }

  async function commit() {
    const trimmedName = draftName.trim();
    if (!trimmedName) {
      setIsEditing(false);
      return;
    }
    const trimmedDescription = draftDescription.trim();
    if (trimmedName === name && trimmedDescription === (description || "")) {
      setIsEditing(false);
      return;
    }
    setIsSaving(true);
    try {
      await onSave({ name: trimmedName, description: trimmedDescription || null });
      setIsEditing(false);
    } finally {
      setIsSaving(false);
    }
  }

  function handleKeyDown(event) {
    if (event.key === "Enter" && event.target.tagName !== "TEXTAREA") commit();
    if (event.key === "Escape") setIsEditing(false);
  }

  if (isEditing) {
    return (
      <div className="editable-role-fields editable-role-fields--editing" onKeyDown={handleKeyDown}>
        <input
          type="text"
          className="editable-role-fields__name-input"
          value={draftName}
          autoFocus
          disabled={isSaving}
          onChange={(event) => setDraftName(event.target.value)}
          aria-label="Nom du rôle"
        />
        <input
          type="text"
          className="editable-role-fields__description-input"
          value={draftDescription}
          disabled={isSaving}
          placeholder="Description (optionnel)"
          onChange={(event) => setDraftDescription(event.target.value)}
          onBlur={commit}
          aria-label="Description du rôle"
        />
      </div>
    );
  }

  return (
    <button
      type="button"
      className="editable-role-fields editable-role-fields__display"
      onClick={startEditing}
      disabled={disabled}
      title={disabled ? undefined : "Cliquer pour modifier"}
    >
      <span className="editable-role-fields__name">{name.toLowerCase()}</span>
      {description && (
        <span className="editable-role-fields__description">{description}</span>
      )}
    </button>
  );
}

export default EditableRoleFields;
