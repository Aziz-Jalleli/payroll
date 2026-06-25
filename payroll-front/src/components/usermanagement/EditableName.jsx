import { useState } from "react";
import "../../styles/usermanagement/EditableName.css";

function EditableName({ value, onSave, disabled }) {
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(value);
  const [isSaving, setIsSaving] = useState(false);

  function startEditing() {
    if (disabled) return;
    setDraft(value);
    setIsEditing(true);
  }

  async function commit() {
    const trimmed = draft.trim();
    if (!trimmed || trimmed === value) {
      setIsEditing(false);
      return;
    }
    setIsSaving(true);
    try {
      await onSave(trimmed);
      setIsEditing(false);
    } finally {
      setIsSaving(false);
    }
  }

  function handleKeyDown(event) {
    if (event.key === "Enter") commit();
    if (event.key === "Escape") setIsEditing(false);
  }

  if (isEditing) {
    return (
      <input
        type="text"
        className="editable-name__input"
        value={draft}
        autoFocus
        disabled={isSaving}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={commit}
        onKeyDown={handleKeyDown}
        aria-label="Nom complet"
      />
    );
  }

  return (
    <button
      type="button"
      className="editable-name__display"
      onClick={startEditing}
      disabled={disabled}
      title={disabled ? undefined : "Cliquer pour modifier"}
    >
      {value}
    </button>
  );
}

export default EditableName;
