import React, { useEffect, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { logout as logoutService, getCurrentUser } from '../services/authService';

import {
  faUser,
  faKey,
  faRightFromBracket,
  faClock,
  faShieldHalved,
  faMoneyBillWave,
} from '@fortawesome/free-solid-svg-icons';
import { useNavigate } from 'react-router-dom';
import AvoCarbonLogo from './AvoCarbonLogo';

export const Sidebar = ({ activeItem, onNavigate }) => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const fetchUser = async () => {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    };
    fetchUser();
  }, []);

  const menuItems = [
    { id: 'users',      label: 'Utilisateurs',       icon: faUser },
    { id: 'roles',      label: 'Rôles',      icon: faShieldHalved  },
    { id: 'permission', label: 'Permissions', icon: faKey  },
    { id: 'pointage',   label: 'Pointage',   icon: faClock  },
    { id: 'allowances', label: 'Primes',     icon: faMoneyBillWave  },
    
  ];

  const handleSignOut = async () => {
    try {
      await logoutService(); // calls backend /logout
    } catch (error) {
      console.error("Logout failed:", error);
    } finally {
      localStorage.removeItem('token'); // optional fallback cleanup
      navigate('/login');
    }
  };
  const visibleItems = menuItems.filter(item => {
    if (!item.permission) return true;

    const [resource, action] = item.permission.split(':');
    console.log("Checking permission for", resource, action, "user:", user);
    return hasPermission(resource, action);
  });

  return (
    <>
      <aside className="sidebar">
        <div className="sidebar__logo">
          <AvoCarbonLogo width={160} height={64} />
        </div>

        <nav className="sidebar__nav">
          {visibleItems.map(item => {
            const isActive = activeItem === item.id;

            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={`sidebar__item${isActive ? ' sidebar__item--active' : ''}`}
              >
                <FontAwesomeIcon icon={item.icon} className="sidebar__icon" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <button onClick={handleSignOut} className="sidebar__signout">
          <FontAwesomeIcon icon={faRightFromBracket} />
          <span>Sign Out</span>
        </button>
      </aside>

      <style jsx>{`
        .sidebar {
          width: 260px;
          height: 100vh;
          background-color: var(--color-bg-elevated);
          border-right: 1px solid var(--color-border);
          box-shadow: var(--shadow-card);
          display: flex;
          flex-direction: column;
          position: fixed;
          left: 0;
          top: 0;
          z-index: 100;
          transition: transform var(--duration-base) var(--ease-standard);
        }

        .sidebar__logo {
          display: flex;
          align-items: center;
          padding: var(--space-4) var(--space-5);
          border-bottom: 1px solid var(--color-border);
        }

        .sidebar__nav {
          flex: 1;
          padding: var(--space-4) var(--space-3);
          display: flex;
          flex-direction: column;
          gap: var(--space-1);
          overflow-y: auto;
        }

        .sidebar__item {
          width: 100%;
          display: flex;
          align-items: center;
          gap: var(--space-3);
          padding: var(--space-3) var(--space-4);
          border: none;
          border-radius: var(--radius-md);
          background: transparent;
          color: var(--color-text-secondary);
          font-size: var(--fs-sm);
          font-weight: var(--fw-medium);
          font-family: var(--font-ui);
          cursor: pointer;
          text-align: left;
          transition:
            background-color var(--duration-fast) var(--ease-standard),
            color var(--duration-fast) var(--ease-standard);
        }

        .sidebar__item:hover {
          background-color: var(--color-slate-light);
          color: var(--color-navy);
        }

        .sidebar__item--active {
          background-color: var(--color-slate-light);
          color: var(--color-navy);
          font-weight: var(--fw-semibold);
        }

        .sidebar__icon {
          font-size: 15px;
          color: var(--color-text-muted);
          flex-shrink: 0;
          width: 16px;
          transition: color var(--duration-fast) var(--ease-standard);
        }

        .sidebar__item:hover .sidebar__icon,
        .sidebar__item--active .sidebar__icon {
          color: var(--color-slate);
        }

        .sidebar__item:focus-visible {
          outline: none;
          box-shadow: var(--shadow-focus);
        }

        .sidebar__signout {
          display: flex;
          align-items: center;
          gap: var(--space-3);
          width: calc(100% - var(--space-6));
          margin: 0 var(--space-3) var(--space-4);
          padding: var(--space-3) var(--space-4);
          border: none;
          border-top: 1px solid var(--color-border);
          border-radius: var(--radius-md);
          background: transparent;
          color: var(--color-error);
          font-size: var(--fs-sm);
          font-weight: var(--fw-medium);
          font-family: var(--font-ui);
          cursor: pointer;
          transition: background-color var(--duration-fast) var(--ease-standard);
        }

        .sidebar__signout:hover {
          background-color: var(--color-error-bg);
        }

        @media (max-width: 1024px) {
          .sidebar {
            width: 100%;
            height: 60px;
            top: auto;
            bottom: 0;
            flex-direction: row;
            align-items: center;
            border-right: none;
            border-top: 1px solid var(--color-border);
            padding: 0 var(--space-2);
          }

          .sidebar__logo {
            display: none;
          }

          .sidebar__nav {
            flex-direction: row;
            padding: 0;
            gap: 0;
            justify-content: space-around;
            width: 100%;
          }

          .sidebar__item {
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: var(--space-1) var(--space-2);
            font-size: var(--fs-xs);
            gap: 4px;
            flex: 1;
          }

          .sidebar__signout {
            display: none;
          }
        }
      `}</style>
    </>
  );
};