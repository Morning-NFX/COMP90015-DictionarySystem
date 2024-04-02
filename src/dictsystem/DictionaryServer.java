package dictsystem;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DictionaryServer {

    private static int port;
    private static String dicFilePath;
    private ServerSocket serverSocket;
    private Connection dbConnection;

    public static void main(String[] args) {
        // check parameters
        if (args.length != 2) {
            System.out.println("Please provide the parameters for staring the program!\nFormat: java -jar DictionaryServer.jar <port> <dictionary-file>");
            System.exit(1);
        }
        try {
            port = Integer.parseInt(args[0]);
        }catch (NumberFormatException e) {
            System.out.println("Port number should be numeric number!");
            System.exit(1);
        }
        dicFilePath = args[1];

        // start program
        try {
            new DictionaryServer().init();
        } catch (IOException e) {
            System.out.println("Unexpected error occurred while starting server!");
            System.exit(1);
        }
    }

    private void init() throws IOException {
        // connect with dicDB
        dbConnection();

        // set up server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Port " + port + " is not available!");  //port not available
            System.exit(1);
        }

        // TODO: GUI here?

        while (true) {
            Socket socket = serverSocket.accept();

            // new connection
            System.out.println("New connection from: " + socket.getInetAddress() + ":" + socket.getPort());

            // handle each client connection in a separate thread
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while (true) {
                        //read request from client
                        JSONParser parser = new JSONParser();
                        JSONObject requestObj = (JSONObject) parser.parse(in.readLine());
                        System.out.println(requestObj.toJSONString());

//                        JSONObject responseObj = clientRequestProcess(requestObj);
                        JSONObject responseObj = new JSONObject();
                        responseObj.put("word", "a");
                        responseObj.put("meaning", "b");

                        // send responseObj back to client
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(responseObj.toJSONString());
                    }
                } catch (Exception e) {
                    System.out.println("Client at " + socket.getInetAddress() + ":" + socket.getPort() + " has disconnected.");
                } finally {
                    // close socket
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Error while closing the socket.");
                    }
                }
            }).start();
        }

    }

    private void dbConnection(){
        try {
            String url = "jdbc:sqlite:" + dicFilePath;
            dbConnection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println("Error while connecting to dictionary file!");
            System.exit(1);
        }
    }

    private void clientRequestProcess(JSONObject requestObj, Socket socket){
        String mode = (String) requestObj.get("mode");
        String word = (String) requestObj.get("word");
        String meaning = (String) requestObj.get("meaning");

        // search for word
        if (mode.equals("search")) {

        }   // add new word
        else if (mode.equals("add")) {

        }   //  remove an existing word
        else if (mode.equals("remove")) {

        }   // update an existing word
        else if (mode.equals("update")) {

        }
    }
}
