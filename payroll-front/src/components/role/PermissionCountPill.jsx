import "../../styles/role/PermissionCountPill.css";

function PermissionCountPill({ count }) {
  if (count === 0) {
    return <span className="permission-count-pill permission-count-pill--empty">Aucune</span>;
  }
  return (
    <span className="permission-count-pill">
      {count} permission{count > 1 ? "s" : ""}
    </span>
  );
}

export default PermissionCountPill;
