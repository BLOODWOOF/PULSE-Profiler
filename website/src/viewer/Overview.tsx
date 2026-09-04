import type { PulseReport } from "../types";
import { fmtBytes, fmtMs, LineChart, Stat } from "./Charts";

export default function Overview({ report }: { report: PulseReport }) {
  const lastCpu = report.cpu?.samples.at(-1);
  const ticks = report.ticks;
  const lastHeap = report.memory?.series.at(-1);
  const heapPct =
    lastHeap && lastHeap.heapMax > 0 ? (lastHeap.heapUsed / lastHeap.heapMax) * 100 : 0;

  return (
    <div>
      <div className="grid">
        <Stat label="TPS" value={ticks ? ticks.tps.toFixed(2) : "—"} warn={!!ticks && ticks.tps < 19.5} />
        <Stat label="MSPT mean" value={fmtMs(ticks?.msptMean)} warn={!!ticks && ticks.msptMean > 40} />
        <Stat label="MSPT 99%" value={fmtMs(ticks?.msptP99)} />
        <Stat label="MSPT max" value={fmtMs(ticks?.msptMax)} warn={!!ticks && ticks.msptMax > 50} />
        <Stat label="Process CPU" value={lastCpu ? `${(lastCpu.process * 100).toFixed(1)}%` : "—"} />
        <Stat label="Heap" value={lastHeap ? `${fmtBytes(lastHeap.heapUsed)} / ${fmtBytes(lastHeap.heapMax)}` : "—"} warn={heapPct > 90} />
        <Stat label="Players" value={String(report.players?.length ?? "—")} />
        <Stat label="Sampler cost" value={report.sampler?.overheadMs != null ? `${report.sampler.overheadMs.toFixed(1)} ms` : "—"} />
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <h3>MSPT</h3>
        <LineChart points={ticks?.series || []} yKey="ms" kind="mspt" />
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <h3>Process CPU (0–1)</h3>
        <LineChart points={report.cpu?.samples || []} yKey="process" kind="cpu" />
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <h3>Heap used</h3>
        <LineChart
          points={report.memory?.series || []}
          yKey="heapUsed"
          kind="heap"
          heatMax={lastHeap?.heapMax}
        />
      </div>

      {report.platform?.mods?.length ? (
        <div className="card" style={{ marginTop: 12 }}>
          <h3>Loaded mods ({report.platform.mods.length})</h3>
          <table className="table">
            <thead>
              <tr>
                <th>id</th>
                <th>name</th>
                <th>version</th>
              </tr>
            </thead>
            <tbody>
              {report.platform.mods.map((m) => (
                <tr key={m.id}>
                  <td>
                    <code>{m.id}</code>
                  </td>
                  <td>{m.name}</td>
                  <td>{m.version}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
