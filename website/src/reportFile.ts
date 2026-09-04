import type { PulseReport } from "./types";

export async function parsePulseFile(file: File): Promise<PulseReport> {
  const buf = new Uint8Array(await file.arrayBuffer());
  let bytes = buf;
  if (buf.length >= 2 && buf[0] === 0x1f && buf[1] === 0x8b) {
    const ds = new DecompressionStream("gzip");
    const stream = new Blob([buf]).stream().pipeThrough(ds);
    bytes = new Uint8Array(await new Response(stream).arrayBuffer());
  }
  const report = JSON.parse(new TextDecoder().decode(bytes)) as PulseReport;
  if (report.schemaVersion !== 1) {
    throw new Error("This file is not a Pulse v1 report");
  }
  return report;
}
