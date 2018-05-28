/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unict.dieei.iogt;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class ECDSAExample {

    public static void main(String[] args) throws Exception {
        Signature s = Signature.getInstance("SHA256withECDSA");
        BigInteger publicKey = new BigInteger("3059301306072a8648ce3d020106082a8648ce3d0301070342000473a00e38f82e90bf8e9aad51be68150eb14b939c1d8fdeed8b98a27f54cb6faf3942511ec4d995122f27f9778256a430b1ab35c56ffe77019cb94863afc37249", 16);
        BigInteger signatureKey = new BigInteger("3045022100a7a9fe03a8cb03cae6daf35c46b9cb9073e573e287bfbb9294837ab06cecb12102201777d28c273ca9b8f4fc2e74ebed775418ab6e48ee6f023d544e23bef476642c", 16);
        
        
        byte[] byteKey = publicKey.toByteArray();
        X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
        KeyFactory kf = KeyFactory.getInstance("EC");

        
        s.initVerify(kf.generatePublic(X509publicKey));
        s.update("Ciao".getBytes());
        boolean valid = s.verify(signatureKey.toByteArray());
        
        System.out.println(valid);
    }
}