import type { PulseReport } from "../types";
import { fmtBytes, LineChart } from "./Charts";

export default function Network({ report }: { report: PulseReport }) {
  const n = report.network;
  if (!n) return <p className="muted">No network counters in this report.</p>;
  return (
    <div>
      <div className="grid">
        <div className="card">
          <h3>Packets in / out</h3>
          <p>
            {n.packetsIn} / {n.packetsOut}
          </p>
        </div>
        <div className="card">
          <h3>Bytes in / out</h3>
          <p>
            {fmtBytes(n.bytesIn)} / {fmtBytes(n.bytesOut)}
          </p>
        </div>
        <div className="card">
          <h3>Connections</h3>
          <p>{n.connections}</p>
        </div>
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <h3>Packets out</h3>
        <LineChart points={n.series || []} yKey="out" kind="netOut" />
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <h3>Packets in</h3>
        <LineChart points={n.series || []} yKey="in" kind="netIn" />
      </div>
      {report.disk ? (
        <div className="card" style={{ marginTop: 12 }}>
          <h3>Disk</h3>
          <p>
            {report.disk.path}: {fmtBytes(report.disk.freeBytes)} free of {fmtBytes(report.disk.totalBytes)}. World
            folder {fmtBytes(report.disk.worldBytes)}.
          </p>
        </div>
      ) : null}
    </div>
  );
}
