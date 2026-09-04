import { RELEASES } from "../links";

export default function Download() {
  return (
    <div className="prose">
      <p className="kicker">Install</p>
      <h1>Download</h1>
      <p className="lede">
        Pulse is a Fabric server mod for Minecraft 26.2. Put the jar in the server{" "}
        <code>mods</code> folder next to Fabric API.
      </p>

      <a className="btn primary" href={RELEASES} target="_blank" rel="noreferrer">
        Latest release
      </a>

      <h2>Requirements</h2>
      <ul>
        <li>Minecraft 26.2</li>
        <li>Fabric Loader 0.19.3 or newer</li>
        <li>Fabric API</li>
        <li>Java 25</li>
      </ul>

      <h2>Install</h2>
      <ol>
        <li>
          Download <code>pulse-*.jar</code> from{" "}
          <a href={RELEASES}>releases</a>.
        </li>
        <li>
          Place it in the server <code>mods</code> folder. It does not need to be on the client.
        </li>
        <li>Restart the server.</li>
        <li>
          Profile with <code>/pulse profiler start</code>, then drop the saved report from{" "}
          <code>config/pulse/reports/</code> on the home page.
        </li>
      </ol>
    </div>
  );
}
