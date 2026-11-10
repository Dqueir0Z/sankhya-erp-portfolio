/*
 * Clientes sem movimentação
 *
 * Objetivo: listar clientes ativos que estão há muito tempo sem comprar, para a equipe
 * comercial agir antes de perder o cliente. Cada cliente aparece uma vez, com a última
 * venda, o vendedor responsável e a faixa:
 *   CRITICO      mais de :DIAS_CRITICO dias sem comprar
 *   ALERTA       entre :DIAS_ALERTA e :DIAS_CRITICO dias (inclusive)
 *   SEM COMPRAS  nenhuma venda confirmada até :DATA_REF (na empresa filtrada, se houver)
 *
 * Regras:
 *   - Venda = nota de venda (TIPMOV 'V') confirmada (STATUSNOTA 'L'). Pedidos pendentes,
 *     devoluções e notas com data depois de :DATA_REF não contam.
 *   - Duas vendas no mesmo dia: vale a de maior NUNOTA (a última lançada).
 *   - Com :CODEMP informado, só as vendas daquela empresa contam.
 *
 * Parâmetros:
 *   :DATA_REF      data de referência (normalmente a data de hoje)
 *   :DIAS_ALERTA   início da faixa de alerta, ex.: 75
 *   :DIAS_CRITICO  acima disso é crítico, ex.: 90
 *   :CODEMP        empresa (TSIEMP.CODEMP) ou NULL para todas
 *
 * Tabelas: TGFPAR (parceiros), TGFCAB (cabeçalho das notas), TGFVEN (vendedores),
 *          TSIEMP (empresas).
 *
 * Dialeto: SQL padrão que roda no Oracle e no PostgreSQL (testado no PostgreSQL;
 * ver sql/testes). ROW_NUMBER() substitui o KEEP DENSE_RANK, que só existe no Oracle.
 */
WITH vendas AS (
    SELECT cab.CODPARC,
           cab.DTNEG,
           cab.VLRNOTA,
           cab.CODEMP,
           ROW_NUMBER() OVER (PARTITION BY cab.CODPARC
                              ORDER BY cab.DTNEG DESC, cab.NUNOTA DESC) AS ORDEM
      FROM TGFCAB cab
     WHERE cab.TIPMOV = 'V'
       AND cab.STATUSNOTA = 'L'
       AND cab.DTNEG <= :DATA_REF
       AND (:CODEMP IS NULL OR cab.CODEMP = :CODEMP)
),
situacao AS (
    SELECT par.CODPARC,
           par.NOMEPARC,
           ven.APELIDO                AS VENDEDOR,
           ult.DTNEG                  AS DT_ULTIMA_VENDA,
           :DATA_REF - ult.DTNEG      AS DIAS_SEM_COMPRA,
           ult.VLRNOTA                AS VLR_ULTIMA_VENDA,
           emp.RAZAOABREV             AS EMPRESA_ULTIMA_VENDA
      FROM TGFPAR par
      LEFT JOIN vendas ult ON ult.CODPARC = par.CODPARC
                          AND ult.ORDEM = 1
      LEFT JOIN TGFVEN ven ON ven.CODVEND = par.CODVEND
      LEFT JOIN TSIEMP emp ON emp.CODEMP = ult.CODEMP
     WHERE par.ATIVO = 'S'
       AND par.CLIENTE = 'S'
)
SELECT sit.CODPARC,
       sit.NOMEPARC,
       sit.VENDEDOR,
       sit.DT_ULTIMA_VENDA,
       sit.DIAS_SEM_COMPRA,
       sit.VLR_ULTIMA_VENDA,
       sit.EMPRESA_ULTIMA_VENDA,
       CASE
           WHEN sit.DT_ULTIMA_VENDA IS NULL THEN 'SEM COMPRAS'
           WHEN sit.DIAS_SEM_COMPRA > :DIAS_CRITICO THEN 'CRITICO'
           ELSE 'ALERTA'
       END AS FAIXA
  FROM situacao sit
 WHERE sit.DT_ULTIMA_VENDA IS NULL
    OR sit.DIAS_SEM_COMPRA >= :DIAS_ALERTA
 ORDER BY CASE
              WHEN sit.DT_ULTIMA_VENDA IS NULL THEN 3
              WHEN sit.DIAS_SEM_COMPRA > :DIAS_CRITICO THEN 1
              ELSE 2
          END,
          sit.DIAS_SEM_COMPRA DESC,
          sit.NOMEPARC;
