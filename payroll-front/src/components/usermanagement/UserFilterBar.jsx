import "../../styles/usermanagement/UserFilterBar.css";

function UserFilterBar({ searchTerm, onSearchChange, roleFilter, onRoleFilterChange, roleOptions }) {
  return (
    <div className="user-filter-bar">
      <div className="user-filter-bar__search">
        <svg
          className="user-filter-bar__search-icon"
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
          className="user-filter-bar__input"
          placeholder="Rechercher par nom ou email…"
          value={searchTerm}
          onChange={(event) => onSearchChange(event.target.value)}
          aria-label="Rechercher par nom ou email"
        />
      </div>

      <select
        className="user-filter-bar__role-select"
        value={roleFilter}
        onChange={(event) => onRoleFilterChange(event.target.value)}
        aria-label="Filtrer par rôle"
      >
        <option value="">Tous les rôles</option>
        {roleOptions.map((role) => (
          <option key={role.name} value={role.name}>
            {role.name.toLowerCase()}
          </option>
        ))}
      </select>
    </div>
  );
}

export default UserFilterBar;
