import "../styles/PermissionPill.css";

function PermissionPill({ label, tone = "neutral" }) {
  return <span className={`permission-pill permission-pill--${tone}`}>{label}</span>;
}

export default PermissionPill;
