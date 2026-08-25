import { Link, NavLink, Outlet } from 'react-router-dom'

export function Layout() {
  return (
    <div className="app">
      <nav className="nav">
        <Link className="nav-brand" to="/">
          Job <span>Helper</span>
        </Link>
        <div className="nav-links">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Home
          </NavLink>
          <NavLink to="/jobs" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Jobs
          </NavLink>
          <NavLink to="/jobs/sources" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Sources
          </NavLink>
          <NavLink to="/roadmap" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Roadmap
          </NavLink>
          <NavLink to="/leetcode" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            LeetCode
          </NavLink>
          <NavLink to="/cv" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            CV
          </NavLink>
          <NavLink to="/behavioral" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Behavioral
          </NavLink>
        </div>
        <NavLink
          to="/profile"
          className={({ isActive }) => `nav-avatar${isActive ? ' active' : ''}`}
          title="Profile"
          aria-label="Profile"
        >
          P
        </NavLink>
      </nav>
      <Outlet />
    </div>
  )
}
