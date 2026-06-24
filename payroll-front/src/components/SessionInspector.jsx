import PermissionPill from "../components/PermissionPill";
import "../styles/SessionInspector.css";

function SessionInspector({ user, onLogout }) {
  const roles = user.roles ? Array.from(user.roles) : [];
  const permissions = user.permissions ? Array.from(user.permissions) : [];

  return (
    <div className="session-inspector">
      <div className="session-inspector__header">
        <div className="session-inspector__identity">
          <span className="session-inspector__status-dot" aria-hidden="true" />
          <div>
            <p className="session-inspector__name">{user.fullName || user.email}</p>
            <p className="session-inspector__email">{user.email}</p>
          </div>
        </div>
        <button type="button" className="session-inspector__logout" onClick={onLogout}>
          End session
        </button>
      </div>

      <div className="session-inspector__section">
        <p className="session-inspector__label">
          Roles <span className="session-inspector__count">{roles.length}</span>
        </p>
        <div className="session-inspector__pills">
          {roles.length === 0 ? (
            <PermissionPill label="no roles assigned" tone="empty" />
          ) : (
            roles.map((role) => <PermissionPill key={role} label={role} tone="role" />)
          )}
        </div>
      </div>

      <div className="session-inspector__section">
        <p className="session-inspector__label">
          Permissions <span className="session-inspector__count">{permissions.length}</span>
        </p>
        <div className="session-inspector__pills">
          {permissions.length === 0 ? (
            <PermissionPill label="none granted" tone="empty" />
          ) : (
            permissions.map((perm) => (
              <PermissionPill key={perm} label={perm} tone="neutral" />
            ))
          )}
        </div>
      </div>

      {roles.length === 0 && (
        <p className="session-inspector__hint">
          This account authenticated successfully but has no roles yet. Ask an
          admin to assign one via{" "}
          <code>PUT /api/admin/users/{user.id}/roles</code>.
        </p>
      )}
    </div>
  );
}

export default SessionInspector;
