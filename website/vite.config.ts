import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

function pagesBase() {
  if (process.env.GITHUB_PAGES !== "true") {
    return "/";
  }
  const repo = process.env.GITHUB_REPOSITORY?.split("/")[1];
  return repo ? `/${repo}/` : "/PULSE-Profiler/";
}

export default defineConfig({
  plugins: [react()],
  base: pagesBase(),
});
