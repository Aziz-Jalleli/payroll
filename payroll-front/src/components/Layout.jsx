import React from 'react';
import { Sidebar } from './Sidebar';
import { useLocation, useNavigate } from 'react-router-dom';

export const Layout = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();

  const routeToItem = {
    '/admin/users':       'users',
    '/admin/permissions': 'permission',
    '/admin/roles':       'roles',
  };

  const activeItem = routeToItem[location.pathname] || null;

  const onNavigate = (itemId) => {
    const path = Object.keys(routeToItem).find((key) => routeToItem[key] === itemId);
    if (path) navigate(path);
  };

  return (
    <div className="layout">
      <Sidebar activeItem={activeItem} onNavigate={onNavigate} />
      <main className="layout__main">
        {children}
      </main>

      <style jsx>{`
        .layout {
          display: flex;
          min-height: 100vh;
          background: var(--color-bg);
        }

        .layout__main {
          margin-left: 260px;
          flex: 1;
          min-width: 0;
          overflow: auto;
        }

        @media (max-width: 1024px) {
          .layout {
            flex-direction: column;
          }
          .layout__main {
            margin-left: 0;
            padding-bottom: 60px;
          }
        }
      `}</style>
    </div>
  );
};