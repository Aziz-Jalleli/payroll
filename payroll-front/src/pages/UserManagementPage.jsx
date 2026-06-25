import { useEffect, useMemo, useState, useCallback } from "react";
import UserFilterBar from "../components/usermanagement/UserFilterBar";
import UserTable from "../components/usermanagement/UserTable";
import DragScrollContainer from "../components/usermanagement/DragScrollContainer";
import ConfirmDialog from "../components/usermanagement/ConfirmDialog";
import Toast from "../components/usermanagement/Toast";
import {
  listUsers,
  updateUser,
  activateUser,
  deactivateUser,
  deleteUser,
  setUserRole,
} from "../services/usermanagement/userService";
import { listRoles } from "../services/usermanagement/roleService";
import { getCurrentUser } from "../services/authService";
import "../styles/usermanagement/UserManagementPage.css";

function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [roleOptions, setRoleOptions] = useState([]);
  const [currentUserId, setCurrentUserId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("");

  const [pendingUserId, setPendingUserId] = useState(null);
  const [toast, setToast] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [userList, roleList, me] = await Promise.all([
        listUsers(),
        listRoles(),
        getCurrentUser(),
      ]);
      setUsers(userList);
      setRoleOptions(roleList);
      setCurrentUserId(me ? me.id : null);
    } catch (error) {
      setLoadError(error.message || "Impossible de charger les utilisateurs.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredUsers = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    return users.filter((user) => {
      const matchesTerm =
        !term ||
        user.fullName?.toLowerCase().includes(term) ||
        user.email?.toLowerCase().includes(term);

      const matchesRole =
        !roleFilter || (user.roles || []).some((r) => r === roleFilter);

      return matchesTerm && matchesRole;
    });
  }, [users, searchTerm, roleFilter]);

  function showToast(message, tone = "error") {
    setToast({ message, tone });
  }

  async function withRowPending(userId, action) {
    setPendingUserId(userId);
    try {
      await action();
    } catch (error) {
      showToast(error.message || "Une erreur est survenue.");
    } finally {
      setPendingUserId(null);
    }
  }

  function handleRenameUser(userId, newName) {
    return withRowPending(userId, async () => {
      const updated = await updateUser(userId, { fullName: newName });
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    });
  }

  function handleChangeRole(userId, newRole) {
    return withRowPending(userId, async () => {
      const updated = await setUserRole(userId, newRole);
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    });
  }

  function handleToggleActive(userId, makeActive) {
    return withRowPending(userId, async () => {
      const updated = makeActive ? await activateUser(userId) : await deactivateUser(userId);
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    });
  }

  function requestDelete(userId) {
    const target = users.find((u) => u.id === userId);
    setPendingDelete(target || { id: userId, fullName: "cet utilisateur" });
  }

  async function confirmDelete() {
    const userId = pendingDelete.id;
    setPendingDelete(null);
    await withRowPending(userId, async () => {
      await deleteUser(userId);
      setUsers((prev) => prev.filter((u) => u.id !== userId));
      showToast("Utilisateur supprimé.", "success");
    });
  }

  return (
    <div className="user-management-page">
      <header className="user-management-page__header">
        <h1 className="user-management-page__title">Utilisateurs</h1>
        <p className="user-management-page__subtitle">
          Invitez des collaborateurs et gérez leurs rôles.
        </p>
      </header>

      <div className="user-management-page__card">
        <div className="user-management-page__card-header">
          <h2 className="user-management-page__card-title">Gestion des Utilisateurs</h2>
          <button type="button" className="user-management-page__create-btn" >
            <svg viewBox="0 0 20 20" aria-hidden="true" className="user-management-page__create-icon">
              <path
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                d="M10 4v12M4 10h12"
              />
              <circle cx="10" cy="10" r="8.3" fill="none" stroke="currentColor" strokeWidth="1.3" />
            </svg>
            Créer un utilisateur
          </button>
        </div>

        <UserFilterBar
          searchTerm={searchTerm}
          onSearchChange={setSearchTerm}
          roleFilter={roleFilter}
          onRoleFilterChange={setRoleFilter}
          roleOptions={roleOptions}
        />

        {loading ? (
          <p className="user-management-page__status">Chargement des utilisateurs…</p>
        ) : loadError ? (
          <p className="user-management-page__status user-management-page__status--error">
            {loadError}
          </p>
        ) : (
          <DragScrollContainer className="user-management-page__table-scroll">
            <UserTable
              users={filteredUsers}
              roleOptions={roleOptions}
              currentUserId={currentUserId}
              pendingUserId={pendingUserId}
              onRenameUser={handleRenameUser}
              onChangeRole={handleChangeRole}
              onToggleActive={handleToggleActive}
              onDeleteUser={requestDelete}
            />
          </DragScrollContainer>
        )}
      </div>

      {pendingDelete && (
        <ConfirmDialog
          title="Supprimer cet utilisateur ?"
          message={`Cette action est définitive et supprimera le compte de ${pendingDelete.fullName}. Cette action est irréversible.`}
          confirmLabel="Supprimer"
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}

      <Toast message={toast?.message} tone={toast?.tone} onDismiss={() => setToast(null)} />
    </div>
  );
}

export default UserManagementPage;