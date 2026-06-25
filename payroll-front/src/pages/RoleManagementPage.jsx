import { useEffect, useMemo, useState, useCallback } from "react";
import RoleFilterBar from "../components/role/RoleFilterBar";
import RoleTable from "../components/role/RoleTable";
import CreateRoleModal from "../components/role/CreateRoleModal";
import ConfirmDialog from "../components/usermanagement/ConfirmDialog";
import Toast from "../components/usermanagement/Toast";
import { listRoles, createRole, updateRole, deleteRole } from "../services/usermanagement/roleService";
import { listPermissions } from "../services/permission/permissionService";
import "../styles/role/RoleManagementPage.css";

function RoleManagementPage() {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [pendingRoleId, setPendingRoleId] = useState(null);
  const [toast, setToast] = useState(null);
  const [isCreating, setIsCreating] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [roleList, permissionList] = await Promise.all([listRoles(), listPermissions()]);
      setRoles(roleList);
      setPermissions(permissionList);
    } catch (error) {
      setLoadError(error.message || "Impossible de charger les rôles.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredRoles = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return roles;
    return roles.filter((role) => role.name?.toLowerCase().includes(term));
  }, [roles, searchTerm]);

  function showToast(message, tone = "error") {
    setToast({ message, tone });
  }

  async function withRowPending(roleId, action) {
    setPendingRoleId(roleId);
    try {
      await action();
    } catch (error) {
      showToast(error.message || "Une erreur est survenue.");
    } finally {
      setPendingRoleId(null);
    }
  }

  async function handleCreate(payload) {
    const created = await createRole(payload);
    setRoles((prev) => [...prev, created]);
    setIsCreating(false);
    showToast("Rôle créé.", "success");
  }

  function handleUpdateRole(roleId, fields) {
    return withRowPending(roleId, async () => {
      const updated = await updateRole(roleId, fields);
      setRoles((prev) => prev.map((r) => (r.id === roleId ? updated : r)));
    });
  }

  function requestDelete(role) {
    setPendingDelete(role);
  }

  async function confirmDelete() {
    const roleId = pendingDelete.id;
    setPendingDelete(null);
    await withRowPending(roleId, async () => {
      await deleteRole(roleId);
      setRoles((prev) => prev.filter((r) => r.id !== roleId));
      showToast("Rôle supprimé.", "success");
    });
  }

  return (
    <div className="role-management-page">
      <header className="role-management-page__header">
        <h1 className="role-management-page__title">Rôles</h1>
        <p className="role-management-page__subtitle">
          Créez des rôles et gérez leurs permissions.
        </p>
      </header>

      <div className="role-management-page__card">
        <div className="role-management-page__card-header">
          <h2 className="role-management-page__card-title">Gestion des Rôles</h2>
          <button
            type="button"
            className="role-management-page__create-btn"
            onClick={() => setIsCreating(true)}
          >
            <svg viewBox="0 0 20 20" aria-hidden="true" className="role-management-page__create-icon">
              <path
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                d="M10 4v12M4 10h12"
              />
              <circle cx="10" cy="10" r="8.3" fill="none" stroke="currentColor" strokeWidth="1.3" />
            </svg>
            Créer un rôle
          </button>
        </div>

        <RoleFilterBar searchTerm={searchTerm} onSearchChange={setSearchTerm} />

        {loading ? (
          <p className="role-management-page__status">Chargement des rôles…</p>
        ) : loadError ? (
          <p className="role-management-page__status role-management-page__status--error">
            {loadError}
          </p>
        ) : (
          <RoleTable
            roles={filteredRoles}
            pendingRoleId={pendingRoleId}
            onUpdateRole={handleUpdateRole}
            onDeleteRole={requestDelete}
          />
        )}
      </div>

      {isCreating && (
        <CreateRoleModal
          permissionOptions={permissions}
          onCreate={handleCreate}
          onCancel={() => setIsCreating(false)}
        />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Supprimer ce rôle ?"
          message={`"${pendingDelete.name?.toLowerCase()}" sera supprimé définitivement. Si des utilisateurs ont encore ce rôle, la suppression sera refusée - retirez-le d'abord de ces comptes via la page Utilisateurs.`}
          confirmLabel="Supprimer"
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}

      <Toast message={toast?.message} tone={toast?.tone} onDismiss={() => setToast(null)} />
    </div>
  );
}

export default RoleManagementPage;
