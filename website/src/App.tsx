import { Link, Navigate, Route, Routes } from "react-router-dom";
import Home from "./pages/Home";
import Viewer from "./pages/Viewer";

export default function App() {
  return (
    <div className="shell">
      <header className="top">
        <Link className="brand" to="/">
          <span className="pulse">PULSE</span> <span className="mark">profiler</span>
        </Link>
        <span className="muted">Fabric server diagnostics</span>
      </header>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/r/:id" element={<Viewer />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
