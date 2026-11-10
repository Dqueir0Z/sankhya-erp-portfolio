package io.github.dqueir0z.erp.nfe;

/** O XML da nota ou do protocolo não pode ser usado para montar o nfeProc. */
public class XmlNfeInvalidoException extends Exception {

    private static final long serialVersionUID = 1L;

    public XmlNfeInvalidoException(String mensagem) {
        super(mensagem);
    }

    public XmlNfeInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
