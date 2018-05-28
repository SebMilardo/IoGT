/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unict.dieei.iogt;

/**
 *
 * @author Seby
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {    
        MultiThreadedServer server = new MultiThreadedServer(9100);
        Thread t = new Thread(server);
        t.start();
        
        try {
            Thread.sleep(200 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping Server");
        server.stop();
    }
}
