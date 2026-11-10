-- Recorte mínimo das tabelas padrão do Sankhya usadas pelas consultas, só com as colunas
-- necessárias. Os tipos seguem o PostgreSQL usado no teste.

CREATE TABLE TSIEMP (
    CODEMP      INTEGER PRIMARY KEY,
    RAZAOABREV  VARCHAR(40) NOT NULL
);

CREATE TABLE TGFVEN (
    CODVEND     INTEGER PRIMARY KEY,
    APELIDO     VARCHAR(40) NOT NULL
);

CREATE TABLE TGFPAR (
    CODPARC     INTEGER PRIMARY KEY,
    NOMEPARC    VARCHAR(100) NOT NULL,
    ATIVO       CHAR(1) NOT NULL,
    CLIENTE     CHAR(1) NOT NULL,
    CODVEND     INTEGER REFERENCES TGFVEN (CODVEND)
);

CREATE TABLE TGFCAB (
    NUNOTA      INTEGER PRIMARY KEY,
    CODPARC     INTEGER NOT NULL REFERENCES TGFPAR (CODPARC),
    CODEMP      INTEGER NOT NULL REFERENCES TSIEMP (CODEMP),
    TIPMOV      CHAR(1) NOT NULL,   -- V venda, D devolução, C compra...
    STATUSNOTA  CHAR(1) NOT NULL,   -- L liberada (confirmada), P pendente, A em andamento
    DTNEG       DATE NOT NULL,
    VLRNOTA     NUMERIC(15, 2) NOT NULL
);
