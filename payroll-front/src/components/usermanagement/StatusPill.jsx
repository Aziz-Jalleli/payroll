import "../../styles/usermanagement/StatusPill.css";

function StatusPill({ enabled }) {
  return (
    <span className={`status-pill ${enabled ? "status-pill--active" : "status-pill--inactive"}`}>
      {enabled ? "Actif" : "Inactif"}
    </span>
  );
}

export default StatusPill;
