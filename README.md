# 🖩 Sistema de Firma Digital de PDFs con Cifrado Híbrido

[![Estado](https://img.shields.io/badge/Estado-✅_Estable-brightgreen)](https://github.com/FJVidalG/FirmaRemotaPDFConCertificado)
[![Java](https://img.shields.io/badge/Java-21-FF160C?logo=java&logoColor=white)](https://java.com)
[![Maven](https://img.shields.io/badge/Maven-3.8.6-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org)
[![Criptografía](https://img.shields.io/badge/Criptografía-RSA%2FAES-8A2BE2)](https://es.wikipedia.org/wiki/Criptografía_híbrida)
[![iText](https://img.shields.io/badge/iText-5.5.13-FF6F00)](https://itextpdf.com)
[![BouncyCastle](https://img.shields.io/badge/Bouncy_Castle-1.70-000000)](https://www.bouncycastle.org)
[![Licencia](https://img.shields.io/badge/Licencia-MIT-yellow)](https://opensource.org/licenses/MIT)

Sistema cliente-servidor para firma digital de documentos PDF utilizando cifrado híbrido (RSA + AES) con validación de integridad.

## 🔧 Tecnologías y Librerías

| Componente          | Tecnología/Librería                                                                 | Función                                                                 |
|---------------------|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| Lenguaje principal  | **Java 21**                                                                         | Desarrollo del núcleo lógico                                           |
| Comunicación        | **Sockets TCP**                                                                     | Interacción cliente-servidor                                           |
| Criptografía        | **RSA (4096 bits)** para intercambio de claves<br>**AES-256** para cifrado de datos | Cifrado híbrido                                                        |
| Firma PDF           | **[iText 5.5.13](https://itextpdf.com/)**                                          | Manipulación y firma digital de documentos PDF                         |
| Seguridad           | **[Bouncy Castle 1.69](https://www.bouncycastle.org/)**                            | Proveedor criptográfico para algoritmos avanzados                      |
| Gestión de proyecto | **Maven**                                                                           | Gestión de dependencias y construcción                                 |

## 📁 Estructura del Proyecto

```text
.
├── src/
│   ├── main/java/com/fjvid/
│   │   ├── Cliente.java          # Lógica del cliente
│   │   ├── Servidor.java         # Lógica del servidor principal
│   │   └── ServidorHilo.java     # Manejo de clientes concurrentes
│   └── test/                     # Pruebas unitarias (opcional)
├── ficheros/                     # Almacenamiento de los PDFs del servidor
│   ├── pdf1.pdf                  # Ejemplos de documentos
│   ├── pdf2.pdf                  
│   └── ...
├── pom.xml                       # Configuración Maven
└── README.md                     # Este archivo
```

## ⚙️ Flujo de Operación

1. **Inicio del Servidor**  
   - Genera par de claves RSA
   - Espera conexiones en el puerto 5555

2. **Conexión del Cliente**  
   - Establece conexión TCP
   - Genera clave AES-256
   - Cifra clave AES con clave pública del servidor (RSA)
   - Solicita documento PDF

3. **Procesamiento en Servidor**  
   - Descifra clave AES recibida
   - Firma digitalmente el PDF solicitado
   - Cifra el PDF firmado con AES
   - Envía el documento cifrado

4. **Validación en Cliente**  
   - Descifra el PDF recibido
   - Verifica firma digital con certificado
   - Valida integridad del documento

## 🛠️ Configuración Requerida

### Prerrequisitos
- Java JDK 21+
- Maven 3.6+
- OpenSSL (para generar certificados de prueba)

### Generación de Certificado (Linux-Ubuntu)
```bash
# 1. Generar clave privada RSA
openssl genpkey -algorithm RSA -out clave-privada.pem -pkeyopt rsa_keygen_bits:4096

# 2. Crear certificado autofirmado (validez 1 año)
openssl req -new -x509 -key clave-privada.pem -out certificado-publico.crt -days 365

# 3. Convertir a formato PKCS#12 (requerido por Java)
openssl pkcs12 -export \
  -inkey clave-privada.pem \
  -in certificado-publico.crt \
  -out certificado.p12 \
  -name "certificado_de_Usuario" \
  -password pass:123456
```

## **⚠️ Consideraciones Clave**  
> - **Ubicación del certificado:**  
>   Colocar `certificado.p12` (o como se haya nombrado) en el directorio raíz del proyecto  
> - **Credenciales:**   
>   ***Nombre por defecto del certificado:*** `certificado_de_Usuario` (modificar en código si se cambia)  
>   ***Contraseña por defecto:*** `123456` (modificar en código si se cambia)  
> - **Seguridad:**  
>   Nunca subir archivos `.p12` o `.pem` al repositorio

## 🚀 Ejecución del Sistema
**1. Compilación y Empaquetado**
```bash
# En la raiz del proyecto
mvn clean package
```
**2. Iniciar Servidor**
```bash
java -cp target/your-project.jar com.fjvid.Servidor
```
**3. Ejecutar Cliente**
```bash
# En otra terminal
java -cp target/your-project.jar com.fjvid.Cliente
```
## Flujo típico de uso:

1. Servidor inicia y muestra: Servidor escuchando en: 0.0.0.0:5555

2. Cliente se conecta y muestra opciones de PDF disponibles

3. Usuario selecciona documento (ej: pdf1.pdf)

4. Servidor procesa y envía pdf_recibido_firmado.pdf

5. Cliente valida firma e integridad automáticamente

## 🔐 Consideraciones de Seguridad

🔐 **Almacenamiento seguro:** Las claves privadas deben guardarse en HSM  
🔄 **Renovación de claves:** Rotar claves RSA periódicamente  
📜 **Validación de certificados:** Usar CA de confianza  
🔒 **Protección de tráfico:** Implementar TLS

## 📌 Notas Adicionales

📚 **Propósito educativo:** Demostración de criptografía aplicada  

💡 **Mejoras posibles:**  
  🛠️ Implementar logger profesional (Log4j2)  
  🔑 Añadir sistema de autenticación mutua  
  🖥️ Desarrollar interfaz gráfica  

📄 **Archivos de prueba:** PDFs con texto lorem ipsum

## 👤 Autor

**Francisco Jesús Vidal García**  
[![Email](https://img.shields.io/badge/📧_Email-fjvidalgarcia%40gmail.com-%23007EC6?style=flat&logo=gmail&logoColor=white)](mailto:fjvidalgarcia@gmail.com)  
[![LinkedIn](https://img.shields.io/badge/🔗_LinkedIn-Francisco_Vidal-%230A66C2?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/francisco-jes%C3%BAs-vidal-garc%C3%ADa-174189336/)
