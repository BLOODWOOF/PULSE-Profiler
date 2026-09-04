import { createServer } from "node:http";
import { mkdir, writeFile, readFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { gunzipSync, gzipSync } from "node:zlib";
import { randomBytes } from "node:crypto";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const dataDir = join(root, "data", "reports");
const port = Number(process.env.PORT || 8787);
const maxBytes = Number(process.env.MAX_BYTES || 25 * 1024 * 1024);
const corsOrigin = process.env.CORS_ORIGIN || "*";

const hits = new Map();

function cors(res) {
  res.setHeader("Access-Control-Allow-Origin", corsOrigin);
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type,Content-Encoding");
}

function limited(ip) {
  const now = Date.now();
  const row = hits.get(ip) || { n: 0, t: now };
  if (now - row.t > 60_000) {
    row.n = 0;
    row.t = now;
  }
  row.n += 1;
  hits.set(ip, row);
  return row.n > 30;
}

function id() {
  return randomBytes(6).toString("base64url");
}

function readBody(req, limit) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on("data", (c) => {
      size += c.length;
      if (size > limit) {
        reject(new Error("too large"));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

const server = createServer(async (req, res) => {
  cors(res);
  if (req.method === "OPTIONS") {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);

  try {
    if (req.method === "GET" && url.pathname === "/health") {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ ok: true }));
      return;
    }

    if (req.method === "POST" && url.pathname === "/reports") {
      const ip = req.socket.remoteAddress || "unknown";
      if (limited(ip)) {
        res.writeHead(429, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "rate limited" }));
        return;
      }

      let raw = await readBody(req, maxBytes);
      const encoding = String(req.headers["content-encoding"] || "");
      if (encoding.includes("gzip") || (raw.length >= 2 && raw[0] === 0x1f && raw[1] === 0x8b)) {
        raw = gunzipSync(raw);
      }

      const parsed = JSON.parse(raw.toString("utf8"));
      if (parsed.schemaVersion !== 1 || typeof parsed.kind !== "string") {
        res.writeHead(400, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "not a pulse v1 report" }));
        return;
      }

      await mkdir(dataDir, { recursive: true });
      const reportId = id();
      await writeFile(join(dataDir, `${reportId}.json.gz`), gzipSync(raw));
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ id: reportId }));
      return;
    }

    const get = url.pathname.match(/^\/reports\/([A-Za-z0-9_-]+)$/);
    if (req.method === "GET" && get) {
      const file = join(dataDir, `${get[1]}.json.gz`);
      if (!existsSync(file)) {
        res.writeHead(404, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "not found" }));
        return;
      }
      const json = gunzipSync(await readFile(file));
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(json);
      return;
    }

    res.writeHead(404, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "not found" }));
  } catch (err) {
    const message = err instanceof Error ? err.message : "error";
    const code = message === "too large" ? 413 : 400;
    res.writeHead(code, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: message }));
  }
});

await mkdir(dataDir, { recursive: true });
server.listen(port, () => {
  console.log(`pulse api on http://127.0.0.1:${port}`);
});
