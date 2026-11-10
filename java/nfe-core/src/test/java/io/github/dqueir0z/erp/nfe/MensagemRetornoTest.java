package io.github.dqueir0z.erp.nfe;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_123;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensagemRetornoTest {

    @Test
    void mostraTotalELinkQuandoExportou() {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Collections.singletonList(new NotaParaExportar(
                1001, 123L, XmlExemplo.nfe(CHAVE_123), XmlExemplo.infProtAutorizado(CHAVE_123))));

        String html = MensagemRetorno.montar(r, "/download?chave=abc");

        assertTrue(html.contains("1 XML exportado."));
        assertTrue(html.contains("href=\"/download?chave=abc\""));
        assertFalse(html.contains("Ignoradas"));
    }

    @Test
    void listaAsNotasIgnoradasComOMotivo() {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Arrays.asList(
                new NotaParaExportar(1001, 123L, XmlExemplo.nfe(CHAVE_123), null)));

        String html = MensagemRetorno.montar(r, null);

        assertTrue(html.contains("Nenhum XML exportado"));
        assertTrue(html.contains("<li>Nota 1001: Protocolo de autorização não informado.</li>"));
    }

    @Test
    void escapaTextoVindoDoXmlParaNaoVirarHtml() {
        String motivoMalicioso = "<script>alert(1)</script>";
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Collections.singletonList(new NotaParaExportar(
                1001, 123L, XmlExemplo.nfe(CHAVE_123),
                XmlExemplo.infProt(CHAVE_123, "302", motivoMalicioso.replace("<", "&lt;").replace(">", "&gt;")))));

        String html = MensagemRetorno.montar(r, null);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
}
