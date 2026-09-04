export function fmtBytes(n: number | undefined) {
  if (n == null || Number.isNaN(n)) return "—";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let v = n;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i += 1;
  }
  return `${v.toFixed(v >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
}

export function fmtMs(n: number | undefined) {
  if (n == null) return "—";
  return `${n.toFixed(2)} ms`;
}

export type ChartKind = "mspt" | "cpu" | "heap" | "netOut" | "netIn";

const palettes: Record<ChartKind, { colors: [string, string, string]; labels: [string, string, string] }> = {
  mspt: {
    colors: ["#c8c8c8", "#ff7a18", "#e10600"],
    labels: ["under 25ms", "25–50ms", "over 50ms"],
  },
  cpu: {
    colors: ["#d4a017", "#ff7a18", "#e10600"],
    labels: ["under 50%", "50–80%", "over 80%"],
  },
  heap: {
    colors: ["#c8c8c8", "#e85d75", "#e10600"],
    labels: ["under 70%", "70–90%", "over 90%"],
  },
  netOut: {
    colors: ["#7a2a2a", "#e10600", "#ff2a2a"],
    labels: ["quiet", "busy", "peak"],
  },
  netIn: {
    colors: ["#5a2030", "#ff4d6d", "#ff8aa0"],
    labels: ["quiet", "busy", "peak"],
  },
};

function heat(kind: ChartKind, value: number, seriesMax: number, heatMax?: number): 0 | 1 | 2 {
  if (kind === "mspt") {
    if (value < 25) return 0;
    if (value < 50) return 1;
    return 2;
  }
  if (kind === "cpu") {
    if (value < 0.5) return 0;
    if (value < 0.8) return 1;
    return 2;
  }
  if (kind === "heap") {
    const cap = heatMax && heatMax > 0 ? heatMax : seriesMax;
    const pct = cap > 0 ? value / cap : 0;
    if (pct < 0.7) return 0;
    if (pct < 0.9) return 1;
    return 2;
  }
  const pct = seriesMax > 0 ? value / seriesMax : 0;
  if (pct < 0.4) return 0;
  if (pct < 0.75) return 1;
  return 2;
}

export function LineChart({
  points,
  yKey,
  kind = "mspt",
  heatMax,
}: {
  points: { t: number; [k: string]: number }[];
  yKey: string;
  kind?: ChartKind;
  heatMax?: number;
}) {
  if (!points.length) return <p className="muted">No samples.</p>;
  const w = 720;
  const h = 140;
  const palette = palettes[kind];
  const ys = points.map((p) => Number(p[yKey] || 0));
  const min = Math.min(...ys);
  const max = Math.max(...ys);
  const span = max - min || 1;
  const coords = points.map((p, i) => ({
    x: (i / Math.max(points.length - 1, 1)) * (w - 8) + 4,
    y: h - 8 - ((Number(p[yKey] || 0) - min) / span) * (h - 16),
    v: Number(p[yKey] || 0),
  }));

  const bands: { color: string; d: string }[] = [];
  for (let i = 1; i < coords.length; i++) {
    const level = heat(kind, Math.max(coords[i - 1].v, coords[i].v), max, heatMax);
    const color = palette.colors[level];
    const last = bands[bands.length - 1];
    const piece = `L${coords[i].x.toFixed(1)} ${coords[i].y.toFixed(1)}`;
    if (last && last.color === color) {
      last.d += piece;
    } else {
      bands.push({
        color,
        d: `M${coords[i - 1].x.toFixed(1)} ${coords[i - 1].y.toFixed(1)}${piece}`,
      });
    }
  }

  return (
    <div>
      <svg viewBox={`0 0 ${w} ${h}`} width="100%" height="140">
        {bands.map((b, i) => (
          <path key={i} d={b.d} fill="none" stroke={b.color} strokeWidth="2.2" strokeLinejoin="round" strokeLinecap="round" />
        ))}
        <text x="8" y="16" fill="#8a8a8a" fontSize="11">
          {max.toFixed(2)}
        </text>
        <text x="8" y={h - 6} fill="#8a8a8a" fontSize="11">
          {min.toFixed(2)}
        </text>
      </svg>
      <div className="chart-legend">
        {palette.labels.map((label, i) => (
          <span key={label}>
            <i style={{ background: palette.colors[i] }} />
            {label}
          </span>
        ))}
      </div>
    </div>
  );
}

export function Stat({ label, value, warn }: { label: string; value: string; warn?: boolean }) {
  return (
    <div className="card">
      <h3>{label}</h3>
      <p className={`stat-value${warn ? " warn" : ""}`}>{value}</p>
    </div>
  );
}
