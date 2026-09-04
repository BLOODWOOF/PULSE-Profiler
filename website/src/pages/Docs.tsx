export default function Docs() {
  return (
    <div className="prose">
      <p className="kicker">Reference</p>
      <h1>Docs</h1>
      <p className="lede">
        Operator commands, config, and how reports get from the server to this viewer.
      </p>

      <h2>Commands</h2>
      <p>All of these need operator permission (game masters).</p>
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
            <td>Start sampling. Defaults come from config (30s / 10ms).</td>
          </tr>
          <tr>
            <td>
              <code>/pulse profiler stop</code>
            </td>
            <td>Stop, save, upload, print a viewer link.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse health</code>
            </td>
            <td>Metrics snapshot without a long sampler run.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse heap</code>
            </td>
            <td>Class histogram plus memory snapshot.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse errors</code>
            </td>
            <td>Recent ERROR/FATAL and uncaught exceptions.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse tps</code>
            </td>
            <td>Live TPS / MSPT line in chat.</td>
          </tr>
          <tr>
            <td>
              <code>/pulse reload</code>
            </td>
            <td>Reload <code>config/pulse.json</code>.</td>
          </tr>
        </tbody>
      </table>

      <h2>Config</h2>
      <p>
        After first launch, edit <code>config/pulse.json</code>. Defaults are written back on load
        so new knobs show up when you update the mod.
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
              <code>uploadUrl</code>
            </td>
            <td>POST endpoint for gzip JSON reports.</td>
          </tr>
          <tr>
            <td>
              <code>viewerBaseUrl</code>
            </td>
            <td>This website origin, used to build chat links.</td>
          </tr>
          <tr>
            <td>
              <code>sampleOnlyServerThread</code>
            </td>
            <td>Cheaper sampler: only dump the server thread.</td>
          </tr>
          <tr>
            <td>
              <code>includeWaitingThreads</code>
            </td>
            <td>Include WAITING / TIMED_WAITING stacks.</td>
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
            <td>Walk the world folder to estimate disk use (slow).</td>
          </tr>
        </tbody>
      </table>

      <h2>Hosting this site</h2>
      <p>
        The viewer is a static Vite app. GitHub Pages can host it. Uploads need a small API you
        run yourself (<code>api/</code> in the repo, default port 8787).
      </p>
      <ol>
        <li>
          Set the Actions variable <code>VITE_API_BASE</code> to your API origin.
        </li>
        <li>
          Enable GitHub Pages from the Actions workflow.
        </li>
        <li>
          In <code>pulse.json</code>, set <code>viewerBaseUrl</code> to the Pages URL and{" "}
          <code>uploadUrl</code> to <code>https://your-api/reports</code>.
        </li>
      </ol>
      <p>
        If the API is down, reports still save under <code>config/pulse/reports/</code> when{" "}
        <code>saveLocal</code> is true. Drop those files on the home page.
      </p>
    </div>
  );
}
