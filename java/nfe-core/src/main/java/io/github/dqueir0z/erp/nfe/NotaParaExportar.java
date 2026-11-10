package io.github.dqueir0z.erp.nfe;

/** Dados de uma nota lidos do ERP para exportação. */
public final class NotaParaExportar {

    private final long nunota;
    private final Long numeroNota;
    private final String xmlNfe;
    private final String xmlProtocolo;

    /**
     * @param nunota       número único da nota no ERP (TGFCAB.NUNOTA)
     * @param numeroNota   número fiscal da nota (TGFCAB.NUMNOTA); pode ser nulo
     * @param xmlNfe       XML da nota
     * @param xmlProtocolo protocolo de autorização ({@code <protNFe>} ou {@code <infProt>})
     */
    public NotaParaExportar(long nunota, Long numeroNota, String xmlNfe, String xmlProtocolo) {
        this.nunota = nunota;
        this.numeroNota = numeroNota;
        this.xmlNfe = xmlNfe;
        this.xmlProtocolo = xmlProtocolo;
    }

    public long getNunota() {
        return nunota;
    }

    public Long getNumeroNota() {
        return numeroNota;
    }

    public String getXmlNfe() {
        return xmlNfe;
    }

    public String getXmlProtocolo() {
        return xmlProtocolo;
    }
}
