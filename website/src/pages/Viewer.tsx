import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiBase, type PulseReport } from "../types";
import { getLocalReport } from "../localReport";
import Overview from "../viewer/Overview";
import Sampler from "../viewer/Sampler";
import Worlds from "../viewer/Worlds";
import Memory from "../viewer/Memory";
import Network from "../viewer/Network";
import Errors from "../viewer/Errors";

const tabs = ["Overview", "Sampler", "Worlds", "Memory", "Network", "Errors"] as const;

export default function Viewer() {
  const { id } = useParams();
  const [tab, setTab] = useState<(typeof tabs)[number]>("Overview");
  const [report, setReport] = useState<PulseReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setTab("Overview");
    setReport(null);
    setError(null);
    if (id === "local") {
      const local = getLocalReport();
      if (!local) setError("No local report loaded. Drop a file on the home page.");
      else setReport(local);
      return;
    }
    if (!id) return;
    const api = apiBase();
    if (!api) {
      setError("Drop a .pulse.json.gz file on the home page to open a report.");
      return;
    }
    fetch(`${api}/reports/${id}`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`Report ${id} was not found`);
        return (await res.json()) as PulseReport;
      })
      .then(setReport)
      .catch((e) => setError(e instanceof Error ? e.message : "Failed to load report"));
  }, [id]);

  if (error) {
    return (
      <div className="prose">
        <p className="err">{error}</p>
        <p>
          <Link to="/">Back home</Link>
        </p>
      </div>
    );
  }
  if (!report) return <p className="muted">Loading report…</p>;

  const when = new Date(report.createdAt).toLocaleString();

  return (
    <div className="viewer">
      <div className="viewer-meta">
        <div>
          <span className="kind">{report.kind}</span>
          <span className="meta">
            {when}
            {report.durationMs != null ? ` · ${Math.round(report.durationMs / 1000)}s` : ""}
            {report.platform?.serverName ? ` · ${report.platform.serverName}` : ""}
            {report.platform?.minecraft ? ` · MC ${report.platform.minecraft}` : ""}
            {report.platform?.java ? ` · Java ${report.platform.java}` : ""}
          </span>
        </div>
        <Link className="btn small" to="/">
          Open another
        </Link>
      </div>
      <div className="tabs">
        {tabs.map((name) => (
          <button key={name} className={tab === name ? "active" : ""} onClick={() => setTab(name)}>
            {name}
          </button>
        ))}
      </div>
      {tab === "Overview" ? <Overview report={report} /> : null}
      {tab === "Sampler" ? <Sampler report={report} /> : null}
      {tab === "Worlds" ? <Worlds report={report} /> : null}
      {tab === "Memory" ? <Memory report={report} /> : null}
      {tab === "Network" ? <Network report={report} /> : null}
      {tab === "Errors" ? <Errors report={report} /> : null}
    </div>
  );
}
