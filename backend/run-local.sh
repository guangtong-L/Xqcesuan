#!/usr/bin/env bash
# 本地启动后端（Bash / Git Bash）
# 端口：8080  ｜  profile：dev
set -euo pipefail

# 1) 自动定位 JDK
candidates=(
  "/d/JDK/jdk1.8.0_171/jdk1.8.0_171"
  "/d/JDK/jdk-17.0.12"
  "${JAVA_HOME:-}"
)
JAVA_HOME=""
for c in "${candidates[@]}"; do
  if [[ -n "$c" && -x "$c/bin/java.exe" ]]; then JAVA_HOME="$c"; break; fi
  if [[ -n "$c" && -x "$c/bin/java" ]]; then JAVA_HOME="$c"; break; fi
done
if [[ -z "$JAVA_HOME" ]]; then
  echo "未找到 JDK，请装 JDK 8/11 后再运行" >&2
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo "[run] JAVA_HOME=$JAVA_HOME"

cd "$(dirname "$0")"

# 2) 若 jar 不存在则打包
if [[ ! -f target/fortune-backend.jar ]]; then
  echo "[run] jar 不存在，先 mvn package"
  mvn -B -DskipTests package
fi

echo "[run] 启动 fortune-backend  端口=8080  profile=dev"
exec java -Duser.timezone=Asia/Shanghai -jar target/fortune-backend.jar \
  --server.port=8080 --spring.profiles.active=dev
