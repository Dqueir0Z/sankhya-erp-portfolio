package io.github.dqueir0z.erp.nfe;

import java.util.Collections;
import java.util.List;

/** ZIP gerado e o motivo de cada nota que ficou de fora. */
public final class ResultadoExportacao {

    private final byte[] zip;
    private final List<String> arquivos;
    private final List<String> ignoradas;

    ResultadoExportacao(byte[] zip, List<String> arquivos, List<String> ignoradas) {
        this.zip = zip;
        this.arquivos = Collections.unmodifiableList(arquivos);
        this.ignoradas = Collections.unmodifiableList(ignoradas);
    }

    /** Conteúdo do ZIP, ou {@code null} quando nenhuma nota pôde ser exportada. */
    public byte[] getZip() {
        return zip == null ? null : zip.clone();
    }

    public boolean temArquivos() {
        return !arquivos.isEmpty();
    }

    /** Nomes dos arquivos dentro do ZIP, na ordem em que foram gravados. */
    public List<String> getArquivos() {
        return arquivos;
    }

    /** Uma mensagem por nota ignorada, no formato "Nota 123: motivo". */
    public List<String> getIgnoradas() {
        return ignoradas;
    }
}
