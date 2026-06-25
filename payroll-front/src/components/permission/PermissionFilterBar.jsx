import "../../styles/permission/PermissionFilterBar.css";

function PermissionFilterBar({ searchTerm, onSearchChange }) {
  return (
    <div className="permission-filter-bar">
      <div className="permission-filter-bar__search">
        <svg
          className="permission-filter-bar__search-icon"
          viewBox="0 0 20 20"
          aria-hidden="true"
        >
          <path
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
            d="M13.5 13.5 17 17M9 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12Z"
          />
        </svg>
        <input
          type="text"
          className="permission-filter-bar__input"
          placeholder="Rechercher par nom (ex: EMPLOYEE, READ)…"
          value={searchTerm}
          onChange={(event) => onSearchChange(event.target.value)}
          aria-label="Rechercher une permission par nom"
        />
      </div>
    </div>
  );
}

export default PermissionFilterBar;
