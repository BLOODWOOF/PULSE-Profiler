import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const pages = process.env.GITHUB_PAGES === "true";

export default defineConfig({
  plugins: [react()],
  base: pages ? "/pulse/" : "/",
});
