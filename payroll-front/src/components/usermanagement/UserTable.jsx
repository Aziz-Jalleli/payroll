import StatusPill from "./StatusPill";
import RoleSelect from "./RoleSelect";
import EditableName from "./EditableName";
import UserRowActions from "./UserRowActions";
import "../../styles/usermanagement/UserTable.css";

function formatDate(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  return date.toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function UserTable({
  users,
  roleOptions,
  currentUserId,
  pendingUserId,
  onRenameUser,
  onChangeRole,
  onToggleActive,
  onDeleteUser,
}) {
  if (users.length === 0) {
    return (
      <div className="user-table__empty">
        Aucun utilisateur ne correspond à ces filtres.
      </div>
    );
  }

  return (
    <table className="user-table">
      <thead>
        <tr>
          <th>Nom complet</th>
          <th>Email</th>
          <th>Rôle</th>
          <th>Statut</th>
          <th>Date de création</th>
          <th aria-hidden="true" />
        </tr>
      </thead>
      <tbody>
        {users.map((user) => {
          const isSelf = currentUserId != null && user.id === currentUserId;
          const isRowBusy = pendingUserId === user.id;

          return (
            <tr key={user.id}>
              <td>
                <EditableName
                  value={user.fullName}
                  disabled={isRowBusy}
                  onSave={(newName) => onRenameUser(user.id, newName)}
                />
              </td>
              <td className="user-table__email">{user.email}</td>
              <td>
                <RoleSelect
                  currentRole={[...(user.roles || [])][0]}
                  roleOptions={roleOptions}
                  disabled={isRowBusy}
                  onChange={(newRole) => onChangeRole(user.id, newRole)}
                />
              </td>
              <td>
                <StatusPill enabled={user.enabled} />
              </td>
              <td className="user-table__date">{formatDate(user.createdAt)}</td>
              <td>
                <UserRowActions
                  enabled={user.enabled}
                  disabled={isRowBusy || isSelf}
                  onToggleActive={() => onToggleActive(user.id, !user.enabled)}
                  onDelete={() => onDeleteUser(user.id)}
                />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default UserTable;
