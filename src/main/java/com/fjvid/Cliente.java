package com.fjvid;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.security.PdfPKCS7;

public class Cliente {
	
	private static final String HOST = "localhost";
    private static final int PUERTO = 5555;
    private static final String ALG_AES = "AES";
    private static final String ALG_RSA = "RSA";
	
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		Scanner teclado = new Scanner(System.in);
		
		try {
			Socket socket = new Socket(HOST, PUERTO);
			System.out.println("Estableciendo conexión con el servidor");
			
			BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
			
			KeyGenerator generadorClave = KeyGenerator.getInstance(ALG_AES);
			generadorClave.init(256); // Iniciamos el generador con un tamaño de 256
			SecretKey claveAES = generadorClave.generateKey(); // Generamos la clave AES para enviar al servidor
			
			byte[] clavePubBytes = Base64.getDecoder().decode(entrada.readLine()); // Decodificamos la clave pública recibida en Base64 y la convertimos a formato X509
			KeyFactory factoria = KeyFactory.getInstance(ALG_RSA);
			
			PublicKey clavePub = factoria.generatePublic(new X509EncodedKeySpec(clavePubBytes)); // Conseguimos la clave publica definitiva en formato X509
			
			Cipher cifradorRSA = Cipher.getInstance(ALG_RSA);
			cifradorRSA.init(Cipher.ENCRYPT_MODE, clavePub);
			byte[] claveAESCifrada = cifradorRSA.doFinal(claveAES.getEncoded()); // Ciframos la clave AES con la clave pública del servidor para enviarla de forma segura
			
			salida.println(Base64.getEncoder().encodeToString(claveAESCifrada));// Enviamos al servidor la clave cifrada

			System.out.println("Clave AES cifrada enviada al servidor");

			
			System.out.println("Indica el archivo pdf deseado(pdf1.pdf, pdf2.pdf, pdf3.pdf, pdf4.pdf o pdf5.pdf)");
			String nombreArchivo = teclado.nextLine();
			salida.println(nombreArchivo); // Enviamos el nombre del archivo PDF que queremos descargar
			System.out.println("Solicitud de archivo enviada");
			
			System.out.println("Recibiendo PDF...");
			Cipher cifradoAES = Cipher.getInstance(ALG_AES);
		    cifradoAES.init(Cipher.DECRYPT_MODE, claveAES); // Inicializamos el cifrador AES en modo descifrado
		    
		   
		    while (entrada.ready()) entrada.read(); // Limpiamos el buffer
		    
		    byte[] buffer = new byte[4096];
		    InputStream is = socket.getInputStream();
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    
		    int numBytes;
		    while ((numBytes = is.read(buffer)) != -1) {
		        byte[] datosDescifrados = cifradoAES.update(buffer, 0, numBytes); // Desciframos los fragmentos del archivo PDF recibido
		        baos.write(datosDescifrados); // Escribimos los datos descifrados en el buffer
		    }
		    
		    byte[] datosFinales = cifradoAES.doFinal(); //Aplicamos el proceso de descifrado
		    baos.write(datosFinales); // Escribimos los datos finales descifrados
		    

		    FileOutputStream fos = new FileOutputStream("pdf_recibido_firmado.pdf"); //SE GENERA EL FICHERO PDF EN LA RAIZ DEL PROYECTO
		    baos.writeTo(fos); // Escribimos todo el contenido descifrado al archivo
		    fos.close();

            System.out.println("PDF firmado recibido");
            
            System.out.println("Verificando la firma del PDF...");

                // Cargar el certificado del servidor
                KeyStore keyStore = KeyStore.getInstance("PKCS12"); // Cargamos el almacén de claves (KeyStore) con el certificado del servidor
			FileInputStream fis = new FileInputStream("certificado.p12"); // Obtenemos el certificado CAMBAMOS EL NOMBRE DEL CERTIFICADO SI FUERA NECESARIO
                keyStore.load(fis, "123456".toCharArray()); // Cargamos la contraseña
                Certificate[] chain = keyStore.getCertificateChain("certificado_de_Usuario");
                X509Certificate serverCert = (X509Certificate) chain[0];

   
                PdfReader reader = new PdfReader("pdf_recibido_firmado.pdf"); // Preparamos el pdf recibido para su lectura
                AcroFields af = reader.getAcroFields();
                ArrayList<String> nombresFirmas = new ArrayList<>(af.getSignatureNames());

                if (nombresFirmas.isEmpty()) {
                    System.out.println("El PDF no contiene firmas.");
                    return;
                }

                for (String nombre : nombresFirmas) {
                    PdfPKCS7 pkcs7 = af.verifySignature(nombre); // Obtenemos información criptografica de la firma
                    X509Certificate certFirma = pkcs7.getSigningCertificate(); // Obtenemos el certificado

                    if (Arrays.equals(certFirma.getEncoded(), serverCert.getEncoded())) { // Comparamos el certificado del servidor con el incluido en la firma del PDF

						System.out.println("Certificado válido.");
                        boolean valido = pkcs7.verify();
                        if (valido) {
                            System.out.println("Firma válida.");
                        } else {
                            System.out.println("Firma inválida.");
                        }
                    }else {
                    	System.out.println("Certificado NO válido.");
                    }
                }
                reader.close();
                fis.close();
		
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

}

