package io.github.dqueir0z.erp.nfe;

/**
 * Mensagem em HTML mostrada ao usuário no fim do botão de ação.
 *
 * <p>Os motivos de rejeição trazem texto vindo do XML (por exemplo o xMotivo da SEFAZ),
 * então tudo é escapado antes de virar HTML.
 */
public final class MensagemRetorno {

    private MensagemRetorno() {
    }

    /**
     * @param resultado    resultado da exportação
     * @param linkDownload endereço do ZIP; ignorado quando nada foi exportado
     */
    public static String montar(ResultadoExportacao resultado, String linkDownload) {
        StringBuilder html = new StringBuilder();
        if (resultado.temArquivos()) {
            int total = resultado.getArquivos().size();
            html.append("<b>Exportação concluída.</b><br>")
                    .append(total).append(total == 1 ? " XML exportado." : " XMLs exportados.")
                    .append("<br><br><a href=\"").append(escapar(linkDownload))
                    .append("\" target=\"_blank\">Baixar o ZIP</a>");
        } else {
            html.append("<b>Nenhum XML exportado.</b> Verifique se as notas estão autorizadas.");
        }
        if (!resultado.getIgnoradas().isEmpty()) {
            html.append("<br><br>Ignoradas:<ul>");
            for (String motivo : resultado.getIgnoradas()) {
                html.append("<li>").append(escapar(motivo)).append("</li>");
            }
            html.append("</ul>");
        }
        return html.toString();
    }

    static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
