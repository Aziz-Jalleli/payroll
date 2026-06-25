import EditableRoleFields from "./EditableRoleFields";
import PermissionCountPill from "./PermissionCountPill";
import RoleRowActions from "./RoleRowActions";
import "../../styles/role/RoleTable.css";

function RoleTable({ roles, pendingRoleId, onUpdateRole, onDeleteRole }) {
  if (roles.length === 0) {
    return (
      <div className="role-table__empty">
        Aucun rôle ne correspond à cette recherche.
      </div>
    );
  }

  return (
    <table className="role-table">
      <thead>
        <tr>
          <th>Rôle</th>
          <th>Permissions</th>
          <th aria-hidden="true" />
        </tr>
      </thead>
      <tbody>
        {roles.map((role) => {
          const isRowBusy = pendingRoleId === role.id;
          const permissionCount = (role.permissions || []).length;

          return (
            <tr key={role.id}>
              <td>
                <EditableRoleFields
                  name={role.name}
                  description={role.description}
                  disabled={isRowBusy}
                  onSave={(fields) => onUpdateRole(role.id, fields)}
                />
              </td>
              <td>
                <PermissionCountPill count={permissionCount} />
              </td>
              <td>
                <RoleRowActions
                  disabled={isRowBusy}
                  onDelete={() => onDeleteRole(role)}
                />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default RoleTable;
