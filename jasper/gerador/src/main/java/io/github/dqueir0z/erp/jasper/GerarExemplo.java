package io.github.dqueir0z.erp.jasper;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Gera o PDF de exemplo de um relatório e a imagem da primeira página (para o README).
 *
 * <p>Conecta no banco de demonstração (sql/testes/dados) indicado por DB_URL, DB_USER e
 * DB_PASSWORD; o script jasper/gerar-exemplo.sh sobe esse banco em Docker.
 */
public final class GerarExemplo {

    private GerarExemplo() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: GerarExemplo <relatorio.jrxml> <pasta-de-saida>");
        }
        Path jrxml = Paths.get(args[0]);
        Path pasta = Files.createDirectories(Paths.get(args[1]));
        String nome = jrxml.getFileName().toString().replaceFirst("\\.jrxml$", "");

        JasperReport relatorio = JasperCompileManager.compileReport(jrxml.toString());

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("DATA_REF", Date.valueOf("2026-11-10"));
        parametros.put("DIAS_ALERTA", 75);
        parametros.put("DIAS_CRITICO", 90);
        parametros.put("CODEMP", null);
        // Formato brasileiro (2.100,00 e 24/04/2026) em qualquer máquina.
        parametros.put(JRParameter.REPORT_LOCALE, new Locale("pt", "BR"));

        JasperPrint impressao;
        try (Connection conexao = DriverManager.getConnection(
                ambiente("DB_URL", "jdbc:postgresql://localhost:55432/erp"),
                ambiente("DB_USER", "postgres"),
                ambiente("DB_PASSWORD", "teste"))) {
            impressao = JasperFillManager.fillReport(relatorio, parametros, conexao);
        }

        File pdf = pasta.resolve(nome + ".pdf").toFile();
        JasperExportManager.exportReportToPdfFile(impressao, pdf.getPath());

        File png = pasta.resolve(nome + ".png").toFile();
        BufferedImage pagina = (BufferedImage) JasperPrintManager.printPageToImage(impressao, 0, 2f);
        ImageIO.write(pagina, "png", png);

        System.out.printf("%d página(s): %s e %s%n", impressao.getPages().size(), pdf, png);
    }

    private static String ambiente(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isEmpty() ? padrao : valor;
    }
}
