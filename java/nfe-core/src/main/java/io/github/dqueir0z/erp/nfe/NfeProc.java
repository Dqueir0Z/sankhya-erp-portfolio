package io.github.dqueir0z.erp.nfe;

/** XML de distribuição da NF-e (nota + protocolo de autorização) pronto para entregar. */
public final class NfeProc {

    private final String chave;
    private final String xml;

    NfeProc(String chave, String xml) {
        this.chave = chave;
        this.xml = xml;
    }

    public String getChave() {
        return chave;
    }

    public String getXml() {
        return xml;
    }
}
