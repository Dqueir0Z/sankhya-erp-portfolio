package io.github.dqueir0z.erp.nfe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Collections;

import static io.github.dqueir0z.erp.nfe.XmlExemplo.CHAVE_123;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova criptográfica de que montar o nfeProc não invalida a assinatura da nota: assina o
 * {@code <infNFe>} como a NF-e é assinada (enveloped, C14N inclusiva, referência ao Id),
 * monta o nfeProc e valida a assinatura no resultado. O certificado é uma chave RSA gerada
 * no teste, não um certificado ICP-Brasil.
 */
class AssinaturaPreservadaTest {

    private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    private static KeyPair chaves;

    @BeforeAll
    static void gerarChaves() throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        chaves = gerador.generateKeyPair();
    }

    @Test
    void assinaturaContinuaValidaDentroDoNfeProc() throws Exception {
        String nfeAssinada = assinar(nfeSemAssinatura(CHAVE_123));
        assertTrue(assinaturaValida(nfeAssinada), "pré-condição: a nota assinada é válida sozinha");

        NfeProc proc = MontadorNfeProc.montar(nfeAssinada, XmlExemplo.infProtAutorizado(CHAVE_123));

        assertTrue(assinaturaValida(proc.getXml()));
    }

    @Test
    void validacaoDetectaConteudoAlterado() throws Exception {
        // Controle: se o teste acima aceitasse qualquer coisa, este também passaria como válido.
        String nfeAssinada = assinar(nfeSemAssinatura(CHAVE_123));
        String adulterada = nfeAssinada.replace("EMPRESA EXEMPLO LTDA", "EMPRESA ALTERADA LTDA");

        assertFalse(assinaturaValida(adulterada));
    }

    private static String nfeSemAssinatura(String chave) {
        return "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                + "<infNFe versao=\"4.00\" Id=\"NFe" + chave + "\">"
                + "<ide><cUF>35</cUF><mod>55</mod><serie>1</serie><nNF>123</nNF><tpAmb>2</tpAmb></ide>"
                + "<emit><xNome>EMPRESA EXEMPLO LTDA</xNome></emit>"
                + "</infNFe></NFe>";
    }

    private static String assinar(String xml) throws Exception {
        Document doc = ler(xml);
        Element infNfe = infNfe(doc);

        XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
        Reference referencia = fabrica.newReference("#" + infNfe.getAttribute("Id"),
                fabrica.newDigestMethod(DigestMethod.SHA256, null),
                Arrays.asList(
                        fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                        fabrica.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null)),
                null, null);
        SignedInfo info = fabrica.newSignedInfo(
                fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fabrica.newSignatureMethod(RSA_SHA256, null),
                Collections.singletonList(referencia));
        KeyInfoFactory chavesInfo = fabrica.getKeyInfoFactory();

        // Como na NF-e, a <Signature> fica dentro de <NFe>, depois de <infNFe>.
        fabrica.newXMLSignature(info, chavesInfo.newKeyInfo(Collections.singletonList(
                        chavesInfo.newKeyValue(chaves.getPublic()))))
                .sign(new DOMSignContext(chaves.getPrivate(), doc.getDocumentElement()));
        return escrever(doc);
    }

    private static boolean assinaturaValida(String xml) throws Exception {
        Document doc = ler(xml);
        infNfe(doc);
        Element assinatura = (Element) doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        DOMValidateContext contexto = new DOMValidateContext(chaves.getPublic(), assinatura);
        return XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto).validate(contexto);
    }

    /** Marca o atributo Id para a referência "#NFe..." da assinatura ser resolvida. */
    private static Element infNfe(Document doc) {
        Element infNfe = (Element) doc.getElementsByTagNameNS(MontadorNfeProc.NAMESPACE_NFE, "infNFe").item(0);
        infNfe.setIdAttribute("Id", true);
        return infNfe;
    }

    private static Document ler(String xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        return fabrica.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String escrever(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter saida = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(saida));
        return saida.toString();
    }
}
