package io.github.dqueir0z.erp.nfe;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monta o {@code <nfeProc>}: o XML que o destinatário recebe, com a nota assinada e o
 * protocolo de autorização da SEFAZ.
 *
 * <p>O ERP guarda os dois separados (a nota em um campo e, muitas vezes, só o
 * {@code <infProt>} em outro). A nota é <b>assinada digitalmente</b>, então este montador
 * valida tudo com um parser, mas copia o trecho da nota como texto, sem reserializar:
 * qualquer mudança no conteúdo assinado invalidaria a assinatura.
 *
 * <p>A estrutura é conferida de forma estrita (filhos diretos, um elemento de cada,
 * namespace da NF-e), para que um XML com a forma errada não seja aceito só porque
 * contém as tags certas em algum lugar.
 */
public final class MontadorNfeProc {

    static final String NAMESPACE_NFE = "http://www.portalfiscal.inf.br/nfe";
    private static final String VERSAO_PADRAO = "4.00";

    /** 100 = autorizado o uso; 150 = autorizado fora do prazo. */
    private static final Set<String> STATUS_AUTORIZADA = new HashSet<>(Arrays.asList("100", "150"));

    private static final Pattern DECLARACAO_XML = Pattern.compile("^\\s*<\\?xml[^>]*\\?>\\s*");
    private static final Pattern ID_INF_NFE = Pattern.compile("NFe(\\d{44})");

    private MontadorNfeProc() {
    }

    /**
     * @param xmlNfe       XML da nota ({@code <NFe>}) ou um {@code <nfeProc>} já completo
     * @param xmlProtocolo {@code <protNFe>} ou apenas o {@code <infProt>}; ignorado quando
     *                     {@code xmlNfe} já é um nfeProc
     */
    public static NfeProc montar(String xmlNfe, String xmlProtocolo) throws XmlNfeInvalidoException {
        String nfe = semDeclaracao(obrigatorio(xmlNfe, "XML da nota"));
        Element raiz = XmlSeguro.ler(nfe, "XML da nota").getDocumentElement();
        exigirNamespaceNfe(raiz, "XML da nota");

        if ("nfeProc".equals(raiz.getLocalName())) {
            return nfeProcExistente(raiz, nfe);
        }
        if (!"NFe".equals(raiz.getLocalName())) {
            throw new XmlNfeInvalidoException(
                    "XML da nota deveria começar em <NFe>, mas começa em <" + raiz.getLocalName() + ">.");
        }

        DadosNota nota = lerNota(raiz);
        String protNfe = protocoloCompleto(obrigatorio(xmlProtocolo, "Protocolo de autorização"), nota.versao);
        validarProtocolo(XmlSeguro.ler(protNfe, "Protocolo de autorização").getDocumentElement(), nota);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<nfeProc xmlns=\"" + NAMESPACE_NFE + "\" versao=\"" + nota.versao + "\">"
                + nfe
                + protNfe
                + "</nfeProc>";
        // O resultado passa pela mesma validação de um nfeProc recebido pronto: pega, por
        // exemplo, um fragmento com xmlns="" que não herdaria o namespace da NF-e.
        validarNfeProc(XmlSeguro.ler(xml, "nfeProc montado").getDocumentElement());
        return new NfeProc(nota.chave, xml);
    }

    private static NfeProc nfeProcExistente(Element nfeProc, String xml) throws XmlNfeInvalidoException {
        DadosNota nota = validarNfeProc(nfeProc);
        return new NfeProc(nota.chave, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml);
    }

    private static DadosNota validarNfeProc(Element nfeProc) throws XmlNfeInvalidoException {
        DadosNota nota = lerNota(filhoUnico(nfeProc, "NFe", "nfeProc"));
        validarProtocolo(filhoUnico(nfeProc, "protNFe", "nfeProc"), nota);
        return nota;
    }

    /** Chave, versão do leiaute e ambiente (1 produção, 2 homologação) da nota. */
    private static DadosNota lerNota(Element nfe) throws XmlNfeInvalidoException {
        Element infNfe = filhoUnico(nfe, "infNFe", "XML da nota");
        String chave = chaveDoId(infNfe.getAttribute("Id"));
        String versao = infNfe.hasAttribute("versao") ? infNfe.getAttribute("versao") : VERSAO_PADRAO;
        String ambiente = textoFilho(filhoUnico(infNfe, "ide", "XML da nota"), "tpAmb", "XML da nota");
        return new DadosNota(chave, versao, ambiente);
    }

    /**
     * O ERP costuma guardar só o {@code <infProt>}: completa com o {@code <protNFe>}.
     * O fragmento pode vir sem namespace (herda o da NF-e dentro do nfeProc) ou já no
     * namespace da NF-e; qualquer outro é recusado.
     */
    private static String protocoloCompleto(String xmlProtocolo, String versao) throws XmlNfeInvalidoException {
        String protocolo = semDeclaracao(xmlProtocolo);
        Element raiz = XmlSeguro.ler(protocolo, "Protocolo de autorização").getDocumentElement();
        String ns = raiz.getNamespaceURI();
        if (ns != null && !NAMESPACE_NFE.equals(ns)) {
            throw new XmlNfeInvalidoException("Protocolo de autorização em namespace inesperado (" + ns + ").");
        }
        if ("protNFe".equals(raiz.getLocalName())) {
            return protocolo;
        }
        if ("infProt".equals(raiz.getLocalName())) {
            String xmlns = ns == null ? "" : " xmlns=\"" + NAMESPACE_NFE + "\"";
            return "<protNFe" + xmlns + " versao=\"" + versao + "\">" + protocolo + "</protNFe>";
        }
        throw new XmlNfeInvalidoException(
                "Protocolo deveria ser <protNFe> ou <infProt>, mas é <" + raiz.getLocalName() + ">.");
    }

    private static void validarProtocolo(Element protNfe, DadosNota nota) throws XmlNfeInvalidoException {
        String descricao = "Protocolo de autorização";
        Element infProt = filhoUnico(protNfe, "infProt", descricao);

        String chaveProtocolo = textoFilho(infProt, "chNFe", descricao);
        if (!nota.chave.equals(chaveProtocolo)) {
            throw new XmlNfeInvalidoException("Protocolo pertence a outra nota (chave " + chaveProtocolo + ").");
        }
        String ambienteProtocolo = textoFilho(infProt, "tpAmb", descricao);
        if (!nota.ambiente.equals(ambienteProtocolo)) {
            throw new XmlNfeInvalidoException("Protocolo de outro ambiente (nota tpAmb " + nota.ambiente
                    + ", protocolo tpAmb " + ambienteProtocolo + ").");
        }
        String status = textoFilho(infProt, "cStat", descricao);
        if (!STATUS_AUTORIZADA.contains(status)) {
            Element motivo = filhoOpcional(infProt, "xMotivo", descricao);
            throw new XmlNfeInvalidoException("Nota não autorizada (cStat " + status
                    + (motivo == null ? "" : ": " + motivo.getTextContent().trim()) + ").");
        }
    }

    private static String chaveDoId(String id) throws XmlNfeInvalidoException {
        Matcher m = ID_INF_NFE.matcher(id == null ? "" : id);
        if (!m.matches()) {
            throw new XmlNfeInvalidoException("Atributo Id de <infNFe> inválido: \"" + id + "\".");
        }
        String chave = m.group(1);
        if (!ChaveAcesso.valida(chave)) {
            throw new XmlNfeInvalidoException("Chave de acesso com dígito verificador inválido: " + chave + ".");
        }
        return chave;
    }

    private static void exigirNamespaceNfe(Element raiz, String descricao) throws XmlNfeInvalidoException {
        if (!NAMESPACE_NFE.equals(raiz.getNamespaceURI())) {
            throw new XmlNfeInvalidoException(descricao + " sem o namespace da NF-e (" + NAMESPACE_NFE + ").");
        }
    }

    /** Exatamente um filho direto com o nome dado, no mesmo namespace do pai. */
    private static Element filhoUnico(Element pai, String nome, String descricao) throws XmlNfeInvalidoException {
        Element filho = filhoOpcional(pai, nome, descricao);
        if (filho == null) {
            throw new XmlNfeInvalidoException(descricao + ": <" + pai.getLocalName() + "> sem o elemento <" + nome + ">.");
        }
        return filho;
    }

    /**
     * Filho direto opcional, obrigatoriamente no mesmo namespace do pai. A nota inteira fica no
     * namespace da NF-e (a raiz é conferida); o fragmento de protocolo, inteiro sem namespace ou
     * inteiro no da NF-e, sem misturar.
     */
    private static Element filhoOpcional(Element pai, String nome, String descricao) throws XmlNfeInvalidoException {
        String nsPai = pai.getNamespaceURI();
        Element encontrado = null;
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no.getNodeType() != Node.ELEMENT_NODE || !nome.equals(no.getLocalName())) {
                continue;
            }
            String ns = no.getNamespaceURI();
            if (nsPai == null ? ns != null : !nsPai.equals(ns)) {
                throw new XmlNfeInvalidoException(descricao + ": <" + nome + "> fora do namespace de <"
                        + pai.getLocalName() + "> (" + ns + ").");
            }
            if (encontrado != null) {
                throw new XmlNfeInvalidoException(descricao + ": <" + nome + "> repetido em <" + pai.getLocalName() + ">.");
            }
            encontrado = (Element) no;
        }
        return encontrado;
    }

    private static String textoFilho(Element pai, String nome, String descricao) throws XmlNfeInvalidoException {
        return filhoUnico(pai, nome, descricao).getTextContent().trim();
    }

    private static String obrigatorio(String valor, String descricao) throws XmlNfeInvalidoException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new XmlNfeInvalidoException(descricao + " não informado.");
        }
        return valor;
    }

    private static String semDeclaracao(String xml) {
        return DECLARACAO_XML.matcher(xml).replaceFirst("").trim();
    }

    private static final class DadosNota {
        final String chave;
        final String versao;
        final String ambiente;

        DadosNota(String chave, String versao, String ambiente) {
            this.chave = chave;
            this.versao = versao;
            this.ambiente = ambiente;
        }
    }
}
