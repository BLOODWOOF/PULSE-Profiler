import type { StackNode } from "../types";

type Box = { x: number; y: number; w: number; name: string; samples: number; depth: number };

function layout(node: StackNode, x: number, y: number, w: number, out: Box[]) {
  if (w < 0.5 || node.samples <= 0) return;
  out.push({ x, y, w, name: node.name, samples: node.samples, depth: y });
  let cursor = x;
  const kids = node.children.slice().sort((a, b) => b.samples - a.samples);
  for (const child of kids) {
    const cw = (child.samples / node.samples) * w;
    layout(child, cursor, y + 1, cw, out);
    cursor += cw;
  }
}

const palette = ["#5a0000", "#8a0000", "#b40000", "#e10600", "#ff2a2a", "#ff4d4d", "#7a0000", "#c40000"];

export default function FlameGraph({ root }: { root: StackNode }) {
  const boxes: Box[] = [];
  const width = 1000;
  layout(root, 0, 0, width, boxes);
  const maxDepth = boxes.reduce((m, b) => Math.max(m, b.depth), 0);
  const row = 18;
  const height = (maxDepth + 2) * row;
  return (
    <div className="flame" style={{ marginTop: 14 }}>
      <svg viewBox={`0 0 ${width} ${height}`} width="100%" height={Math.min(height, 420)}>
        {boxes.map((b, i) => (
          <g key={i}>
            <title>{`${b.name} (${b.samples})`}</title>
            <rect
              x={b.x}
              y={b.depth * row}
              width={Math.max(b.w - 0.4, 0.4)}
              height={row - 1}
              fill={palette[b.depth % palette.length]}
              opacity={0.9}
            />
            {b.w > 40 ? (
              <text x={b.x + 4} y={b.depth * row + 13} fontSize="10" fill="#fff">
                {b.name.length > Math.floor(b.w / 7) ? `${b.name.slice(0, Math.floor(b.w / 7))}…` : b.name}
              </text>
            ) : null}
          </g>
        ))}
      </svg>
    </div>
  );
}
