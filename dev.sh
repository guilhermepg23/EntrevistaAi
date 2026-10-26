#!/usr/bin/env bash
# Sobe o ambiente local inteiro com um comando: Postgres (Docker), backend
# (Maven, lendo o código direto, sem gerar imagem) e front (Vite).
#
#   ./dev.sh
#
# Ctrl+C derruba backend e front. O Postgres continua no Docker, pra próxima
# subida ser rápida; pra desligar: docker compose stop postgres.
#
# Variáveis: as do .env da raiz (as mesmas do docker-compose). O front lê o
# frontend/.env (VITE_API_URL=http://localhost:8080).
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Falta o .env na raiz. Copie o .env.example e preencha os valores." >&2
  exit 1
fi
set -a; . ./.env; set +a
export FRONTEND_BASE_URL="${FRONTEND_BASE_URL:-http://localhost:5173}"

# JAVA_HOME: usa o do ambiente; senão, o JDK 21 que o IntelliJ baixa em ~/.jdks.
if [ -z "${JAVA_HOME:-}" ]; then
  jdk=$(ls -d "$HOME"/.jdks/*21* 2>/dev/null | sort | tail -1 || true)
  if [ -n "$jdk" ]; then export JAVA_HOME="$jdk"; fi
fi
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi

porta_ocupada() { (echo > "/dev/tcp/127.0.0.1/$1") 2>/dev/null; }
for porta in 8080 5173; do
  if porta_ocupada "$porta"; then
    echo "A porta $porta já está em uso (backend ou front de outra execução?)." >&2
    echo "Veja quem é com: netstat -ano | grep \":$porta\"" >&2
    exit 1
  fi
done

echo "==> Postgres (Docker)"
docker compose up -d --wait postgres

pids=()
# Derruba a árvore de processos inteira: no Windows (Git Bash) o mvn e o npm
# abrem java/node como filhos, e um kill simples deixava esses vivos segurando
# as portas 8080 e 5173.
parar() {
  trap - INT TERM EXIT
  echo
  echo "==> Parando backend e front..."
  for pid in "${pids[@]}"; do
    if [ -r "/proc/$pid/winpid" ]; then
      taskkill //F //T //PID "$(cat "/proc/$pid/winpid")" > /dev/null 2>&1 || true
    else
      pkill -TERM -P "$pid" 2>/dev/null || true
      kill "$pid" 2>/dev/null || true
    fi
  done
  echo "Pronto. O Postgres segue no Docker (docker compose stop postgres para desligar)."
}
trap parar INT TERM EXIT

echo "==> Backend (mvn spring-boot:run)"
(cd backend && exec mvn -q -DskipTests spring-boot:run) > >(sed -u 's/^/[back] /') 2>&1 &
pids+=($!)

echo "==> Front (vite)"
(cd frontend && exec npm run dev -- --port 5173 --strictPort) > >(sed -u 's/^/[front] /') 2>&1 &
pids+=($!)

# Avisa quando o backend responde (o primeiro boot leva uns 30s).
for _ in $(seq 1 90); do
  if curl -s localhost:8080/health 2>/dev/null | grep -q UP; then
    echo
    echo "==> Tudo no ar: front em http://localhost:5173 (backend em http://localhost:8080). Ctrl+C para parar."
    break
  fi
  sleep 2
done

wait
