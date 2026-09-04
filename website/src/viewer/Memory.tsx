import type { PulseReport } from "../types";
import { fmtBytes, LineChart } from "./Charts";

export default function Memory({ report }: { report: PulseReport }) {
  const last = report.memory?.series.at(-1);
  const hist = report.heapHistogram || [];
  return (
    <div>
      <div className="grid">
        <div className="card">
          <h3>Heap</h3>
          <p>
            {fmtBytes(last?.heapUsed)} used / {fmtBytes(last?.heapCommitted)} committed / {fmtBytes(last?.heapMax)} max
          </p>
        </div>
        <div className="card">
          <h3>Non-heap</h3>
          <p>{fmtBytes(last?.nonHeap)}</p>
        </div>
        <div className="card">
          <h3>Metaspace</h3>
          <p>{fmtBytes(last?.metaspace)}</p>
        </div>
        <div className="card">
          <h3>Direct / mapped</h3>
          <p>
            {fmtBytes(last?.direct)} / {fmtBytes(last?.mapped)}
          </p>
        </div>
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <h3>Heap over time</h3>
        <LineChart
          points={report.memory?.series || []}
          yKey="heapUsed"
          kind="heap"
          heatMax={last?.heapMax}
        />
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <h3>GC collectors</h3>
        <table className="table">
          <thead>
            <tr>
              <th>name</th>
              <th>count</th>
              <th>time</th>
            </tr>
          </thead>
          <tbody>
            {(report.gc?.collectors || []).map((c) => (
              <tr key={c.name}>
                <td>{c.name}</td>
                <td>{c.count}</td>
                <td>{c.timeMs} ms</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {hist.length ? (
        <div className="card" style={{ marginTop: 12 }}>
          <h3>Class histogram</h3>
          <table className="table">
            <thead>
              <tr>
                <th>class</th>
                <th>instances</th>
                <th>bytes</th>
              </tr>
            </thead>
            <tbody>
              {hist.map((h) => (
                <tr key={h.className}>
                  <td>
                    <code>{h.className}</code>
                  </td>
                  <td>{h.instances}</td>
                  <td>{fmtBytes(h.bytes)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="muted">No histogram in this report.</p>
      )}
    </div>
  );
}
