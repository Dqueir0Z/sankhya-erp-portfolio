package io.github.dqueir0z.erp.nfe;

/**
 * XMLs fictícios: CNPJ zerado, ambiente de homologação (tpAmb 2) e assinatura sem valor real.
 * As chaves têm dígito verificador correto para passar na validação.
 */
final class XmlExemplo {

    static final String CHAVE_123 = "35261100000000000000550010000001231000000014";
    static final String CHAVE_124 = "35261100000000000000550010000001241000000011";
    static final String CHAVE_125 = "35261100000000000000550010000001251000000019";

    private XmlExemplo() {
    }

    /** Como o ERP grava: com declaração XML e o namespace da NF-e na raiz. */
    static String nfe(String chave) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + nfeSemDeclaracao(chave);
    }

    static String nfeSemDeclaracao(String chave) {
        return "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                + "<infNFe versao=\"4.00\" Id=\"NFe" + chave + "\">"
                + "<ide><cUF>35</cUF><mod>55</mod><serie>1</serie><nNF>" + Integer.parseInt(chave.substring(25, 34))
                + "</nNF><tpAmb>2</tpAmb></ide>"
                + "<emit><xNome>EMPRESA EXEMPLO LTDA</xNome></emit>"
                + "</infNFe>"
                + "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">"
                + "<SignedInfo/><SignatureValue>ZXhlbXBsbw==</SignatureValue>"
                + "</Signature>"
                + "</NFe>";
    }

    /** Só o infProt, como muitos ERPs guardam o retorno da SEFAZ. */
    static String infProt(String chave, String cStat, String xMotivo) {
        return "<infProt><tpAmb>2</tpAmb><chNFe>" + chave + "</chNFe>"
                + "<dhRecbto>2026-11-10T10:00:00-03:00</dhRecbto><nProt>135260000000001</nProt>"
                + "<cStat>" + cStat + "</cStat><xMotivo>" + xMotivo + "</xMotivo></infProt>";
    }

    static String infProtAutorizado(String chave) {
        return infProt(chave, "100", "Autorizado o uso da NF-e");
    }
}
