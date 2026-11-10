package io.github.dqueir0z.erp.nfe;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_123;
import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_124;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MontadorNfeProcTest {

    @Test
    void montaNfeProcAPartirDaNotaEDoInfProt() throws Exception {
        NfeProc proc = MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), XmlExemplo.infProtAutorizado(CHAVE_123));

        assertEquals(CHAVE_123, proc.getChave());
        Element raiz = XmlSeguro.ler(proc.getXml(), "resultado").getDocumentElement();
        assertEquals("nfeProc", raiz.getLocalName());
        assertEquals(MontadorNfeProc.NAMESPACE_NFE, raiz.getNamespaceURI());
        assertEquals("4.00", raiz.getAttribute("versao"));
        assertEquals(1, raiz.getElementsByTagNameNS("*", "protNFe").getLength());
        assertTrue(proc.getXml().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><nfeProc"));
    }

    @Test
    void copiaANotaAssinadaSemAlterarNenhumCaractere() throws Exception {
        NfeProc proc = MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), XmlExemplo.infProtAutorizado(CHAVE_123));

        assertTrue(proc.getXml().contains(XmlExemplo.nfeSemDeclaracao(CHAVE_123)),
                "o trecho <NFe> precisa sair idêntico, senão a assinatura digital deixa de valer");
    }

    @Test
    void aceitaProtocoloCompleto() throws Exception {
        String protNfe = "<protNFe versao=\"4.00\">" + XmlExemplo.infProtAutorizado(CHAVE_123) + "</protNFe>";

        NfeProc proc = MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), protNfe);

        assertEquals(1, XmlSeguro.ler(proc.getXml(), "resultado").getElementsByTagNameNS("*", "protNFe").getLength());
    }

    @Test
    void aceitaAutorizacaoForaDoPrazo() throws Exception {
        String prot = XmlExemplo.infProt(CHAVE_123, "150", "Autorizado o uso da NF-e, autorização fora de prazo");

        assertEquals(CHAVE_123, MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), prot).getChave());
    }

    @Test
    void devolveNfeProcQueJaEstavaCompleto() throws Exception {
        String completo = MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), XmlExemplo.infProtAutorizado(CHAVE_123)).getXml();

        NfeProc proc = MontadorNfeProc.montar(completo, null);

        assertEquals(completo, proc.getXml());
    }

    @Test
    void recusaProtocoloDeOutraNota() {
        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), XmlExemplo.infProtAutorizado(CHAVE_124)));
        assertTrue(e.getMessage().contains("outra nota"));
    }

    @Test
    void recusaNotaDenegada() {
        String denegada = XmlExemplo.infProt(CHAVE_123, "302", "Uso denegado");

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), denegada));
        assertTrue(e.getMessage().contains("302"));
    }

    @Test
    void recusaProtocoloDeOutroAmbiente() {
        // Nota de homologação (tpAmb 2) com protocolo de produção (tpAmb 1).
        String producao = XmlExemplo.infProtAutorizado(CHAVE_123).replace("<tpAmb>2</tpAmb>", "<tpAmb>1</tpAmb>");

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), producao));
        assertTrue(e.getMessage().contains("outro ambiente"));
    }

    @Test
    void recusaProtNfeSemInfProt() {
        // Tem chNFe e cStat, mas fora do lugar: não pode passar só por conter as tags.
        String semInfProt = "<protNFe versao=\"4.00\"><tpAmb>2</tpAmb><chNFe>" + CHAVE_123
                + "</chNFe><cStat>100</cStat></protNFe>";

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), semInfProt));
        assertTrue(e.getMessage().contains("<infProt>"));
    }

    @Test
    void recusaElementoRepetidoNoProtocolo() {
        String chaveDupla = XmlExemplo.infProtAutorizado(CHAVE_123)
                .replace("<chNFe>", "<chNFe>" + CHAVE_123 + "</chNFe><chNFe>");

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), chaveDupla));
        assertTrue(e.getMessage().contains("repetido"));
    }

    @Test
    void recusaProtocoloEmNamespaceDiferente() {
        String outroNamespace = XmlExemplo.infProtAutorizado(CHAVE_123)
                .replace("<infProt>", "<infProt xmlns=\"http://exemplo.invalido/ns\">");

        assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), outroNamespace));
    }

    @Test
    void recusaProtNfeEmNamespaceDiferente() {
        String protNfe = "<protNFe xmlns=\"http://exemplo.invalido/ns\" versao=\"4.00\">"
                + XmlExemplo.infProtAutorizado(CHAVE_123) + "</protNFe>";

        assertThrows(XmlNfeInvalidoException.class, () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), protNfe));
    }

    @Test
    void aceitaInfProtNoNamespaceDaNfe() throws Exception {
        String comNamespace = XmlExemplo.infProtAutorizado(CHAVE_123)
                .replace("<infProt>", "<infProt xmlns=\"http://www.portalfiscal.inf.br/nfe\">");

        assertEquals(CHAVE_123, MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), comNamespace).getChave());
    }

    @Test
    void recusaInfProtQueAnulaONamespace() {
        // xmlns="" impediria o infProt de herdar o namespace da NF-e dentro do nfeProc.
        String anulado = XmlExemplo.infProtAutorizado(CHAVE_123).replace("<infProt>", "<infProt xmlns=\"\">");

        assertThrows(XmlNfeInvalidoException.class, () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), anulado));
    }

    @Test
    void recusaInfNfeSemNamespaceDentroDaNota() {
        // A raiz está certa, mas o <infNFe> saiu do namespace da NF-e.
        String nfe = XmlExemplo.nfe(CHAVE_123).replace("<infNFe ", "<infNFe xmlns=\"\" ");

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(nfe, XmlExemplo.infProtAutorizado(CHAVE_123)));
        assertTrue(e.getMessage().contains("namespace"));
    }

    @Test
    void recusaNotaSemProtocolo() {
        assertThrows(XmlNfeInvalidoException.class, () -> MontadorNfeProc.montar(XmlExemplo.nfe(CHAVE_123), "  "));
    }

    @Test
    void recusaXmlMalFormado() {
        assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar("<NFe><infNFe>", XmlExemplo.infProtAutorizado(CHAVE_123)));
    }

    @Test
    void recusaChaveComDigitoVerificadorErrado() {
        String chaveErrada = CHAVE_123.substring(0, 43) + "9";

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(XmlExemplo.nfe(chaveErrada), XmlExemplo.infProtAutorizado(chaveErrada)));
        assertTrue(e.getMessage().contains("dígito verificador"));
    }

    @Test
    void recusaXmlForaDoPadraoNfe() {
        String semNamespace = "<NFe><infNFe Id=\"NFe" + CHAVE_123 + "\"/></NFe>";

        assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(semNamespace, XmlExemplo.infProtAutorizado(CHAVE_123)));
    }

    @Test
    void recusaDoctypeParaEvitarXxe() {
        // Um XML adulterado no banco tentando ler um arquivo do servidor.
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE NFe [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe Id=\"NFe" + CHAVE_123 + "\">&x;</infNFe></NFe>";

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(xxe, XmlExemplo.infProtAutorizado(CHAVE_123)));
        assertTrue(e.getMessage().contains("DOCTYPE"), "deve ser recusado pelo DOCTYPE: " + e.getMessage());
    }

    @Test
    void recusaQualquerDoctypeMesmoSemEntidadeExterna() {
        // Entidades internas permitem ataques de expansão ("billion laughs"); nota fiscal não usa DOCTYPE.
        String comEntidade = "<?xml version=\"1.0\"?><!DOCTYPE NFe [<!ENTITY x \"texto\">]>"
                + XmlExemplo.nfeSemDeclaracao(CHAVE_123).replace("EMPRESA EXEMPLO LTDA", "&x;");

        XmlNfeInvalidoException e = assertThrows(XmlNfeInvalidoException.class,
                () -> MontadorNfeProc.montar(comEntidade, XmlExemplo.infProtAutorizado(CHAVE_123)));
        assertTrue(e.getMessage().startsWith("XML da nota") && e.getMessage().contains("DOCTYPE"),
                "deve ser recusado já na leitura da nota: " + e.getMessage());
    }
}
