import { useEffect, useMemo, useState, useCallback } from "react";
import PermissionFilterBar from "../components/permission/PermissionFilterBar";
import PermissionTable from "../components/permission/PermissionTable";
import CreatePermissionModal from "../components/permission/CreatePermissionModal";
import ConfirmDialog from "../components/usermanagement/ConfirmDialog";
import Toast from "../components/usermanagement/Toast";
import {
  listPermissions,
  createPermission,
  deletePermission,
  assignPermissionToRole,
  removePermissionFromRole,
} from "../services/permission/permissionService";
import { listRoles } from "../services/usermanagement/roleService";
import "../styles/permission/PermissionManagementPage.css";

function PermissionManagementPage() {
  const [permissions, setPermissions] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [pendingPermissionId, setPendingPermissionId] = useState(null);
  const [toast, setToast] = useState(null);
  const [isCreating, setIsCreating] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [permissionList, roleList] = await Promise.all([listPermissions(), listRoles()]);
      setPermissions(permissionList);
      setRoles(roleList);
    } catch (error) {
      setLoadError(error.message || "Impossible de charger les permissions.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredPermissions = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return permissions;
    return permissions.filter((permission) => {
      const fullName = `${permission.resource}:${permission.action}`.toLowerCase();
      return (
        fullName.includes(term) ||
        permission.resource?.toLowerCase().includes(term) ||
        permission.action?.toLowerCase().includes(term) ||
        permission.description?.toLowerCase().includes(term)
      );
    });
  }, [permissions, searchTerm]);

  function showToast(message, tone = "error") {
    setToast({ message, tone });
  }

  async function withRowPending(permissionId, action) {
    setPendingPermissionId(permissionId);
    try {
      await action();
    } catch (error) {
      showToast(error.message || "Une erreur est survenue.");
    } finally {
      setPendingPermissionId(null);
    }
  }

  async function handleCreate(payload) {
    const created = await createPermission(payload);
    setPermissions((prev) => [...prev, created]);
    // Re-fetch roles since the new permission may have been attached to
    // some of them immediately (roleNames in the create request).
    const refreshedRoles = await listRoles();
    setRoles(refreshedRoles);
    setIsCreating(false);
    showToast("Permission créée.", "success");
  }

  function handleAssignRole(permissionId, roleId) {
    return withRowPending(permissionId, async () => {
      const updatedRole = await assignPermissionToRole(roleId, permissionId);
      setRoles((prev) => prev.map((r) => (r.id === updatedRole.id ? updatedRole : r)));
    });
  }

  function handleUnassignRole(permissionId, roleId) {
    return withRowPending(permissionId, async () => {
      const updatedRole = await removePermissionFromRole(roleId, permissionId);
      setRoles((prev) => prev.map((r) => (r.id === updatedRole.id ? updatedRole : r)));
    });
  }

  function requestDelete(permission) {
    setPendingDelete(permission);
  }

  async function confirmDelete() {
    const permissionId = pendingDelete.id;
    setPendingDelete(null);
    await withRowPending(permissionId, async () => {
      await deletePermission(permissionId);
      setPermissions((prev) => prev.filter((p) => p.id !== permissionId));
      showToast("Permission supprimée.", "success");
    });
  }

  return (
    <div className="permission-management-page">
      <header className="permission-management-page__header">
        <h1 className="permission-management-page__title">Permissions</h1>
        <p className="permission-management-page__subtitle">
          Créez des permissions et assignez-les aux rôles.
        </p>
      </header>

      <div className="permission-management-page__card">
        <div className="permission-management-page__card-header">
          <h2 className="permission-management-page__card-title">Gestion des Permissions</h2>
          <button
            type="button"
            className="permission-management-page__create-btn"
            onClick={() => setIsCreating(true)}
          >
            <svg viewBox="0 0 20 20" aria-hidden="true" className="permission-management-page__create-icon">
              <path
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                d="M10 4v12M4 10h12"
              />
              <circle cx="10" cy="10" r="8.3" fill="none" stroke="currentColor" strokeWidth="1.3" />
            </svg>
            Créer une permission
          </button>
        </div>

        <PermissionFilterBar searchTerm={searchTerm} onSearchChange={setSearchTerm} />

        {loading ? (
          <p className="permission-management-page__status">Chargement des permissions…</p>
        ) : loadError ? (
          <p className="permission-management-page__status permission-management-page__status--error">
            {loadError}
          </p>
        ) : (
          <PermissionTable
            permissions={filteredPermissions}
            roles={roles}
            pendingPermissionId={pendingPermissionId}
            onAssignRole={handleAssignRole}
            onUnassignRole={handleUnassignRole}
            onDeletePermission={requestDelete}
          />
        )}
      </div>

      {isCreating && (
        <CreatePermissionModal
          roleOptions={roles}
          onCreate={handleCreate}
          onCancel={() => setIsCreating(false)}
        />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Supprimer cette permission ?"
          message={`"${pendingDelete.resource}:${pendingDelete.action}" sera supprimée définitivement. Si elle est encore assignée à un rôle, retirez-la d'abord de ce rôle ci-dessus.`}
          confirmLabel="Supprimer"
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}

      <Toast message={toast?.message} tone={toast?.tone} onDismiss={() => setToast(null)} />
    </div>
  );
}

export default PermissionManagementPage;
