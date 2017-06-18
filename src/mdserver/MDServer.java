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
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.*;
import java.io.*;
import java.util.*;
import javafx.stage.WindowEvent;
import java.net.InetAddress;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import java.nio.ByteBuffer;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.SelectionMode;
import java.nio.charset.Charset;

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

    private ObservableList<String> MonitorList;

    public SServer MonitorServer;
    ListView connectlist;
    private boolean blistening = false;
    private boolean bReceiving = false;

    public void setExServer(SServer server) {
        MonitorServer = server;
    }

    public void setLog(ObservableList<String> list) {
        MonitorList = list;
    }

    public void log(String s) {
        MonitorList.add(s);
    }

    class client extends Thread {

        public client(Socket clientSocket, BufferedReader input, BufferedWriter output) {
            this.clientSocket = clientSocket;
            this.output = output;
            this.input = input;
        }

        public double[] toDouble(byte[] bytes, int num) {
            double[] da = new double[num / 8];
            for (int i = 0; i < num / 8; i++) {
                byte[] te = new byte[8];
                System.arraycopy(bytes, i * 8, te, 0, 8);
                da[i] = ByteBuffer.wrap(te).getDouble();
            }
            return da;
        }

        public boolean ready = false;
        public Socket clientSocket;
        public BufferedWriter output = null;
        public BufferedReader input = null;

        char[] buf = new char[1024 * 1024];

        // class SocketReaderThread extends Thread {
        @Override
        public void run() {
            if (clientSocket != null && clientSocket.isConnected()) {
                //onClosedStatus(false) ;
            }
            while (bReceiving) {
                try {
                    if (input != null) {
                        //String line;
                        int nrLen;
                        nrLen = input.read(buf);
                        System.out.printf("%d\n", nrLen);
                        byte[] bt = new byte[nrLen];
                        for (int i = 0; i < nrLen; i++) {
                            bt[i] = (byte) buf[i];
                        }
                        double[] data = toDouble(bt, nrLen);
                        for (int i = 0; i < nrLen / 8; i++) {
                            System.out.println(data[i]);
                        }

                        for (int i = 0; i < MonitorServer.clientList.size(); i++) {
                           client c = MonitorServer.clientList.get(i);
                           c.output.write(buf, 0, nrLen);
                           c.output.flush();
                            System.out.printf("Send******************%d\n", nrLen);
                       }

//                        while ((line = input.readLine()) != null) {
//                           // String logMsg = "recv> " + line;
//                           // System.out.println(logMsg);
//                            for (int i = 0; i < MonitorServer.clientList.size(); i++) {
//                                client c = MonitorServer.clientList.get(i);
//                                c.output.write(logMsg, 0, logMsg.length());
//                                c.output.newLine();
//                                c.output.flush();
//                            }
//                        }
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

    public void setListening(boolean b) {
        blistening = b;
        bReceiving = b;
    }

    public void closeAllSocket() {

        for (int i = 0; i < clientList.size(); i++) {
            try {
                client connect = clientList.get(i);
                Socket ct = connect.clientSocket;

                System.out.printf("close: %d Local: %s Remote %s %04d\n", i, ct.getLocalAddress().toString(),
                        ct.getInetAddress().toString(), ct.getLocalPort());
                if (ct.isConnected()) {
                    try {
                        if (connect.output != null) {
                            connect.output.close();
                        }
                        if (connect.input != null) {
                            connect.input.close();
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    //System.out.println("dddddddddddddddddd");
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
        while (blistening) {
            try {
                clientSocket = server.accept();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    input = new BufferedReader(new InputStreamReader(
                            clientSocket.getInputStream()));
                    output = new BufferedWriter(new OutputStreamWriter(
                            clientSocket.getOutputStream()));
                    output.flush();
                    InetAddress ad = clientSocket.getInetAddress();
                    String str = String.format("%d:L %s  R %s %04d\n", i++, clientSocket.getLocalAddress().toString(),
                            clientSocket.getInetAddress().toString(), clientSocket.getLocalPort());
                    System.out.printf(str);
                    log(str);
                    c = new client(clientSocket, input, output);
                    clientList.add(c);
                    c.start();
                }
            } catch (IOException e) {
                //System.out.println("from");
                System.out.println(e);
            }
        }
    }

}

public class MDServer extends Application {

    public SServer SensorServer;
    public SServer MonitorServer;
    private ObservableList<String> logMonitorData;
    private ObservableList<String> logSensorData;

    @Override
    public void start(Stage primaryStage) {

        //GUI initiation
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        VBox vMon = new VBox();
        vMon.setSpacing(4);
        Label monPort = new Label("Monitor Port:");
        TextField monPortTextField = new TextField();
        monPortTextField.setText("2015");
        vMon.getChildren().addAll(monPort, monPortTextField);
        VBox vSen = new VBox();
        vSen.setSpacing(4);
        Label senPort = new Label("Sensor Port:");
        TextField senPortTextField = new TextField();
        senPortTextField.setText("2016");
        vSen.getChildren().addAll(senPort, senPortTextField);

        grid.add(vMon, 0, 0);
        grid.add(vSen, 1, 0);
        ListView MonList = new ListView();
        MonList.setMinSize(50, 100);
        ListView SenList = new ListView();
        SenList.setMinSize(50, 100);
        grid.setPrefSize(380, 480);
        grid.add(MonList, 0, 1);
        grid.add(SenList, 1, 1);
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.CENTER_RIGHT);
        Button btnstart = new Button("  Start Listening  ");
        Button btnclose = new Button("  Close  ");
        hbBtn.getChildren().addAll(btnstart, btnclose);
        grid.add(hbBtn, 0, 2, 2, 1);

        logMonitorData = FXCollections.observableArrayList();
        MonList.setItems(logMonitorData);
        MonList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        logSensorData = FXCollections.observableArrayList();
        SenList.setItems(logSensorData);
        SenList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        btnstart.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent e) {
                int portNumber = Integer.parseInt(senPortTextField.getText());
                SensorServer = new SServer(portNumber);
                SensorServer.setListening(true);
                SensorServer.setLog(logSensorData);
                SensorServer.start();

                int MonportNumber = Integer.parseInt(monPortTextField.getText());
                MonitorServer = new SServer(MonportNumber);
                MonitorServer.setListening(true);
                MonitorServer.setLog(logMonitorData);
                MonitorServer.start();
                SensorServer.setExServer(MonitorServer);
            }
        });

        btnclose.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent e) {
                try {
                    SensorServer.setListening(false);
                    SensorServer.server.close();
                    MonitorServer.setListening(false);
                    MonitorServer.server.close();
                } catch (IOException ioe) {
                    System.out.println(ioe);
                }
                SensorServer.closeAllSocket();
                MonitorServer.closeAllSocket();
            }
        });

        Scene scene = new Scene(grid, 500, 500);
        primaryStage.setTitle("MDServer");
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
