package dictsystem;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.*;
import java.sql.*;

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

            String socketInfo = socket.getInetAddress() + ":" + socket.getPort();

            // new connection
            System.out.println("New connection from: " + socketInfo);

            // handle each client connection in a separate thread
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while (true) {
                        //read request from client
                        JSONParser parser = new JSONParser();
                        JSONObject requestObj = (JSONObject) parser.parse(in.readLine());

                        JSONObject responseObj =handleClientRequest(requestObj, socketInfo);

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

    /**
     * Connect to the dictionary database
     */
    private void dbConnection(){
        try {
            String url = "jdbc:sqlite:" + dicFilePath;
            dbConnection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println("Error while connecting to dictionary file!");
            System.exit(1);
        }
    }

    /**
     * process client request unit
     * @param requestObj request JSON sent from client
     * @param socketInfo client socket information (String)
     * @return  response JSON object
     */
    private JSONObject handleClientRequest(JSONObject requestObj, String socketInfo){
        String mode = (String) requestObj.get("mode");
        String word = (String) requestObj.get("word");
        String meaning = (String) requestObj.get("meaning");

        // search for word
        if (mode.equals("search")) {
            System.out.println(socketInfo + " is trying to search word: " + word);
            return searchWord(word);
        }   // add new word
        else if (mode.equals("add")) {;
            System.out.println(socketInfo + " is trying to add word: " + word + " with meaning: " + meaning);
            return addWord(word, meaning);
        }   //  remove an existing word
        else if (mode.equals("remove")) {

        }   // update an existing word
        else if (mode.equals("update")) {

        }
        // invalid operation
        JSONObject responseObj = new JSONObject();
        responseObj.put("status", "failed");
        responseObj.put("message", "Invalid operation!");
        return responseObj;
    }

    /**
     * Search the corresponding meaning based on give word
     * @param word  word to search
     * @return response JSON object
     */
    private JSONObject searchWord(String word) {
        JSONObject responseObj = new JSONObject();
        // check word not empty
        if (word.isEmpty()) {
            responseObj.put("status", "error");
            responseObj.put("message", "Please enter the word!");
            return responseObj;
        }
        // search word in db
        try {
            String sql = "SELECT meaning FROM dictionary WHERE word = ?";
            PreparedStatement pstmt = dbConnection.prepareStatement(sql);
            pstmt.setString(1, word);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // successfully find the word
                String meaning = rs.getString("meaning");
                responseObj.put("status", "success");
                responseObj.put("meaning", meaning);
            } else {
                // word not found
                responseObj.put("status", "failed");
                responseObj.put("message", "Word not found in dictionary!");
            }
            pstmt.close();
            return responseObj;
        } catch (SQLException e) {
            // database error
            responseObj.put("status", "error");
            responseObj.put("message", "Database error!");
            return responseObj;
        }
    }

    /**
     * Add a new word to the dictionary (lock)
     * @param word new word
     * @param meaning meaning of the word
     * @return response JSON object
     */
    private synchronized JSONObject addWord(String word, String meaning) {
        JSONObject responseObj = new JSONObject();
        try {
            String sql = "INSERT INTO dictionary (word, meaning) VALUES (?, ?)";
            PreparedStatement pstmt =dbConnection.prepareStatement(sql);
            pstmt.setString(1, word);
            pstmt.setString(2, meaning);
            pstmt.executeUpdate();
            pstmt.close();
            // successfully add the word
            responseObj.put("status", "success");
            return responseObj;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                // word already exists
                responseObj.put("status", "failed");
                responseObj.put("message", "Word already exists in dictionary!");
                return responseObj;
            } else {
                // database error
                e.printStackTrace();
                responseObj.put("status", "error");
                responseObj.put("message", "Database error!");
                return responseObj;
            }
        }
    }
}
