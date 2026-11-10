# SQL: consultas para o Sankhya

## Clientes sem movimentação

[`consultas/clientes_sem_movimentacao.sql`](consultas/clientes_sem_movimentacao.sql)

Lista os clientes ativos que estão há muito tempo sem comprar, para a equipe comercial agir antes de perder o cliente. Cada cliente aparece uma vez, com a última venda, o vendedor e a faixa:

| Faixa | Regra |
|---|---|
| CRITICO | mais de `:DIAS_CRITICO` dias sem comprar |
| ALERTA | de `:DIAS_ALERTA` a `:DIAS_CRITICO` dias (inclusive) |
| SEM COMPRAS | nenhuma venda confirmada até a data de referência (na empresa filtrada, se houver filtro) |

**Regras de negócio:**
- Só conta a nota de venda (`TIPMOV = 'V'`) confirmada (`STATUSNOTA = 'L'`). Pedido pendente, devolução e nota com data depois da referência não contam.
- Duas vendas no mesmo dia: vale a última lançada (maior `NUNOTA`).
- Com `:CODEMP`, só as vendas daquela empresa contam.

**Técnica:**
- CTE para separar a regra em etapas legíveis (vendas válidas, situação de cada cliente, faixa).
- `ROW_NUMBER() OVER (PARTITION BY ...)` para pegar a última venda de cada cliente. Ele substitui o `KEEP DENSE_RANK LAST`, que só existe no Oracle, e deixa a consulta **portável**: usa só recursos comuns ao Oracle e ao PostgreSQL. Os testes rodam no PostgreSQL; no Oracle ela não foi executada.
- `LEFT JOIN` para manter quem nunca comprou.
- Os limites de dias e a empresa são parâmetros, nada fixo no código.

## Testes

```bash
sql/testes/rodar.sh
```

Sobe um PostgreSQL 16 descartável no Docker, cria o recorte das tabelas ([`testes/dados/schema.sql`](testes/dados/schema.sql)), carrega dados inventados ([`testes/dados/exemplo.sql`](testes/dados/exemplo.sql)), roda a consulta e compara a saída com o CSV de [`testes/esperado`](testes/esperado/).

O resultado esperado foi escrito à mão a partir das regras, não gerado pela própria consulta. Os dados cobrem os casos que costumam dar bug: exatamente 75 e 90 dias, 74 dias (fora), pedido pendente recente, devolução recente, venda com data futura, duas vendas no mesmo dia, venda antiga e recente do mesmo cliente, venda antiga numa empresa e recente em outra (com e sem filtro), cliente inativo, fornecedor e cliente sem vendedor.

Para conferir que os testes pegam erro de verdade, alterei a consulta de propósito: trocar `>=` por `>` no limite de alerta, ou pegar a venda mais antiga em vez da mais recente, faz o teste falhar.
