/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdserver;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.net.*;
import java.io.*;
import java.util.*;
import javafx.stage.WindowEvent;
import java.net.InetAddress;

/**
 *
 * @author freddie
 */
class SServer extends Thread {

    public int port;
    public ServerSocket server;
    Socket clientSocket;
    public BufferedWriter output = null;
    public BufferedReader input = null;

    public SServer MonitorServer;

    public void setExServer(SServer server) {
        MonitorServer = server;
    }

    class client extends Thread {

        public client(Socket clientSocket, BufferedReader input, BufferedWriter output) {
            this.clientSocket = clientSocket;
            this.output = output;
            this.input = input;
            //  SocketReaderThread reader = new SocketReaderThread;
            // reader.start();
        }
        public boolean ready = false;
        public Socket clientSocket;
        public BufferedWriter output = null;
        public BufferedReader input = null;

        // class SocketReaderThread extends Thread {
        @Override
        public void run() {
            if (clientSocket != null && clientSocket.isConnected()) {
                //onClosedStatus(false) ;
            }
            while (true) {
                try {
                    if (input != null) {
                        String line;
                        while ((line = input.readLine()) != null) {
                            String logMsg = "recv> " + line;
                            System.out.println(logMsg);
                            for (int i = 0; i < MonitorServer.clientList.size(); i++) {
                                client c = MonitorServer.clientList.get(i);
                                c.output.write(logMsg, 0, logMsg.length());
                                c.output.newLine();
                                c.output.flush();
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }

    List<client> clientList = new ArrayList<client>();

    public SServer(int port) {
        this.port = port;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void closeAllSocket() {

        for (int i = 0; i < clientList.size(); i++) {
            try {
                Socket ct = clientList.get(i).clientSocket;

                System.out.printf("close: %d Local: %s Remote %s %04d\n", i, ct.getLocalAddress().toString(),
                        ct.getInetAddress().toString(), ct.getLocalPort());
                if (ct.isConnected()) {
                    System.out.println("dddddddddddddddddd");
                    ct.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }

    }

    public void run() {
        client c;
        int i = 1;
        while (true) {
            try {
                clientSocket = server.accept();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    input = new BufferedReader(new InputStreamReader(
                            clientSocket.getInputStream()));
                    output = new BufferedWriter(new OutputStreamWriter(
                            clientSocket.getOutputStream()));
                    output.flush();
                    InetAddress ad = clientSocket.getInetAddress();
                    System.out.printf("connected: %d Local: %s Remote %s %04d\n", i++, clientSocket.getLocalAddress().toString(),
                            clientSocket.getInetAddress().toString(), clientSocket.getLocalPort());
                    c = new client(clientSocket, input, output);
                    clientList.add(c);
                    c.start();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

}

public class MDServer extends Application {

    public SServer s1;
    public SServer monserver;

    @Override
    public void start(Stage primaryStage) {

        int portNumber = 2056;
        s1 = new SServer(portNumber);
        s1.start();

        int MonportNumber = 2015;
        monserver = new SServer(MonportNumber);
        monserver.start();

        s1.setExServer(monserver);

        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
                s1.closeAllSocket();

            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);

        Scene scene = new Scene(root, 300, 250);

        primaryStage.setTitle("Hello World!");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                System.out.println("Close World!");

            }
        });

        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}

//        private synchronized void waitForReady() {
//            while (!ready) {
//                try {
//                    wait();
//                } catch (InterruptedException e) {
//                }
//            }
//        }
//
//        private synchronized void notifyReady() {
//            ready = true;
//            notifyAll();
//        }
