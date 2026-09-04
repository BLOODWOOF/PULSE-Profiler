import { useState } from "react";
import type { PulseReport } from "../types";

export default function Errors({ report }: { report: PulseReport }) {
  const errors = report.errors || [];
  const [open, setOpen] = useState<string | null>(null);
  if (!errors.length) return <p className="ok">No errors captured in this window.</p>;
  return (
    <div>
      <table className="table">
        <thead>
          <tr>
            <th>count</th>
            <th>level</th>
            <th>logger</th>
            <th>message</th>
            <th>thread</th>
          </tr>
        </thead>
        <tbody>
          {errors.map((e) => (
            <tr key={e.fingerprint} onClick={() => setOpen(open === e.fingerprint ? null : e.fingerprint)}>
              <td>{e.count}</td>
              <td className={e.level === "ERROR" || e.level === "FATAL" ? "bad" : "warn"}>{e.level}</td>
              <td>
                <code>{e.logger}</code>
              </td>
              <td>{e.message}</td>
              <td>{e.thread}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {errors
        .filter((e) => e.fingerprint === open)
        .map((e) => (
          <pre className="stack" key={e.fingerprint}>
            {e.stack || "(no stack)"}
          </pre>
        ))}
    </div>
  );
}
