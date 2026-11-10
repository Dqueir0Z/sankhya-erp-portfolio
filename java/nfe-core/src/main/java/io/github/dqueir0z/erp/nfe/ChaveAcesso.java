package io.github.dqueir0z.erp.nfe;

/**
 * Chave de acesso da NF-e: 44 dígitos, sendo o último o dígito verificador
 * calculado por módulo 11 (pesos 2 a 9, da direita para a esquerda).
 */
public final class ChaveAcesso {

    public static final int TAMANHO = 44;

    private ChaveAcesso() {
    }

    public static boolean valida(String chave) {
        if (chave == null || chave.length() != TAMANHO || !soDigitosAscii(chave)) {
            return false;
        }
        return digitoVerificador(chave.substring(0, TAMANHO - 1)) == chave.charAt(TAMANHO - 1) - '0';
    }

    /** Calcula o dígito verificador para os 43 primeiros dígitos da chave. */
    public static int digitoVerificador(String chaveSemDigito) {
        if (chaveSemDigito == null || chaveSemDigito.length() != TAMANHO - 1 || !soDigitosAscii(chaveSemDigito)) {
            throw new IllegalArgumentException("Informe os 43 dígitos iniciais da chave.");
        }
        int soma = 0;
        int peso = 2;
        for (int i = chaveSemDigito.length() - 1; i >= 0; i--) {
            soma += (chaveSemDigito.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Só 0 a 9: {@link Character#isDigit} também aceita dígitos de outros alfabetos. */
    private static boolean soDigitosAscii(String texto) {
        return texto.chars().allMatch(c -> c >= '0' && c <= '9');
    }
}
