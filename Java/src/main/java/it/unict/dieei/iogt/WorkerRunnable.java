/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unict.dieei.iogt;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class WorkerRunnable implements Runnable {

    protected Socket clientSocket = null;
    protected String serverText = null;

    public WorkerRunnable(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText = serverText;
    }

    @Override
    public void run() {
        try {
            long time;
            try (InputStream input = clientSocket.getInputStream()) {
                    ObjectInputStream ois = new ObjectInputStream(input);
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    final Gson gson = new Gson();
                    SensedData data = gson.fromJson((String)ois.readObject(), SensedData.class);
                    
                    System.out.println(data.toString());
                    oos.writeObject("ENABLE_CAMERA -1.1 -0.7 -1.2 -1.4 -0.1 -0.2");
                    
                    
                    
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(WorkerRunnable.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                clientSocket.close();
            }
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }
}
