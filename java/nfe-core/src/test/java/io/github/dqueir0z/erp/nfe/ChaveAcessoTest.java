package io.github.dqueir0z.erp.nfe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaveAcessoTest {

    @Test
    void aceitaChaveComDigitoCorreto() {
        assertTrue(ChaveAcesso.valida(XmlExemplo.CHAVE_123));
        assertTrue(ChaveAcesso.valida(XmlExemplo.CHAVE_124));
    }

    @Test
    void recusaDigitoVerificadorErrado() {
        String errada = XmlExemplo.CHAVE_123.substring(0, 43) + "5";
        assertFalse(ChaveAcesso.valida(errada));
    }

    @Test
    void recusaTamanhoOuCaracteresInvalidos() {
        assertFalse(ChaveAcesso.valida(null));
        assertFalse(ChaveAcesso.valida("123"));
        assertFalse(ChaveAcesso.valida(XmlExemplo.CHAVE_123.replace('5', 'A')));
    }

    @Test
    void restoZeroViraDigitoZero() {
        // 43 zeros: soma 0, resto 0, dígito 0.
        assertEquals(0, ChaveAcesso.digitoVerificador(repetir('0', 43)));
    }

    @Test
    void restoUmTambemViraDigitoZero() {
        // Último dígito 6 com peso 2: soma 12, resto 1. Pela regra o dígito é 0, não 11 - 1 = 10.
        assertEquals(0, ChaveAcesso.digitoVerificador(repetir('0', 42) + "6"));
    }

    @Test
    void restoMaiorQueUmUsaOnzeMenosResto() {
        // Último dígito 1 com peso 2: soma 2, resto 2, dígito 9.
        assertEquals(9, ChaveAcesso.digitoVerificador(repetir('0', 42) + "1"));
    }

    @Test
    void recusaDigitosDeOutrosAlfabetos() {
        // '٣' é o 3 arábico-índico: Character.isDigit aceitaria, mas não é chave válida.
        String comDigitoArabe = XmlExemplo.CHAVE_123.substring(0, 10) + '٣' + XmlExemplo.CHAVE_123.substring(11);
        assertFalse(ChaveAcesso.valida(comDigitoArabe));
        assertThrows(IllegalArgumentException.class,
                () -> ChaveAcesso.digitoVerificador(comDigitoArabe.substring(0, 43)));
    }

    @Test
    void exigeQuarentaETresDigitosParaCalcular() {
        assertThrows(IllegalArgumentException.class, () -> ChaveAcesso.digitoVerificador("12"));
    }

    private static String repetir(char c, int vezes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vezes; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
