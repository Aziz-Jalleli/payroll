import "../../styles/permission/PermissionNamePill.css";

function PermissionNamePill({ resource, action }) {
  return (
    <span className="permission-name-pill">
      {resource}
      <span className="permission-name-pill__separator">:</span>
      {action}
    </span>
  );
}

export default PermissionNamePill;
