let current: import("./types").PulseReport | null = null;

export function setLocalReport(report: import("./types").PulseReport) {
  current = report;
}

export function getLocalReport() {
  return current;
}
