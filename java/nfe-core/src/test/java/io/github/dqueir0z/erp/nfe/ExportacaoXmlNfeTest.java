package io.github.dqueir0z.erp.nfe;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_123;
import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_124;
import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_125;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportacaoXmlNfeTest {

    @Test
    void geraUmArquivoPorNotaComONumeroDaNota() throws Exception {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Arrays.asList(
                nota(1001, 123L, CHAVE_123),
                nota(1002, 124L, CHAVE_124)));

        assertEquals(Arrays.asList("123.xml", "124.xml"), r.getArquivos());
        Map<String, String> zip = lerZip(r.getZip());
        assertEquals(r.getArquivos(), Arrays.asList(zip.keySet().toArray(new String[0])));
        assertTrue(zip.get("123.xml").contains(CHAVE_123));
        assertTrue(r.getIgnoradas().isEmpty());
    }

    @Test
    void notaComProblemaNaoImpedeAsOutras() throws Exception {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Arrays.asList(
                nota(1001, 123L, CHAVE_123),
                new NotaParaExportar(1002, 124L, XmlExemplo.nfe(CHAVE_124), null),
                nota(1003, 125L, CHAVE_125)));

        assertEquals(Arrays.asList("123.xml", "125.xml"), r.getArquivos());
        assertEquals(1, r.getIgnoradas().size());
        assertTrue(r.getIgnoradas().get(0).startsWith("Nota 1002: Protocolo"));
        assertEquals(2, lerZip(r.getZip()).size());
    }

    @Test
    void numeroRepetidoGanhaSufixoEmVezDeSobrescrever() throws Exception {
        // Mesmo número fiscal em séries ou empresas diferentes.
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Arrays.asList(
                nota(1001, 500L, CHAVE_123),
                nota(1002, 500L, CHAVE_124)));

        assertEquals(Arrays.asList("500.xml", "500_2.xml"), r.getArquivos());
        assertEquals(2, lerZip(r.getZip()).size());
    }

    @Test
    void semNumeroDaNotaUsaAChaveDeAcesso() throws Exception {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Collections.singletonList(nota(1001, null, CHAVE_123)));

        assertEquals(Collections.singletonList(CHAVE_123 + ".xml"), r.getArquivos());
    }

    @Test
    void nenhumaNotaValidaNaoGeraZip() {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Collections.singletonList(
                new NotaParaExportar(1001, 123L, "<NFe>", null)));

        assertFalse(r.temArquivos());
        assertNull(r.getZip());
        assertEquals(1, r.getIgnoradas().size());
    }

    @Test
    void listaVaziaNaoGeraZip() {
        ResultadoExportacao r = ExportacaoXmlNfe.exportar(Collections.emptyList());

        assertFalse(r.temArquivos());
        assertTrue(r.getIgnoradas().isEmpty());
    }

    private static NotaParaExportar nota(long nunota, Long numero, String chave) {
        return new NotaParaExportar(nunota, numero, XmlExemplo.nfe(chave), XmlExemplo.infProtAutorizado(chave));
    }

    private static Map<String, String> lerZip(byte[] zip) throws IOException {
        Map<String, String> arquivos = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entrada;
            while ((entrada = in.getNextEntry()) != null) {
                ByteArrayOutputStream conteudo = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int lidos;
                while ((lidos = in.read(buffer)) > 0) {
                    conteudo.write(buffer, 0, lidos);
                }
                arquivos.put(entrada.getName(), new String(conteudo.toByteArray(), StandardCharsets.UTF_8));
            }
        }
        return arquivos;
    }
}
