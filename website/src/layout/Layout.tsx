import type { ReactNode } from "react";
import { Link, NavLink } from "react-router-dom";
import { ISSUES, REPO } from "../links";

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="page">
      <header className="nav">
        <Link className="brand" to="/">
          <img className="brand-icon" src={`${import.meta.env.BASE_URL}icon.png`} width={28} height={28} alt="" />
          <span className="pulse">PULSE</span>
          <span className="mark">profiler</span>
        </Link>
        <nav className="nav-links">
          <NavLink to="/" end>
            Home
          </NavLink>
          <NavLink to="/download">Download</NavLink>
          <NavLink to="/docs">Docs</NavLink>
          <a href={REPO} target="_blank" rel="noreferrer">
            GitHub
          </a>
        </nav>
      </header>
      <main className="main">{children}</main>
      <footer className="foot">
        <span>
          MIT · BLOODWOLF · <a href={REPO}>source</a>
        </span>
        <span>
          <a href={ISSUES}>issues</a>
        </span>
      </footer>
    </div>
  );
}
