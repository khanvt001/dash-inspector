import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import './Layout.css';

export default function Layout() {
  const [drawerOpen, setDrawerOpen] = useState(false);

  const toggleDrawer = () => {
    setDrawerOpen(!drawerOpen);
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
  };

  return (
    <div className="layout">
      <header className="header">
        <button className="menu-button" onClick={toggleDrawer} aria-label="Toggle menu">
          <span className="menu-icon"></span>
        </button>
        <h1 className="header-title">DaShInspector</h1>
      </header>

      <div className={`drawer-overlay ${drawerOpen ? 'open' : ''}`} onClick={closeDrawer}></div>

      <nav className={`drawer ${drawerOpen ? 'open' : ''}`}>
        <div className="drawer-header">
          <h2>DaShInspector</h2>
        </div>
        <ul className="drawer-menu">
          <li>
            <NavLink
              to="/shared-preference"
              className={({ isActive }) => `drawer-link ${isActive ? 'active' : ''}`}
              onClick={closeDrawer}
            >
              <svg className="drawer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z" />
                <path d="M2 17l10 5 10-5" />
                <path d="M2 12l10 5 10-5" />
              </svg>
              SharedPreference
            </NavLink>
          </li>
          <li>
            <NavLink
              to="/database"
              className={({ isActive }) => `drawer-link ${isActive ? 'active' : ''}`}
              onClick={closeDrawer}
            >
              <svg className="drawer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <ellipse cx="12" cy="5" rx="9" ry="3" />
                <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
                <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
              </svg>
              Database
            </NavLink>
          </li>
        </ul>
      </nav>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
