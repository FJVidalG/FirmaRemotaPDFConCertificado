package com.fjvid;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.PrivateKeySignature;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ServidorHilo extends Thread{
	private Socket socket;
	private KeyPair clave;

	// Datos del certificado generado localmente (almacenado en formato PKCS#12)
	private static final String RUTA_CERT = "certificado.p12"; // CAMBIAMOS A LA RUTA DEL CERTIFICADO QUE HAYAMOS CREADO
    private static final String CONT_CERT = "123456"; // CONTRASEÑA DEL CERTIFICADO
    private static final String NOM_CERT = "certificado_de_Usuario";
	
	public ServidorHilo(Socket socket, KeyPair clave) {
		this.socket = socket;
		this.clave = clave;
	}
	
	private void firmarPDF(File ficheroEntrada, File ficheroSalida) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        try (FileInputStream fis = new FileInputStream(RUTA_CERT)) { // Abrimos archivo .p12 del certificado
            KeyStore keyStore = KeyStore.getInstance("PKCS12"); // Creamos la clave PKCS12 estandar para certificados
            keyStore.load(fis, CONT_CERT.toCharArray());
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(NOM_CERT, CONT_CERT.toCharArray()); // Cargamos el certificado con su contraseña
            Certificate[] chain = keyStore.getCertificateChain(NOM_CERT);
            
            try (FileInputStream inputPdf = new FileInputStream(ficheroEntrada);
                    FileOutputStream fos = new FileOutputStream(ficheroSalida)) {

                   PdfReader reader = new PdfReader(inputPdf); // Prepara el PDF para la firma
                   PdfStamper stamper = PdfStamper.createSignature(reader, fos, '\0');
                   
                   // Modificamos la apariencia de la firma
                   PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
                   appearance.setReason("Firma Digital");
                   appearance.setLocation("España");
                   appearance.setSignatureCreator("Firma automática");
                   
                   // Creamos la firma digital utilizando la clave privada y los certificados
                   ExternalSignature signature = new PrivateKeySignature(privateKey, "SHA-256", BouncyCastleProvider.PROVIDER_NAME); // Usamos clave privada para la firma
                   ExternalDigest digest = new BouncyCastleDigest();

                   
                   MakeSignature.signDetached(appearance, digest, signature, chain, null, null, null, 0, MakeSignature.CryptoStandard.CMS); //Firmamos el PDF

                   stamper.close();
                   System.out.println("PDF firmado con éxito.");
            }
            
        }
    }
	
	@Override
	public void run() {
		try {
			System.out.println("Iniciando intercambio de claves con el cliente");
			Cipher cifrador = Cipher.getInstance("RSA");
			
			BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
			
			System.out.println("Enviando clave pública...");
			String clavePub = Base64.getEncoder().encodeToString(clave.getPublic().getEncoded()); // Codificamos la clave pública en Base64 para enviarla al cliente
			salida.println(clavePub); // La enviamos al cliente
			
			System.out.println("Recibiendo clave AES del cliente...");
			byte[] claveAESCifrada = Base64.getDecoder().decode(entrada.readLine().getBytes()); // Recibimos la clave AES cifrada con la clave pública del servidor
			
			System.out.println("Desencriptando clave AES del cliente...");
			cifrador.init(Cipher.DECRYPT_MODE, clave.getPrivate());
			byte[] claveAES = cifrador.doFinal(claveAESCifrada); // Desciframos la clave AES usando la clave privada del servidor
			
			System.out.println("Esperando solicitud de pdf...");
			String PDF = entrada.readLine();
			
			System.out.printf("PDF solicitado %s\n", PDF);
			File fichero = new File("ficheros/" + PDF);
			if (!fichero.exists()) {
			    System.out.println("No existe PDF, cerrando conexión");
			    salida.println("ERROR: Archivo no encontrado"); // Enviar mensaje de error
			    socket.close();
			    return;
			} else {
			    salida.println("OK"); // Confirmamos al cliente que el archivo existe
			}
			
			// Firmamos el archivo PDF
	        String ficheroFirmado = "ficheros/firmado_" + PDF;
	        firmarPDF(fichero, new File(ficheroFirmado)); // Firmamos el archivo PDF utilizando el certificado y clave privada del servidor
	        
	        System.out.println("Cifrando archivo PDF firmado con AES...");
	        Cipher cifradoAES = Cipher.getInstance("AES");
	        cifradoAES.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(claveAES, "AES"));
	        
	        FileInputStream fis = new FileInputStream(ficheroFirmado);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        byte[] buffer = new byte[4096];
	        int numBytes;
	        
	        while ((numBytes = fis.read(buffer)) != -1) {
	            baos.write(cifradoAES.update(buffer, 0, numBytes));
	        }
	        baos.write(cifradoAES.doFinal());
	        
	        byte[] archivoCifrado = baos.toByteArray(); // Obtenemos el archivo cifrado
	        
	        
	        OutputStream os = socket.getOutputStream();
	        os.write(archivoCifrado); // Enviamos el archivo cifrado al cliente
	        os.flush();
	        
	        fis.close();
	        baos.close();
	        os.close();
	        entrada.close();
	        System.out.println("Archivo firmado y cifrado enviado al cliente.");
	        socket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
