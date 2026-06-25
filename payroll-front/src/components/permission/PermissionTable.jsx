import PermissionNamePill from "./PermissionNamePill";
import RoleAssignment from "./RoleAssignment";
import PermissionRowActions from "./PermissionRowActions";
import "../../styles/permission/PermissionTable.css";

function PermissionTable({
  permissions,
  roles,
  pendingPermissionId,
  onAssignRole,
  onUnassignRole,
  onDeletePermission,
}) {
  if (permissions.length === 0) {
    return (
      <div className="permission-table__empty">
        Aucune permission ne correspond à cette recherche.
      </div>
    );
  }

  return (
    <table className="permission-table">
      <thead>
        <tr>
          <th>Permission</th>
          <th>Description</th>
          <th>Rôles</th>
          <th aria-hidden="true" />
        </tr>
      </thead>
      <tbody>
        {permissions.map((permission) => {
          const isRowBusy = pendingPermissionId === permission.id;
          const assignedRoles = roles.filter((role) =>
            (role.permissions || []).some((p) => p.id === permission.id)
          );
          const availableRoles = roles.filter(
            (role) => !assignedRoles.some((r) => r.id === role.id)
          );

          return (
            <tr key={permission.id}>
              <td>
                <PermissionNamePill resource={permission.resource} action={permission.action} />
              </td>
              <td className="permission-table__description">
                {permission.description || "—"}
              </td>
              <td>
                <RoleAssignment
                  assignedRoles={assignedRoles}
                  availableRoles={availableRoles}
                  disabled={isRowBusy}
                  onAssign={(roleId) => onAssignRole(permission.id, roleId)}
                  onUnassign={(roleId) => onUnassignRole(permission.id, roleId)}
                />
              </td>
              <td>
                <PermissionRowActions
                  disabled={isRowBusy}
                  onDelete={() => onDeletePermission(permission)}
                />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default PermissionTable;
