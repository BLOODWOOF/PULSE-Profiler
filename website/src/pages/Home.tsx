import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { RELEASES } from "../links";
import { setLocalReport } from "../localReport";
import { parsePulseFile } from "../reportFile";
import type { PulseReport } from "../types";

export default function Home() {
  const nav = useNavigate();
  const input = useRef<HTMLInputElement>(null);
  const [err, setErr] = useState<string | null>(null);
  const [over, setOver] = useState(false);

  async function onFile(file: File) {
    setErr(null);
    try {
      const report = await parsePulseFile(file);
      setLocalReport(report);
      nav("/r/local");
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Could not read that file");
    }
  }

  return (
    <div>
      <section className="hero">
        <p className="kicker">Fabric 26.2 · dedicated servers</p>
        <h1>
          Diagnose lag, memory, and crashes.
        </h1>
        <p className="lede">
          Pulse samples CPU stacks, tick spikes, heap, GC, worlds, packets, and errors, then
          opens the result in this viewer.
        </p>
        <div className="hero-actions">
          <Link className="btn primary" to="/download">
            Get the mod
          </Link>
          <Link className="btn" to="/docs">
            Read the docs
          </Link>
          <button
            type="button"
            className="btn"
            onClick={async () => {
              try {
                const res = await fetch(`${import.meta.env.BASE_URL}sample.pulse.json`);
                const report = (await res.json()) as PulseReport;
                setLocalReport(report);
                nav("/r/local");
              } catch {
                setErr("Could not load the bundled sample report");
              }
            }}
          >
            Open a sample
          </button>
        </div>
      </section>

      <section className="features">
        <article>
          <h3>Sampler</h3>
          <p>Call trees, flame graphs, tick-spike stacks, and a server-thread-only mode for lighter sampling.</p>
        </article>
        <article>
          <h3>Health</h3>
          <p>TPS, MSPT percentiles, CPU, heap, GC, disk, players, weather, and per-world entity counts.</p>
        </article>
        <article>
          <h3>Memory</h3>
          <p>Heap over time, collector pauses, and a class histogram.</p>
        </article>
        <article>
          <h3>Worlds</h3>
          <p>Chunks, entities, block-entity tickers, loads/unloads, difficulty, and rain in one snapshot.</p>
        </article>
        <article>
          <h3>Network</h3>
          <p>Packet and byte counters with a quiet/busy/peak view of the sample window.</p>
        </article>
        <article>
          <h3>Errors</h3>
          <p>Deduped ERROR/FATAL lines and uncaught exceptions with stacks and hit counts.</p>
        </article>
      </section>

      <section className="steps">
        <h2>How it works</h2>
        <ol>
          <li>
            <strong>Install</strong>
            Drop the jar into the Fabric 26.2 <code>mods</code> folder.
          </li>
          <li>
            <strong>Sample</strong>
            Run <code>/pulse profiler start 30</code> as an operator. <code>/pulse tps</code> shows live windows anytime.
          </li>
          <li>
            <strong>Open</strong>
            Drop the saved <code>.pulse.json.gz</code> from <code>config/pulse/reports/</code> below.
          </li>
        </ol>
      </section>

      <section className="viewer-panel">
        <div className="viewer-copy">
          <h2>Viewer</h2>
          <p>
            This site opens Pulse reports. After a profile finishes, the file is under{" "}
            <code>config/pulse/reports/</code>. Drop it below.
          </p>
        </div>
        <div
          className={`drop${over ? " hot" : ""}`}
          onClick={() => input.current?.click()}
          onDragOver={(e) => {
            e.preventDefault();
            setOver(true);
          }}
          onDragLeave={() => setOver(false)}
          onDrop={(e) => {
            e.preventDefault();
            setOver(false);
            const file = e.dataTransfer.files[0];
            if (file) void onFile(file);
          }}
        >
          <strong>Drop a .pulse.json.gz file</strong>
          <span>or click to choose one</span>
          <input
            ref={input}
            type="file"
            hidden
            accept=".gz,.json,.pulse.json.gz,application/gzip,application/json"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void onFile(file);
            }}
          />
        </div>
        {err ? <p className="err">{err}</p> : null}
      </section>

      <p className="quiet">
        Need the mod? See <Link to="/download">download</Link> or{" "}
        <a href={RELEASES}>GitHub releases</a>.
      </p>
    </div>
  );
}
