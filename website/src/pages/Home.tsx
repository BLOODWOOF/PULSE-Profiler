import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { PulseReport } from "../types";
import { setLocalReport } from "../localReport";

async function parseFile(file: File): Promise<PulseReport> {
  const buf = new Uint8Array(await file.arrayBuffer());
  let bytes = buf;
  if (buf.length >= 2 && buf[0] === 0x1f && buf[1] === 0x8b) {
    const ds = new DecompressionStream("gzip");
    const stream = new Blob([buf]).stream().pipeThrough(ds);
    bytes = new Uint8Array(await new Response(stream).arrayBuffer());
  }
  const report = JSON.parse(new TextDecoder().decode(bytes)) as PulseReport;
  if (report.schemaVersion !== 1) {
    throw new Error("This file is not a Pulse v1 report");
  }
  return report;
}

export default function Home() {
  const nav = useNavigate();
  const input = useRef<HTMLInputElement>(null);
  const [err, setErr] = useState<string | null>(null);

  async function onFile(file: File) {
    setErr(null);
    try {
      const report = await parseFile(file);
      setLocalReport(report);
      nav("/r/local");
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Could not read that file");
    }
  }

  return (
    <div>
      <section className="hero">
        <h1>See what the server is actually doing</h1>
        <p>
          Pulse goes further than a basic profiler: per-world weather and block entities, tick
          spikes, heap histograms, packet counters, and a configurable sampler that can stay on
          the server thread when you need it cheap. Run a command in-game, then open the link or
          drop the saved report here.
        </p>
      </section>

      <div
        className="drop"
        onClick={() => input.current?.click()}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          const file = e.dataTransfer.files[0];
          if (file) void onFile(file);
        }}
      >
        <strong>Drop a .pulse.json.gz file</strong>
        <div>or click to choose one</div>
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

      <p>
        <button
          type="button"
          className="chip"
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
          Open sample report
        </button>
      </p>

      <div className="grid">
        <article className="card">
          <h3>Profiler</h3>
          <p>
            <code>/pulse profiler start 30</code> samples every thread, then uploads a
            shareable report when it stops.
          </p>
        </article>
        <article className="card">
          <h3>Health</h3>
          <p>
            TPS, MSPT percentiles, CPU, heap, GC, disk, players, and per-world entity
            counts without waiting on a long sample.
          </p>
        </article>
        <article className="card">
          <h3>Memory and errors</h3>
          <p>
            Class histograms, GC pauses, and a deduped ERROR/FATAL log so crashes and
            leaks are easier to pin down.
          </p>
        </article>
      </div>
    </div>
  );
}
