package com.fjvid;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class Servidor {
	private static final int PUERTO = 5555;
	private static final String ALG = "RSA";

	public Servidor() {
		ServerSocket socketSRV;
		
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALG);
			KeyPair clave = keyGen.generateKeyPair();
			
			
			socketSRV = new ServerSocket(PUERTO);
			System.out.printf("Servidor escuchando en: %s\n", socketSRV.getLocalSocketAddress().toString());
			int nCli = 0;
			
			
			while(true){
				Socket socketCLI = socketSRV.accept();
				nCli++;
				System.out.printf("\nAtendiendo al cliente nº: %d\n", nCli);
				
				new ServidorHilo(socketCLI, clave).start();
				
			}

		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) {
		new Servidor();
	}

}
