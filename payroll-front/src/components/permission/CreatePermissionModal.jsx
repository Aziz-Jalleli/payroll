import { useState } from "react";
import "../../styles/permission/CreatePermissionModal.css";

function CreatePermissionModal({ roleOptions, onCreate, onCancel }) {
  const [resource, setResource] = useState("");
  const [action, setAction] = useState("");
  const [description, setDescription] = useState("");
  const [selectedRoleIds, setSelectedRoleIds] = useState([]);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  function toggleRole(roleName) {
    setSelectedRoleIds((prev) =>
      prev.includes(roleName) ? prev.filter((r) => r !== roleName) : [...prev, roleName]
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!resource.trim() || !action.trim()) {
      setError("Resource et action sont obligatoires.");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      await onCreate({
        resource: resource.trim().toUpperCase(),
        action: action.trim().toUpperCase(),
        description: description.trim() || undefined,
        roleNames: selectedRoleIds,
      });
    } catch (err) {
      setError(err.message || "Impossible de créer la permission.");
      setIsSaving(false);
    }
  }

  return (
    <div className="create-permission-modal__overlay" role="presentation" onClick={onCancel}>
      <form
        className="create-permission-modal"
        onClick={(event) => event.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <h2 className="create-permission-modal__title">Créer une permission</h2>

        <div className="create-permission-modal__field">
          <label htmlFor="permission-resource">Resource</label>
          <input
            id="permission-resource"
            type="text"
            placeholder="ex: EMPLOYEE"
            value={resource}
            onChange={(event) => setResource(event.target.value)}
            disabled={isSaving}
            autoFocus
          />
        </div>

        <div className="create-permission-modal__field">
          <label htmlFor="permission-action">Action</label>
          <input
            id="permission-action"
            type="text"
            placeholder="ex: READ, CREATE, UPDATE, DELETE"
            value={action}
            onChange={(event) => setAction(event.target.value)}
            disabled={isSaving}
          />
        </div>

        <div className="create-permission-modal__field">
          <label htmlFor="permission-description">Description (optionnel)</label>
          <input
            id="permission-description"
            type="text"
            placeholder="ex: Voir les fiches employés"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            disabled={isSaving}
          />
        </div>

        {roleOptions.length > 0 && (
          <div className="create-permission-modal__field">
            <label>Assigner immédiatement à des rôles (optionnel)</label>
            <div className="create-permission-modal__role-grid">
              {roleOptions.map((role) => (
                <label key={role.id} className="create-permission-modal__role-checkbox">
                  <input
                    type="checkbox"
                    checked={selectedRoleIds.includes(role.name)}
                    onChange={() => toggleRole(role.name)}
                    disabled={isSaving}
                  />
                  {role.name.toLowerCase()}
                </label>
              ))}
            </div>
          </div>
        )}

        {error && <p className="create-permission-modal__error">{error}</p>}

        <div className="create-permission-modal__actions">
          <button type="button" className="create-permission-modal__cancel" onClick={onCancel} disabled={isSaving}>
            Annuler
          </button>
          <button type="submit" className="create-permission-modal__submit" disabled={isSaving}>
            {isSaving ? "Création…" : "Créer"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default CreatePermissionModal;
