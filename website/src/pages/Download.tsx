import { RELEASES, REPO } from "../links";

export default function Download() {
  return (
    <div className="prose">
      <p className="kicker">Install</p>
      <h1>Download</h1>
      <p className="lede">
        Pulse is a Fabric server mod for Minecraft 26.2. Grab the latest jar from GitHub Releases
        and drop it in <code>mods</code> next to Fabric API.
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
          Place it in the server <code>mods</code> folder. Client install is not required.
        </li>
        <li>
          Start the server once so it writes <code>config/pulse.json</code>.
        </li>
        <li>
          Point <code>uploadUrl</code> and <code>viewerBaseUrl</code> at your API and this site.
        </li>
      </ol>

      <h2>Build from source</h2>
      <p>
        Clone <a href={REPO}>{REPO}</a>, then in <code>mod/</code> run{" "}
        <code>gradlew build</code>. The remapped jar lands in <code>mod/build/libs</code>.
      </p>
    </div>
  );
}
