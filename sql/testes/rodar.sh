#!/usr/bin/env bash
# Testa as consultas de sql/consultas num PostgreSQL descartável (Docker).
# Cada caso roda a consulta com parâmetros fixos e compara o CSV com sql/testes/esperado.
#
# Uso: sql/testes/rodar.sh          (precisa só do Docker)
set -euo pipefail
cd "$(dirname "$0")"

CONTAINER="sankhya-sql-teste-$$"
IMAGEM="postgres:16-alpine"
SAIDA="$(mktemp -d)"

docker run -d --rm --name "$CONTAINER" -e POSTGRES_PASSWORD=teste -e POSTGRES_DB=erp "$IMAGEM" >/dev/null
trap 'docker rm -f "$CONTAINER" >/dev/null 2>&1; rm -rf "$SAIDA"' EXIT

# O entrypoint sobe um servidor temporário, inicializa e reinicia: espera o definitivo.
for _ in $(seq 1 60); do
    if docker logs "$CONTAINER" 2>&1 | grep -q "PostgreSQL init process complete" \
        && docker exec "$CONTAINER" pg_isready -U postgres -d erp -q; then
        break
    fi
    sleep 1
done

psql() { docker exec -i "$CONTAINER" psql -U postgres -d erp -v ON_ERROR_STOP=1 -q "$@"; }

psql < dados/schema.sql
psql < dados/exemplo.sql

falhas=0
caso() {
    local nome="$1" consulta="$2"; shift 2
    psql --csv "$@" < "../consultas/$consulta.sql" > "$SAIDA/$nome.csv"
    if diff -u "esperado/$nome.csv" "$SAIDA/$nome.csv"; then
        echo "ok      $nome"
    else
        echo "FALHOU  $nome"
        falhas=$((falhas + 1))
    fi
}

REF=(-v "DATA_REF=DATE '2026-11-10'" -v DIAS_ALERTA=75 -v DIAS_CRITICO=90)
caso clientes_sem_movimentacao_todas_empresas clientes_sem_movimentacao "${REF[@]}" -v CODEMP=NULL
caso clientes_sem_movimentacao_empresa_2      clientes_sem_movimentacao "${REF[@]}" -v CODEMP=2

if [ "$falhas" -eq 0 ]; then echo "Todos os casos passaram."; else echo "$falhas caso(s) falharam."; fi
exit "$falhas"
