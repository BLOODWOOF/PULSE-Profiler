import { useMemo, useState } from "react";
import type { PulseReport, StackNode } from "../types";
import FlameGraph from "./FlameGraph";

function matches(node: StackNode, q: string): boolean {
  if (!q) return true;
  if (node.name.toLowerCase().includes(q) || (node.mod || "").toLowerCase().includes(q)) return true;
  return node.children.some((c) => matches(c, q));
}

function NodeRow({
  node,
  total,
  depth,
  query,
}: {
  node: StackNode;
  total: number;
  depth: number;
  query: string;
}) {
  const [open, setOpen] = useState(depth < 2);
  if (!matches(node, query)) return null;
  const pct = total > 0 ? (node.samples / total) * 100 : 0;
  return (
    <div>
      <div className="tree-row" style={{ paddingLeft: depth * 12 }} onClick={() => setOpen(!open)}>
        <div className="bar">
          <i style={{ width: `${pct}%` }} />
        </div>
        <span className="muted" style={{ width: 72 }}>
          {pct.toFixed(1)}%
        </span>
        <span className="muted" style={{ width: 64 }}>
          {node.samples}
        </span>
        <span>
          {node.children.length ? (open ? "▾ " : "▸ ") : "  "}
          {node.name}
        </span>
        {node.mod ? <span className="muted">[{node.mod}]</span> : null}
      </div>
      {open
        ? node.children
            .slice()
            .sort((a, b) => b.samples - a.samples)
            .map((child) => (
              <NodeRow key={child.name} node={child} total={total} depth={depth + 1} query={query} />
            ))
        : null}
    </div>
  );
}

export default function Sampler({ report }: { report: PulseReport }) {
  const groups = report.sampler?.groups || {};
  const names = Object.keys(groups);
  const [group, setGroup] = useState(names[0] || "Server");
  const [query, setQuery] = useState("");
  const q = query.trim().toLowerCase();
  const selected = groups[group];
  const total = selected?.samples || 0;

  const spikes = report.ticks?.spikes || [];
  const flameRoot = useMemo(() => selected?.root, [selected]);

  if (!names.length) return <p className="muted">This report has no sampler data.</p>;

  return (
    <div>
      <p className="muted">
        interval {report.sampler?.intervalMs}ms · {report.sampler?.threadDumps} dumps
        {report.sampler?.overheadMs != null ? ` · sampler used ${report.sampler.overheadMs.toFixed(1)}ms` : ""}
        {report.sampler?.threadStates?.[group]
          ? ` · runnable ${report.sampler.threadStates[group].runnable} / blocked ${report.sampler.threadStates[group].blocked} / waiting ${report.sampler.threadStates[group].waiting}`
          : ""}
      </p>
      <div className="tabs">
        {names.map((name) => (
          <button key={name} className={group === name ? "active" : ""} onClick={() => setGroup(name)}>
            {name} ({groups[name].samples})
          </button>
        ))}
      </div>
      <input
        type="search"
        placeholder="Filter frames or mod id"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      {flameRoot ? <FlameGraph root={flameRoot} /> : null}
      {selected ? (
        <div style={{ marginTop: 12 }}>
          <NodeRow node={selected.root} total={total} depth={0} query={q} />
        </div>
      ) : null}

      {report.sampler?.lockWait && Object.keys(report.sampler.lockWait).length ? (
        <div className="card" style={{ marginTop: 12 }}>
          <h3>Lock waits</h3>
          <table className="table">
            <thead>
              <tr>
                <th>lock</th>
                <th>hits</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(report.sampler.lockWait)
                .sort((a, b) => b[1] - a[1])
                .map(([name, n]) => (
                  <tr key={name}>
                    <td>
                      <code>{name}</code>
                    </td>
                    <td>{n}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {spikes.length ? (
        <div className="card" style={{ marginTop: 16 }}>
          <h3>Tick spikes ({spikes.length})</h3>
          {spikes.map((s, i) => (
            <details key={i}>
              <summary>
                t={s.t}ms · {s.ms.toFixed(1)} ms
              </summary>
              {s.stacks.map((st, j) => (
                <pre className="stack" key={j}>
                  {st.thread}
                  {"\n"}
                  {st.frames.join("\n")}
                </pre>
              ))}
            </details>
          ))}
        </div>
      ) : null}
    </div>
  );
}
