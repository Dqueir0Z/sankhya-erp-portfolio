-- Dados inventados. Datas relativas à data de referência dos testes (10/11/2026),
-- para cada caso ficar exatamente na distância que quer testar.

INSERT INTO TSIEMP (CODEMP, RAZAOABREV) VALUES
    (1, 'MATRIZ EXEMPLO'),
    (2, 'FILIAL EXEMPLO');

INSERT INTO TGFVEN (CODVEND, APELIDO) VALUES
    (1, 'MARIANA'),
    (2, 'PAULO');

INSERT INTO TGFPAR (CODPARC, NOMEPARC, ATIVO, CLIENTE, CODVEND) VALUES
    (101, 'ALFA COMERCIO LTDA', 'S', 'S', 1),     -- 100 dias: crítico
    (102, 'BETA MATERIAIS ME',  'S', 'S', 2),     -- 80 dias: alerta
    (103, 'GAMA DISTRIBUIDORA', 'S', 'S', 1),     -- comprou há 10 dias: fora
    (104, 'DELTA ATACADO',      'S', 'S', 2),     -- nunca comprou
    (105, 'EPSILON LOJA',       'N', 'S', 1),     -- inativo: fora
    (106, 'ZETA INDUSTRIA',     'S', 'N', 1),     -- fornecedor, não cliente: fora
    (107, 'ETA SUPERMERCADO',   'S', 'S', 1),     -- pedido pendente recente não conta
    (108, 'TETA FARMACIA',      'S', 'S', 2),     -- devolução recente não conta
    (109, 'IOTA PAPELARIA',     'S', 'S', 1),     -- exatamente 75 dias: alerta
    (110, 'KAPA AUTOPECAS',     'S', 'S', 2),     -- 74 dias: fora
    (111, 'LAMBDA CONFECCOES',  'S', 'S', 1),     -- exatamente 90 dias: ainda alerta
    (112, 'MI FERRAGENS',       'S', 'S', 2),     -- 91 dias: crítico
    (113, 'NI ELETRO',          'S', 'S', 1),     -- duas vendas no mesmo dia
    (114, 'XI PET SHOP',        'S', 'S', NULL),  -- sem vendedor; venda futura não conta
    (115, 'OMEGA BEBIDAS',      'S', 'S', 1),     -- venda antiga e recente: vale a recente (fora)
    (116, 'PI CALCADOS',        'S', 'S', 2);     -- antiga na empresa 2, recente na 1

INSERT INTO TGFCAB (NUNOTA, CODPARC, CODEMP, TIPMOV, STATUSNOTA, DTNEG, VLRNOTA) VALUES
    (1001, 101, 1, 'V', 'L', DATE '2026-11-10' - 100, 1500.00),
    (1002, 102, 1, 'V', 'L', DATE '2026-11-10' - 80,   820.50),
    (1003, 103, 2, 'V', 'L', DATE '2026-11-10' - 10,   300.00),
    (1005, 105, 1, 'V', 'L', DATE '2026-11-10' - 200,  999.00),
    (1006, 106, 1, 'V', 'L', DATE '2026-11-10' - 300,   50.00),
    (1007, 107, 1, 'V', 'L', DATE '2026-11-10' - 200, 2100.00),
    (1008, 107, 1, 'V', 'P', DATE '2026-11-10' - 20,   400.00),
    (1009, 108, 2, 'V', 'L', DATE '2026-11-10' - 76,   640.00),
    (1010, 108, 2, 'D', 'L', DATE '2026-11-10' - 5,    640.00),
    (1011, 109, 1, 'V', 'L', DATE '2026-11-10' - 75,   120.00),
    (1012, 110, 2, 'V', 'L', DATE '2026-11-10' - 74,    75.00),
    (1013, 111, 1, 'V', 'L', DATE '2026-11-10' - 90,   330.00),
    (1014, 112, 2, 'V', 'L', DATE '2026-11-10' - 91,   910.00),
    (1015, 113, 1, 'V', 'L', DATE '2026-11-10' - 95,   100.00),
    (1016, 113, 2, 'V', 'L', DATE '2026-11-10' - 95,   250.00),
    (1017, 114, 1, 'V', 'L', DATE '2026-11-10' - 120,   60.00),
    (1018, 114, 2, 'V', 'L', DATE '2026-11-10' + 3,    999.99),
    (1019, 115, 1, 'V', 'L', DATE '2026-11-10' - 150,  700.00),
    (1020, 115, 1, 'V', 'L', DATE '2026-11-10' - 5,     90.00),
    (1021, 116, 2, 'V', 'L', DATE '2026-11-10' - 100,  480.00),
    (1022, 116, 1, 'V', 'L', DATE '2026-11-10' - 3,     55.00);
