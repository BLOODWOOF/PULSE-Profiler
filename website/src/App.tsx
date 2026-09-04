import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./layout/Layout";
import Home from "./pages/Home";
import Download from "./pages/Download";
import Docs from "./pages/Docs";
import Viewer from "./pages/Viewer";

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/download" element={<Download />} />
        <Route path="/docs" element={<Docs />} />
        <Route path="/r/:id" element={<Viewer />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
