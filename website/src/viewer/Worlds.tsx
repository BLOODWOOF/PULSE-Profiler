import type { PulseReport } from "../types";

export default function Worlds({ report }: { report: PulseReport }) {
  const worlds = report.worlds || [];
  if (!worlds.length) return <p className="muted">No world snapshots in this report.</p>;
  return (
    <div>
      <table className="table">
        <thead>
          <tr>
            <th>World</th>
            <th>MSPT</th>
            <th>Chunks</th>
            <th>Entities</th>
            <th>Players</th>
            <th>Loads</th>
            <th>Unloads</th>
            <th>Block entities</th>
            <th>Weather</th>
          </tr>
        </thead>
        <tbody>
          {worlds.map((w) => (
            <tr key={w.id}>
              <td>
                <code>{w.id}</code>
              </td>
              <td>{w.msptMean.toFixed(2)}</td>
              <td>{w.chunks}</td>
              <td>{w.entities}</td>
              <td>{w.players}</td>
              <td>{w.chunkLoads}</td>
              <td>{w.chunkUnloads}</td>
              <td>{w.tickingBlockEntities ?? "—"}</td>
              <td>
                {w.raining ? "rain" : "clear"}
                {w.thundering ? " / thunder" : ""}
                {w.difficulty ? ` · ${w.difficulty}` : ""}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {worlds.map((w) => (
        <div className="card" key={`${w.id}-types`} style={{ marginTop: 12 }}>
          <h3>{w.id} entity types</h3>
          <table className="table">
            <thead>
              <tr>
                <th>type</th>
                <th>count</th>
              </tr>
            </thead>
            <tbody>
              {w.entityTypes.map((e) => (
                <tr key={e.type}>
                  <td>
                    <code>{e.type}</code>
                  </td>
                  <td>{e.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
      {report.players?.length ? (
        <div className="card" style={{ marginTop: 12 }}>
          <h3>Players</h3>
          <table className="table">
            <thead>
              <tr>
                <th>name</th>
                <th>ping</th>
                <th>world</th>
                <th>mode</th>
              </tr>
            </thead>
            <tbody>
              {report.players.map((p) => (
                <tr key={p.name}>
                  <td>{p.name}</td>
                  <td>{p.ping}</td>
                    <td>
                    <code>{p.world}</code>
                  </td>
                  <td>{p.gameMode || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
