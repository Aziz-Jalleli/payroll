import { useState } from "react";
import "../../styles/role/CreateRoleModal.css";

function CreateRoleModal({ permissionOptions, onCreate, onCancel }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [selectedPermissionIds, setSelectedPermissionIds] = useState([]);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  function togglePermission(permissionId) {
    setSelectedPermissionIds((prev) =>
      prev.includes(permissionId)
        ? prev.filter((id) => id !== permissionId)
        : [...prev, permissionId]
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!name.trim()) {
      setError("Le nom du rôle est obligatoire.");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      await onCreate({
        name: name.trim().toUpperCase(),
        description: description.trim() || undefined,
        permissionIds: selectedPermissionIds,
      });
    } catch (err) {
      setError(err.message || "Impossible de créer le rôle.");
      setIsSaving(false);
    }
  }

  return (
    <div className="create-role-modal__overlay" role="presentation" onClick={onCancel}>
      <form
        className="create-role-modal"
        onClick={(event) => event.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <h2 className="create-role-modal__title">Créer un rôle</h2>

        <div className="create-role-modal__field">
          <label htmlFor="role-name">Nom</label>
          <input
            id="role-name"
            type="text"
            placeholder="ex: HR_MANAGER"
            value={name}
            onChange={(event) => setName(event.target.value)}
            disabled={isSaving}
            autoFocus
          />
        </div>

        <div className="create-role-modal__field">
          <label htmlFor="role-description">Description (optionnel)</label>
          <input
            id="role-description"
            type="text"
            placeholder="ex: Gère les fiches employés et les départements"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            disabled={isSaving}
          />
        </div>

        {permissionOptions.length > 0 && (
          <div className="create-role-modal__field">
            <label>Attacher des permissions (optionnel)</label>
            <div className="create-role-modal__permission-grid">
              {permissionOptions.map((permission) => (
                <label key={permission.id} className="create-role-modal__permission-checkbox">
                  <input
                    type="checkbox"
                    checked={selectedPermissionIds.includes(permission.id)}
                    onChange={() => togglePermission(permission.id)}
                    disabled={isSaving}
                  />
                  <span className="create-role-modal__permission-label">
                    {permission.resource}:{permission.action}
                  </span>
                </label>
              ))}
            </div>
          </div>
        )}

        {error && <p className="create-role-modal__error">{error}</p>}

        <div className="create-role-modal__actions">
          <button type="button" className="create-role-modal__cancel" onClick={onCancel} disabled={isSaving}>
            Annuler
          </button>
          <button type="submit" className="create-role-modal__submit" disabled={isSaving}>
            {isSaving ? "Création…" : "Créer"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default CreateRoleModal;
