package dictsystem.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;

import static java.lang.Thread.sleep;

public class DictionaryServer {

    JFrame frame = new JFrame("Server");
    JTextArea messageCenter = new JTextArea();
    JScrollPane messageCenterScroll = new JScrollPane(messageCenter);
    JPanel dictionaryPanel = new JPanel(new BorderLayout());
    JButton refreshBtn = new JButton("Refresh Dictionary");
    JTextArea wordList = new JTextArea();

    private static int port;
    private static String dicFilePath;
    private ServerSocket serverSocket;
    private Connection dbConnection;
    private HashMap<String, String> dictionary = new HashMap<>();
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("(yyyyMMdd-HH:mm:ss)");

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
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        setComponentsStyle();
        addComponentsToContainer();

        // connect with dicDB
        dbConnection();
        // set up server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Port " + port + " is not available!");  //port not available
            System.exit(1);
        }

        // get words and meanings from db
        refreshBtn.addActionListener(e -> {
            reloadDicFromDB();
            wordList.setText("");
            dictionary.forEach((word, meaning) -> {
                String modifiedMeaning = meaning.replace("\n", " | ");
                wordList.append(word + "\t" + modifiedMeaning + "\n");
            });
        });

        while (true) {
            Socket socket = serverSocket.accept();

            String socketInfo = socket.getInetAddress() + ":" + socket.getPort();

            // new connection
            updateMessageCenter(now.format(formatter) + " New connection from: " + socketInfo);

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
                    updateMessageCenter(now.format(formatter) + " Client at " + socket.getInetAddress() + ":" + socket.getPort() + " has disconnected.");
                } finally {
                    // close socket
                    try {
                        socket.close();
                    } catch (IOException e) {
                        updateMessageCenter(now.format(formatter) + " Error while closing connection with Client at " + socket.getInetAddress() + ":" + socket.getPort());
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
        // load dictionary from db
        reloadDicFromDB();
    }

    /**
     * Load words and meanings from DB to memory
     */
    private void reloadDicFromDB() {
        dictionary.clear();
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM dictionary");
            while (rs.next()) {
                String word = rs.getString("word");
                String meaning = rs.getString("meaning");
                dictionary.put(word, meaning);
            }
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error while getting words from dictionary file!");
            System.exit(1);
        }
    }

    private void setComponentsStyle() {
        wordList.setPreferredSize(new Dimension(600, 200));
    }

    private void addComponentsToContainer() {
        frame.add(new JLabel("Message Center"), BorderLayout.NORTH);

        frame.add(messageCenterScroll, BorderLayout.CENTER);

        dictionaryPanel.add(refreshBtn, BorderLayout.NORTH);
        dictionaryPanel.add(wordList, BorderLayout.CENTER);
        frame.add(dictionaryPanel, BorderLayout.SOUTH);
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
            updateMessageCenter(now.format(formatter) + " " + socketInfo + " - Search word: " + word);
            return searchWord(word);
        }
        // use lock for data manipulation operations
        synchronized (DictionaryServer.class) {
            // add new word
            if (mode.equals("add")) {;
                updateMessageCenter(now.format(formatter) + " " + socketInfo + " - Add word: " + word + " with meaning: " + meaning);
                return addWord(word, meaning);
            }   //  remove an existing word
            else if (mode.equals("remove")) {
                updateMessageCenter(now.format(formatter) + " " + socketInfo + " - Remove word: " + word);
                return removeWord(word);
            }   // update an existing word
            else if (mode.equals("update")) {
                updateMessageCenter(now.format(formatter) + " " + socketInfo + " - Update word: " + word + " with meaning: " + meaning );
                return updateWord(word, meaning);
            }
        }
        // invalid operation
        JSONObject responseObj = new JSONObject();
        responseObj.put("status", "fail");
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
            responseObj.put("message", "Please provide the word!");
            return responseObj;
        }
        // search word in memory
        if (dictionary.containsKey(word)) {
            responseObj.put("status", "success");
            responseObj.put("meaning", dictionary.get(word));
            return responseObj;
        } else {
            responseObj.put("status", "fail");
            responseObj.put("message", "Word " + word + " not found in dictionary!");
            return responseObj;
        }
    }

    /**
     * Add a new word to the dictionary
     * @param word new word
     * @param meaning meaning of the word
     * @return response JSON object
     */
    private JSONObject addWord(String word, String meaning) {
        JSONObject responseObj = new JSONObject();
        // check word and meaning not empty
        if (word.isEmpty() || meaning.isEmpty()) {
            responseObj.put("status", "error");
            responseObj.put("message", "Please provide the word and meaning!");
            return responseObj;
        }
        // check whether the word already exists
        if (dictionary.containsKey(word)) {
            responseObj.put("status", "fail");
            responseObj.put("message", "Word " + word + " already exists in dictionary!");
            return responseObj;
        }
        // add word to db
        try {
            String sql = "INSERT INTO dictionary (word, meaning) VALUES (?, ?)";
            PreparedStatement pstmt = dbConnection.prepareStatement(sql);
            pstmt.setString(1, word);
            pstmt.setString(2, meaning);
            pstmt.executeUpdate();
            pstmt.close();
            // update dictionary in memory
            dictionary.put(word, meaning);
            responseObj.put("status", "success");
            responseObj.put("message", "Word " + word + " has been added to dictionary!");
            return responseObj;
        } catch (SQLException e) {
            // database error
            responseObj.put("status", "error");
            responseObj.put("message", "Database error!");
            return responseObj;
        }
    }

    /**
     * Remove a word from the dictionary
     * @param word word to remove
     * @return response JSON object
     */
    private JSONObject removeWord(String word) {
        JSONObject responseObj = new JSONObject();
        // check word not empty
        if (word.isEmpty()) {
            responseObj.put("status", "error");
            responseObj.put("message", "Please provide the word!");
            return responseObj;
        }
        // check whether the word exists
        if (!dictionary.containsKey(word)) {
            responseObj.put("status", "fail");
            responseObj.put("message", "Word " + word + " not found in dictionary!");
            return responseObj;
        }
        // remove word from db
        try {
            String sql = "DELETE FROM dictionary WHERE word = ?";
            PreparedStatement pstmt = dbConnection.prepareStatement(sql);
            pstmt.setString(1, word);
            pstmt.executeUpdate();
            pstmt.close();
            // update dictionary in memory
            dictionary.remove(word);
            responseObj.put("status", "success");
            responseObj.put("message", "Word " + word + " has been removed from dictionary!");
            return responseObj;
        } catch (SQLException e) {
            // database error
            responseObj.put("status", "error");
            responseObj.put("message", "Database error!");
            return responseObj;
        }
    }

    /**
     * Update the meaning of a word in the dictionary
     * @param word word to update
     * @param meaning new meaning of the word
     * @return response JSON object
     */
    private JSONObject updateWord(String word, String meaning) {
        JSONObject responseObj = new JSONObject();
        // check word and meaning not empty
        if (word.isEmpty() || meaning.isEmpty()) {
            responseObj.put("status", "error");
            responseObj.put("message", "Please provide the word and meaning!");
            return responseObj;
        }
        // check whether the word exists
        if (!dictionary.containsKey(word)) {
            responseObj.put("status", "fail");
            responseObj.put("message", "Word " + word + " not found in dictionary!");
            return responseObj;
        }
        // check if the meaning is the same
        String[] existingMeanings = dictionary.get(word).split("\n");
        if (Arrays.asList(existingMeanings).contains(meaning)) {
            responseObj.put("status", "fail");
            responseObj.put("message", "Meaning " + meaning + " of word " + word + " is already exists!");
            return responseObj;
        }
        // update word in db
        try {
            String sql = "UPDATE dictionary SET meaning = ? WHERE word = ?";
            PreparedStatement pstmt = dbConnection.prepareStatement(sql);
            pstmt.setString(1, dictionary.get(word) + "\n" + meaning);
            pstmt.setString(2, word);
            pstmt.executeUpdate();
            pstmt.close();
            // update dictionary in memory
            dictionary.put(word, dictionary.get(word) + "\n" + meaning);
            responseObj.put("status", "success");
            responseObj.put("message", "Meaning " + meaning + " has been added to word " + word + " in dictionary!");
            return responseObj;
        } catch (SQLException e) {
            // database error
            responseObj.put("status", "error");
            responseObj.put("message", "Database error!");
            return responseObj;
        }
    }

    private void updateMessageCenter(String message) {
        messageCenter.setText(message + "\n" + messageCenter.getText());
    }
}
