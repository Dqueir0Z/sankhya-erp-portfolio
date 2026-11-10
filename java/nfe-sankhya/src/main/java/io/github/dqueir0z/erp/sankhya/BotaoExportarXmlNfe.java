package io.github.dqueir0z.erp.sankhya;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.ws.ServiceContext;
import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;
import io.github.dqueir0z.erp.nfe.ExportacaoXmlNfe;
import io.github.dqueir0z.erp.nfe.MensagemRetorno;
import io.github.dqueir0z.erp.nfe.NotaParaExportar;
import io.github.dqueir0z.erp.nfe.ResultadoExportacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Botão de ação da tela de notas (Central de Notas / Portal de Vendas): exporta o XML de
 * distribuição das notas selecionadas em um ZIP e devolve o link de download.
 *
 * <p>Esta classe só lê os dados do ERP e entrega o arquivo. A montagem do nfeProc, a
 * validação e o ZIP ficam em {@code nfe-core}, que é testado sem o Sankhya.
 *
 * <p>No ERP, cadastre uma ação do tipo "Rotina Java" na tela de notas apontando para esta
 * classe, com o jar gerado por {@code ./gradlew :java:nfe-sankhya:jar}.
 */
public class BotaoExportarXmlNfe implements AcaoRotinaJava {

    /** Entidade padrão do Sankhya com o XML da nota e o retorno da SEFAZ. */
    private static final String ENTIDADE_ARQUIVO_NFE = "ArquivoNFe";
    /** Cabeçalho da nota (TGFCAB). */
    private static final String ENTIDADE_CABECALHO = "CabecalhoNota";

    private static final String NOME_DO_ZIP = "XML_NFE.zip";
    private static final String URL_DOWNLOAD = "/mge/visualizadorArquivos.mge?chaveArquivo=";

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();
        if (linhas == null || linhas.length == 0) {
            contexto.setMensagemRetorno("Selecione ao menos uma nota.");
            return;
        }

        JapeSession.SessionHandle sessao = null;
        try {
            sessao = JapeSession.open();
            ResultadoExportacao resultado = ExportacaoXmlNfe.exportar(lerNotas(linhas));
            String link = resultado.temArquivos() ? publicarParaDownload(resultado.getZip()) : null;
            contexto.setMensagemRetorno(MensagemRetorno.montar(resultado, link));
        } finally {
            JapeSession.close(sessao);
        }
    }

    private static List<NotaParaExportar> lerNotas(Registro[] linhas) throws Exception {
        JapeWrapper arquivos = JapeFactory.dao(ENTIDADE_ARQUIVO_NFE);
        JapeWrapper cabecalhos = JapeFactory.dao(ENTIDADE_CABECALHO);

        List<NotaParaExportar> notas = new ArrayList<>(linhas.length);
        for (Registro linha : linhas) {
            BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
            DynamicVO arquivo = arquivos.findOne("NUNOTA = ?", nunota);
            DynamicVO cabecalho = cabecalhos.findOne("NUNOTA = ?", nunota);

            BigDecimal numNota = cabecalho == null ? null : cabecalho.asBigDecimal("NUMNOTA");
            notas.add(new NotaParaExportar(
                    nunota.longValue(),
                    numNota == null ? null : numNota.longValue(),
                    arquivo == null ? null : arquivo.asString("XML"),
                    arquivo == null ? null : arquivo.asString("XMLPROTAUTNOT")));
        }
        return notas;
    }

    /** Guarda o ZIP na sessão HTTP do usuário e devolve o endereço do visualizador de arquivos. */
    private static String publicarParaDownload(byte[] zip) throws Exception {
        String chave = "xml_nfe_" + UIDGenerator.getNextID();
        SessionFile arquivo = SessionFile.createSessionFile(NOME_DO_ZIP, "zip", zip);
        ServiceContext.getCurrent().putHttpSessionAttribute(chave, arquivo);
        return URL_DOWNLOAD + chave;
    }
}
