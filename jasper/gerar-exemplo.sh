#!/usr/bin/env bash
# Gera jasper/exemplos/*.pdf e *.png com os dados fictícios de sql/testes/dados.
# Sobe um PostgreSQL descartável em Docker, preenche o relatório e remove o banco no fim.
#
# Uso: jasper/gerar-exemplo.sh      (precisa de Docker e de um JDK 11 ou mais novo)
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
CONTAINER="sankhya-jasper-exemplo-$$"
PORTA=55432

# Porta publicada só no loopback: o banco de exemplo não fica acessível pela rede.
docker run -d --rm --name "$CONTAINER" -p "127.0.0.1:$PORTA:5432" \
    -e POSTGRES_PASSWORD=teste -e POSTGRES_DB=erp postgres:16-alpine >/dev/null
trap 'docker rm -f "$CONTAINER" >/dev/null 2>&1' EXIT

for _ in $(seq 1 60); do
    if docker logs "$CONTAINER" 2>&1 | grep -q "PostgreSQL init process complete" \
        && docker exec "$CONTAINER" pg_isready -U postgres -d erp -q; then
        break
    fi
    sleep 1
done

for arquivo in schema exemplo; do
    docker exec -i "$CONTAINER" psql -U postgres -d erp -v ON_ERROR_STOP=1 -q \
        < "$RAIZ/sql/testes/dados/$arquivo.sql"
done

cd "$RAIZ"
DB_URL="jdbc:postgresql://localhost:$PORTA/erp" ./gradlew :jasper:gerador:run --console=plain
