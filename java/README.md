# Java: exportar XML de NF-e

Botão de ação do Sankhya: o usuário seleciona notas na tela, clica em **Exportar XML** e recebe um link para baixar um ZIP com o XML de distribuição de cada nota.

## O problema

O ERP guarda separados o XML da nota e o retorno da SEFAZ (muitas vezes só o `<infProt>`). O cliente e o contador precisam do **nfeProc**, o XML completo com a nota e o protocolo de autorização.

## Estrutura

| Módulo | O que tem | Depende do Sankhya? |
|---|---|---|
| [`nfe-core`](nfe-core/) | Toda a regra: montagem do nfeProc, validações, ZIP e mensagem de retorno | Não. Compila e testa em qualquer máquina |
| [`nfe-sankhya`](nfe-sankhya/) | O botão de ação (`AcaoRotinaJava`): lê as notas do ERP, chama o núcleo e publica o ZIP para download | Sim. Só entra no build com `-PsankhyaLibs` |

Separar assim deixa a regra testável sem o ERP. O adaptador fica fino: só lê dados e entrega o arquivo.

## Decisões

- **A nota assinada é copiada como texto, não reserializada.** O XML da NF-e tem assinatura digital. Ler o XML como objeto e gravar de novo pode mudar o conteúdo assinado e invalidar a assinatura. O núcleo valida tudo com um parser e monta o resultado juntando os trechos originais. Um teste assina uma nota com uma chave RSA gerada na hora (enveloped, C14N inclusiva, referência ao `Id`, como na NF-e), monta o nfeProc e **valida a assinatura criptograficamente** no resultado. Um teste de controle confirma que a validação reprova um XML alterado.
- **Parser protegido contra XXE.** O XML vem do banco e pode ter sido adulterado. DOCTYPE e entidades externas são recusados, e o processamento de XInclude fica desligado. Os dois testes de DOCTYPE falham se essa proteção for desligada.
- **Estrutura conferida de forma estrita.** Filhos diretos, um elemento de cada e namespace da NF-e: um XML com as tags certas no lugar errado não passa.
- **O protocolo precisa ser daquela nota, do mesmo ambiente e de uma autorização.** A chave e o `tpAmb` do `<infProt>` têm que bater com os da nota, e o `cStat` tem que ser 100 ou 150. Uma nota denegada não vai para o ZIP.
- **Chave de acesso com dígito verificador conferido** (módulo 11, só dígitos de 0 a 9).
- **Uma nota com problema não derruba a exportação.** Ela fica de fora, e a mensagem final lista o motivo de cada uma, com o texto escapado para não virar HTML na tela.
- **Nome do arquivo = número da nota**, que é como o usuário procura. Números repetidos (séries ou empresas diferentes) ganham sufixo (`500_2.xml`) em vez de sobrescrever.
- **Bytecode direcionado ao Java 8** (`options.release = 8`). A compatibilidade com versões antigas do Sankhya não foi testada.

## Como rodar

```bash
./gradlew :java:nfe-core:test
```

Para gerar o jar da extensão, aponte a pasta com os jars da sua instalação do Sankhya:

```bash
./gradlew :java:nfe-sankhya:jar -PsankhyaLibs=C:/caminho/dos/jars
```

O jar sai em `java/nfe-sankhya/build/libs/exportar-xml-nfe.jar`, já com o núcleo dentro. No ERP, cadastre um botão de ação na tela de notas apontando para a classe `io.github.dqueir0z.erp.sankhya.BotaoExportarXmlNfe`.

## Validado

- 40 testes JUnit no núcleo (`./gradlew test`).
- O adaptador compila contra as bibliotecas do Sankhya 4.35. Esta versão não foi executada dentro de um ERP.
