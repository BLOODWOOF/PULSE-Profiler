export default function Docs() {
  return (
    <div className="prose">
      <p className="kicker">Reference</p>
      <h1>Docs</h1>
      <p className="lede">Commands and config for running Pulse on a Fabric server.</p>

      <h2>Commands</h2>
      <p>These require operator permission.</p>
      <table className="table">
        <thead>
          <tr>
            <th>Command</th>
            <th>What it does</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <code>/pulse profiler start [seconds] [intervalMs]</code>
            </td>
            <td>Start sampling. Defaults are 30 seconds at 10ms unless changed in config.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse profiler stop</code>
            </td>
            <td>Stop and save a report.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse health</code>
            </td>
            <td>Snapshot of TPS, memory, worlds, and similar.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse heap</code>
            </td>
            <td>Class histogram plus a memory snapshot.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse errors</code>
            </td>
            <td>Recent errors and uncaught exceptions.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse tps</code>
            </td>
            <td>Live TPS / MSPT in chat.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse reload</code>
            </td>
            <td>
              Reload <code>config/pulse.json</code>.
            </td>
          </tr>
        </tbody>
      </table>

      <h2>Config</h2>
      <p>
        <code>config/pulse.json</code> is created the first time the server starts with Pulse installed.
      </p>
      <table className="table">
        <thead>
          <tr>
            <th>Key</th>
            <th>Meaning</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <code>sampleOnlyServerThread</code>
            </td>
            <td>Only sample the server thread.</td>
          </tr>
          <tr>
            <td>
              <code>includeWaitingThreads</code>
            </td>
            <td>Include waiting threads in the sample.</td>
          </tr>
          <tr>
            <td>
              <code>actionBarHud</code>
            </td>
            <td>Show TPS / MSPT on the action bar for operators.</td>
          </tr>
          <tr>
            <td>
              <code>scanWorldSize</code>
            </td>
            <td>Estimate world folder size. This can hitch the save.</td>
          </tr>
          <tr>
            <td>
              <code>saveLocal</code>
            </td>
            <td>
              Keep a copy under <code>config/pulse/reports/</code>. Those files can be dropped on the home page.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
