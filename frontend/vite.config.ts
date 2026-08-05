import path from "node:path";
import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 백엔드 주소는 사람마다 다르다. 자기 PC 에서 백엔드를 띄우면 localhost 고,
  // 공용 서버를 쓰면 그 주소다. 코드에 박아두면 주소가 바뀔 때마다 커밋하고
  // 팀원 전원이 pull 을 받아야 하므로 .env.local 에서 읽는다
  // (*.local 은 .gitignore 에 걸려 커밋되지 않는다. .env.example 참고)
  const env = loadEnv(mode, __dirname, "VITE_");
  const apiTarget = env.VITE_API_TARGET || "http://localhost:60921";

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    server: {
      proxy: {
        "/api": {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
