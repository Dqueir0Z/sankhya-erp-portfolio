package io.github.dqueir0z.erp.nfe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Gera um ZIP com o XML de distribuição (nfeProc) de cada nota.
 *
 * <p>Nome de cada arquivo: o número da nota ({@code 123.xml}), que é como o usuário procura;
 * sem número, a chave de acesso. Números repetidos (séries ou empresas diferentes) ganham
 * sufixo ({@code 123_2.xml}) em vez de sobrescrever. Uma nota com problema não interrompe
 * as outras: ela entra na lista de ignoradas com o motivo.
 */
public final class ExportacaoXmlNfe {

    private ExportacaoXmlNfe() {
    }

    public static ResultadoExportacao exportar(List<NotaParaExportar> notas) {
        List<String> arquivos = new ArrayList<>();
        List<String> ignoradas = new ArrayList<>();
        Set<String> nomesUsados = new HashSet<>();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (NotaParaExportar nota : notas) {
                NfeProc nfeProc;
                try {
                    nfeProc = MontadorNfeProc.montar(nota.getXmlNfe(), nota.getXmlProtocolo());
                } catch (XmlNfeInvalidoException e) {
                    ignoradas.add("Nota " + nota.getNunota() + ": " + e.getMessage());
                    continue;
                }
                String nome = nomeUnico(nomeBase(nota, nfeProc), nomesUsados);
                zip.putNextEntry(new ZipEntry(nome));
                zip.write(nfeProc.getXml().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                arquivos.add(nome);
            }
        } catch (IOException e) {
            // ByteArrayOutputStream não falha em escrita; só chega aqui por erro de programação.
            throw new UncheckedIOException(e);
        }
        return new ResultadoExportacao(arquivos.isEmpty() ? null : bytes.toByteArray(), arquivos, ignoradas);
    }

    static String nomeBase(NotaParaExportar nota, NfeProc nfeProc) {
        Long numero = nota.getNumeroNota();
        return numero != null && numero > 0 ? String.valueOf(numero) : nfeProc.getChave();
    }

    private static String nomeUnico(String base, Set<String> usados) {
        String nome = base + ".xml";
        for (int i = 2; !usados.add(nome); i++) {
            nome = base + "_" + i + ".xml";
        }
        return nome;
    }
}
