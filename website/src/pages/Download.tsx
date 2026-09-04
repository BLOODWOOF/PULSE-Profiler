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
      <div className="req-grid">
        <article>
          <span>Game</span>
          <strong>Minecraft 26.2</strong>
        </article>
        <article>
          <span>Loader</span>
          <strong>Fabric 0.19.3+</strong>
        </article>
        <article>
          <span>API</span>
          <strong>Fabric API</strong>
        </article>
        <article>
          <span>Runtime</span>
          <strong>Java 25</strong>
        </article>
      </div>

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
