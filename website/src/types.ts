export type ReportKind = "profiler" | "health" | "heap" | "errors";

export interface PlatformInfo {
  minecraft?: string;
  loader?: string;
  loaderVersion?: string;
  java?: string;
  jvm?: string;
  os?: string;
  arch?: string;
  cpus?: number;
  maxHeap?: number;
  jvmArgs?: string[];
  mods?: { id: string; version: string; name: string }[];
  brand?: string;
  serverName?: string;
  playerCount?: number;
  uptimeMs?: number;
}

export interface SeriesPoint {
  t: number;
  [key: string]: number;
}

export interface StackNode {
  name: string;
  samples: number;
  self: number;
  children: StackNode[];
  mod?: string;
}

export interface PulseReport {
  schemaVersion: 1;
  kind: ReportKind;
  createdAt: number;
  durationMs?: number;
  platform?: PlatformInfo;
  cpu?: {
    samples: { t: number; process: number; system: number; threads: number }[];
  };
  ticks?: {
    count: number;
    tps: number;
    msptMean: number;
    msptP95: number;
    msptP99: number;
    msptMax: number;
    series: { t: number; ms: number }[];
    spikes: { t: number; ms: number; stacks: { thread: string; frames: string[] }[] }[];
  };
  worlds?: {
    id: string;
    msptMean: number;
    chunks: number;
    entities: number;
    players: number;
    entityTypes: { type: string; count: number }[];
    chunkLoads: number;
    chunkUnloads: number;
    tickingBlockEntities?: number;
    difficulty?: string;
    dayTime?: number;
    raining?: boolean;
    thundering?: boolean;
  }[];
  players?: { name: string; ping: number; world: string; gameMode?: string }[];
  viewDistance?: number;
  simulationDistance?: number;
  memory?: {
    series: {
      t: number;
      heapUsed: number;
      heapCommitted: number;
      heapMax: number;
      nonHeap: number;
      metaspace: number;
      direct: number;
      mapped: number;
    }[];
  };
  gc?: {
    collectors: { name: string; count: number; timeMs: number }[];
    series: { t: number; pauseMs: number; name: string }[];
  };
  disk?: { path: string; totalBytes: number; freeBytes: number; worldBytes: number };
  network?: {
    packetsIn: number;
    packetsOut: number;
    bytesIn: number;
    bytesOut: number;
    connections: number;
    series: { t: number; in: number; out: number; bytesIn: number; bytesOut: number }[];
  };
  heapHistogram?: { className: string; instances: number; bytes: number }[];
  errors?: {
    at: number;
    level: string;
    logger: string;
    thread: string;
    message: string;
    stack: string;
    count: number;
    fingerprint: string;
  }[];
  sampler?: {
    intervalMs: number;
    threadDumps: number;
    overheadMs?: number;
    threadStates?: Record<string, { runnable: number; blocked: number; waiting: number; other: number }>;
    groups: Record<string, { samples: number; root: StackNode }>;
  };
}

export function apiBase(): string | null {
  const raw = import.meta.env.VITE_API_BASE;
  if (!raw) return null;
  return raw.replace(/\/$/, "");
}
