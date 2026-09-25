# Firma digital de PDFs con cifrado híbrido

[![Estado](https://img.shields.io/badge/Estado-Finalizado-brightgreen)](https://github.com/FJVidalG/FirmaRemotaPDFConCertificado)
[![JDK](https://img.shields.io/badge/JDK-21-437291?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![Criptografía](https://img.shields.io/badge/Criptografía-RSA%2FAES-8A2BE2)](https://es.wikipedia.org/wiki/Criptografía_híbrida)
[![iText](https://img.shields.io/badge/iText-5.5.13.3-FF6F00)](https://itextpdf.com)
[![BouncyCastle](https://img.shields.io/badge/Bouncy_Castle-1.70-000000)](https://www.bouncycastle.org)
[![Licencia](https://img.shields.io/badge/Licencia-MIT-yellow)](https://opensource.org/licenses/MIT)

Sistema cliente-servidor en Java que firma documentos PDF con un certificado digital y los envía cifrados. Práctica del ciclo de Desarrollo de Aplicaciones Multiplataforma.

El cliente pide un PDF por su nombre. El servidor lo firma con su certificado, lo cifra y se lo envía. El cliente lo descifra y comprueba que la firma es válida y que la ha hecho ese servidor.

## Cómo funciona

El canal usa cifrado híbrido: RSA para intercambiar una clave de sesión y AES para cifrar el documento, que es la parte pesada.

1. Al arrancar, el servidor genera un par de claves RSA y escucha en el puerto 5555. Cada cliente se atiende en su propio hilo (`ServidorHilo`).
2. El servidor envía su clave pública al cliente, codificada en Base64.
3. El cliente genera una clave AES de 256 bits, la cifra con la clave pública del servidor y se la envía. Solo el servidor puede descifrarla, con su clave privada.
4. El cliente pide un PDF por su nombre y el servidor lo busca en la carpeta `ficheros/`.
5. El servidor firma el PDF con iText, usando la clave privada del certificado PKCS#12 (firma CMS con SHA-256). Después lo cifra con la clave AES y lo envía.
6. El cliente descifra el archivo, lo guarda como `pdf_recibido_firmado.pdf` y verifica la firma: comprueba que el certificado que firma es el del servidor y que el documento no se ha modificado después de firmarlo.

## Tecnologías

| Componente | Tecnología | Para qué |
|---|---|---|
| Lenguaje | Java | Desarrollado con JDK 21. El `pom.xml` compila para Java 8 |
| Comunicación | Sockets TCP | `ServerSocket` en el servidor, `Socket` en el cliente |
| Criptografía | RSA y AES | RSA para el intercambio de claves, AES para cifrar el documento |
| Firma de PDF | [iText 5.5.13.3](https://itextpdf.com/) | Firma y verificación de firmas en PDF |
| Proveedor criptográfico | [Bouncy Castle 1.70](https://www.bouncycastle.org/) | Algoritmos que usa iText para la firma |
| Construcción | Maven | Dependencias y JAR ejecutable con el plugin Shade |

## Estructura

```text
.
├── src/main/java/com/fjvid/
│   ├── Servidor.java        Genera las claves RSA y acepta conexiones
│   ├── ServidorHilo.java    Atiende a un cliente: intercambio de claves, firma y envío
│   └── Cliente.java         Pide el PDF, lo descifra y verifica la firma
├── ficheros/                PDFs de prueba que sirve el servidor
│   ├── pdf1.pdf
│   ├── pdf2.pdf
│   └── ...
├── pom.xml
└── README.md
```

## Requisitos

- JDK 8 o superior
- Maven
- OpenSSL, para generar el certificado de prueba

## Generar el certificado

El servidor firma con un certificado en formato PKCS#12. Para pruebas vale uno autofirmado. En Linux:

```bash
# Clave privada RSA
openssl genpkey -algorithm RSA -out clave-privada.pem -pkeyopt rsa_keygen_bits:4096

# Certificado autofirmado, válido un año
openssl req -new -x509 -key clave-privada.pem -out certificado-publico.crt -days 365

# Conversión a PKCS#12, que es el formato que lee Java
openssl pkcs12 -export \
  -inkey clave-privada.pem \
  -in certificado-publico.crt \
  -out certificado.p12 \
  -name "certificado_de_Usuario" \
  -password pass:123456
```

El programa espera estos valores, que están fijados en el código:

- Archivo `certificado.p12` en la raíz del proyecto.
- Alias `certificado_de_Usuario`.
- Contraseña `123456`.

Si cambias alguno, cámbialo también en `ServidorHilo.java` y en `Cliente.java`. Los archivos `.p12` y `.pem` no deben subirse nunca al repositorio.

## Ejecución

Compilar desde la raíz del proyecto:

```bash
mvn clean package
```

Arrancar el servidor:

```bash
java -jar target/FirmaRemotaPDFConCertificado-0.0.1-SNAPSHOT.jar
```

El servidor muestra `Servidor escuchando en: 0.0.0.0/0.0.0.0:5555`.

Arrancar el cliente en otra terminal:

```bash
java -cp target/FirmaRemotaPDFConCertificado-0.0.1-SNAPSHOT.jar com.fjvid.Cliente
```

El cliente pide el nombre del documento (por ejemplo `pdf1.pdf`), lo recibe firmado, lo guarda como `pdf_recibido_firmado.pdf` en la raíz del proyecto y muestra si el certificado y la firma son válidos.

## Limitaciones conocidas

- **El modo de cifrado AES no está indicado.** `Cipher.getInstance("AES")` usa por defecto el modo ECB, que deja ver patrones del documento y no detecta si se ha modificado por el camino. Lo correcto sería `AES/GCM/NoPadding` con un vector de inicialización aleatorio.
- **El tamaño de la clave RSA del canal no está fijado.** Sin llamar a `initialize()` se usa el valor por defecto del JDK: 2048 bits hasta Java 18 y 3072 a partir de Java 19. Convendría fijarlo de forma explícita. El certificado de firma es aparte y usa la clave que se genere con OpenSSL.
- **El relleno RSA es PKCS#1 v1.5**, que se considera antiguo. Hoy se recomienda OAEP.
- **El cliente no autentica la clave pública que recibe.** Alguien situado en medio de la conexión podría enviar la suya y leer el documento. La firma del PDF sí se comprueba contra el certificado, pero el canal no. En producción el canal iría sobre TLS, con autenticación mutua si hiciera falta.
- **El nombre del archivo que pide el cliente no se valida.** Habría que impedir rutas como `../` para que no se puedan pedir archivos fuera de `ficheros/`.
- **Si el archivo no existe, el cliente no lo detecta** e intenta descifrar la respuesta de error como si fuera el documento.
- **La contraseña del certificado está en el código.** Con un certificado de prueba no importa. En un sistema real se leería de la configuración o de un almacén seguro.
- **Los mensajes se escriben con `System.out`.** Con un sistema de registro como Log4j2 se podrían filtrar por nivel y guardar en archivo.

## Autor

Francisco Jesús Vidal García. [GitHub](https://github.com/FJVidalG) | [LinkedIn](https://www.linkedin.com/in/francisco-jesus-vidal-garcia)

**Francisco Jesús Vidal García**  
[![Email](https://img.shields.io/badge/📧_Email-fjvidalgarcia%40gmail.com-%23007EC6?style=flat&logo=gmail&logoColor=white)](mailto:fjvidalgarcia@gmail.com)  
[![LinkedIn](https://img.shields.io/badge/🔗_LinkedIn-Francisco_Vidal-%230A66C2?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/francisco-jes%C3%BAs-vidal-garc%C3%ADa-174189336/)
