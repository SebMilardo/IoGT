package it.unict.dieei.iogt;

import java.io.*;
import java.security.*;
import javax.net.ssl.*;

public class SecureServer {
   public static void main(String[] args) {
      String ksName = "/Users/Seby/testkey.jks";
      char ksPass[] = "password".toCharArray();
      char ctPass[] = "password".toCharArray();
      try {
         KeyStore ks = KeyStore.getInstance("JKS");
         ks.load(new FileInputStream(ksName), ksPass);
         KeyManagerFactory kmf = 
         KeyManagerFactory.getInstance("SunX509");
         kmf.init(ks, ctPass);
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(kmf.getKeyManagers(), null, null);
         SSLServerSocketFactory ssf = sc.getServerSocketFactory();
         SSLServerSocket s 
            = (SSLServerSocket) ssf.createServerSocket(8888);
         System.out.println("Server started:");
         
         // Listening to the port
         SSLSocket c = (SSLSocket) s.accept();
         
         BufferedWriter w = new BufferedWriter(
            new OutputStreamWriter(c.getOutputStream()));
         BufferedReader r = new BufferedReader(
            new InputStreamReader(c.getInputStream()));
         String m = r.readLine();
         w.write("HTTP/1.0 200 OK");
         w.newLine();
         w.write("Content-Type: text/html");
         w.newLine();
         w.newLine();
         w.write("<html><body>Hello world!</body></html>");
         w.newLine();
         w.flush();
         w.close();
         r.close();
         c.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}