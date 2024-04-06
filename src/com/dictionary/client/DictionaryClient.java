// Xinyu Wang 1460767
package com.dictionary.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class DictionaryClient {
    JFrame frame = new JFrame("Client");

    Panel searchPanel = new Panel();
    JTextField searchKeyWord = new JTextField();
    JButton searchBtn = new JButton("Search");

    JTextArea searchResult = new JTextArea();

    Panel actionPanel = new Panel();
    JButton addBtn = new JButton("Add");
    JButton removeBtn = new JButton("Remove");
    JButton updateBtn = new JButton("Update");

    // Socket variables
    private static String serverAddress;
    private static int serverPort;
    private static Socket socket;

    public static void main(String[] args) {
        // check and parse parameters
        if (args.length != 2) {
            System.out.println("Please provide the parameters for staring the program!\nFormat: java -jar DictionaryClient.jar <server-address> <server-port>");
            System.exit(1);
        }
        serverAddress = args[0];
        try {
            serverPort = Integer.parseInt(args[1]);
        }catch (NumberFormatException e) {
            System.out.println("Port number should be numeric number!");
            System.exit(1);
        }

        // start program
        new DictionaryClient().init();
    }

    private void init() {
        frame.setSize(610, 550);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        setComponentsStyle();
        addComponentsToContainer();

        try {
            socket = new Socket(serverAddress, serverPort);
            frame.setTitle("Client [" + socket.getLocalSocketAddress() + "]");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error when trying to connect with server!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        addActionListeners();

        // close btn to terminate socket and program
        addWindowCloseListener();
    }

    private void setComponentsStyle() {
        searchKeyWord.setPreferredSize(new Dimension(300, 30));

        searchResult.setEditable(false);

        Dimension actionButtonSize = new Dimension(100, 30);
        addBtn.setPreferredSize(actionButtonSize);
        removeBtn.setPreferredSize(actionButtonSize);
        updateBtn.setPreferredSize(actionButtonSize);
    }

    private void addComponentsToContainer() {
        // search bar area
        searchPanel.add(searchKeyWord);
        searchPanel.add(searchBtn);
        frame.add(searchPanel, BorderLayout.NORTH);

        // returned result area
        frame.add(searchResult, BorderLayout.CENTER);

        // action btn area
        actionPanel.add(addBtn);
        actionPanel.add(removeBtn);
        actionPanel.add(updateBtn);
        frame.add(actionPanel, BorderLayout.SOUTH);
    }

    /**
     * When user click the close button
     */
    private void addWindowCloseListener() {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    /**
     * When user click on buttons
     */
    private void addActionListeners() {
        // search meaning
        searchBtn.addActionListener(e -> {
            String word = searchKeyWord.getText();
            sendToServer("search", word, "");
            JSONObject responseObj = receiveFromServer();
            if (responseObj.get("status").equals("success")) {
                searchResult.setText((String) responseObj.get("meaning"));
            } else if (responseObj.get("status").equals("fail")) {
                searchResult.setText("");
                JOptionPane.showMessageDialog(frame, responseObj.get("message"), "Fail", JOptionPane.WARNING_MESSAGE);
            } else {
                searchResult.setText("");
                JOptionPane.showMessageDialog(frame, responseObj.get("message"), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // add a new word
        addBtn.addActionListener(e -> {
            JDialog addDialog = new JDialog(frame, "Add Word", true);
            addDialog.setSize(400, 300);
            addDialog.setLocationRelativeTo(frame);

            JTextField wordField = new JTextField();
            wordField.setPreferredSize(new Dimension(200, 20));

            JTextField meaningField = new JTextField();
            meaningField.setPreferredSize(new Dimension(200, 20));

            // execute Btn -> add new word
            JButton executeBtn = new JButton("Add");
            executeBtn.addActionListener(ev -> {
                String newWord = wordField.getText();
                String newMeaning = meaningField.getText();
                sendToServer("add", newWord, newMeaning);
                JSONObject responseObj = receiveFromServer();
                if (responseObj.get("status").equals("success")) {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Success", JOptionPane.INFORMATION_MESSAGE);
                } else if (responseObj.get("status").equals("fail")) {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Fail", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Error", JOptionPane.ERROR_MESSAGE);
                }
                addDialog.dispose();
            });

            // Cancel Btn -> close dialog
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(ev -> {
                addDialog.dispose();
            });
            addDialog.setLayout(new GridLayout(3, 2));
            addDialog.add(new JLabel("Word:"));
            addDialog.add(wordField);
            addDialog.add(new JLabel("Meaning:"));
            addDialog.add(meaningField);
            addDialog.add(executeBtn);
            addDialog.add(cancelBtn);
            addDialog.setVisible(true);
        });

        // remove an existing word
        removeBtn.addActionListener(e -> {
            JDialog addDialog = new JDialog(frame, "Remove Word", true);
            addDialog.setSize(400, 300);
            addDialog.setLocationRelativeTo(frame);

            JTextField wordField = new JTextField();
            wordField.setPreferredSize(new Dimension(200, 30));

            // execute Btn -> remove word
            JButton executeBtn = new JButton("Remove");
            executeBtn.addActionListener(ev -> {
                String word = wordField.getText();
                sendToServer("remove", word, null);
                JSONObject responseObj = receiveFromServer();
                if (responseObj.get("status").equals("success")) {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Success", JOptionPane.INFORMATION_MESSAGE);
                } else if (responseObj.get("status").equals("fail")) {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Fail", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Error", JOptionPane.ERROR_MESSAGE);
                }
                addDialog.dispose();
            });

            // cancel Btn -> close dialog
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(ev -> {
                addDialog.dispose();
            });

            addDialog.setLayout(new GridLayout(2, 2));
            addDialog.add(new JLabel("Word:"));
            addDialog.add(wordField);
            addDialog.add(executeBtn);
            addDialog.add(cancelBtn);
            addDialog.setVisible(true);
        });

        // update an existing word
        updateBtn.addActionListener(e -> {
            JDialog addDialog = new JDialog(frame, "Remove Word", true);
            addDialog.setSize(400, 300);
            addDialog.setLocationRelativeTo(frame);

            JTextField wordField = new JTextField();
            wordField.setPreferredSize(new Dimension(200, 30));

            JTextField meaningField = new JTextField();
            meaningField.setPreferredSize(new Dimension(200, 30));

            // execute Btn -> update word
            JButton executeBtn = new JButton("Update");
            executeBtn.addActionListener(ev -> {
                String word = wordField.getText();
                String meaning = meaningField.getText();
                sendToServer("update", word, meaning);
                JSONObject responseObj = receiveFromServer();
                if (responseObj.get("status").equals("success")) {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Success", JOptionPane.INFORMATION_MESSAGE);
                } else if (responseObj.get("status").equals("fail")) {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Fail", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(addDialog, responseObj.get("message"), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            // cancel Btn -> close dialog
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(ev -> {
                addDialog.dispose();
            });

            addDialog.setLayout(new GridLayout(3, 2));
            addDialog.add(new JLabel("Word:"));
            addDialog.add(wordField);
            addDialog.add(new JLabel("New Meaning:"));
            addDialog.add(meaningField);
            addDialog.add(executeBtn);
            addDialog.add(cancelBtn);
            addDialog.setVisible(true);
        });
    }

    /**
     * Send the request to Server
     * @param mode operation type [search/add/remove/update]
     * @param word word
     * @param meaning meaning of the word
     */
    private void sendToServer(String mode, String word, String meaning){
        // create json object and content
        JSONObject requestObj = new JSONObject();
        requestObj.put("mode", mode);
        requestObj.put("word", word);
        requestObj.put("meaning", meaning);
        // send json request to server
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(requestObj.toJSONString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error when trying to connect with server!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Receive response from server
     * @return response JSONObject
     */
    private JSONObject receiveFromServer(){
        try {
            // Create BufferedReader to read the input stream from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // read response from server
            JSONParser parser = new JSONParser();
            JSONObject responseObj = (JSONObject) parser.parse(in.readLine());
            return responseObj;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error when trying to connect with server!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (org.json.simple.parser.ParseException e) {
            JOptionPane.showMessageDialog(frame, "Error when trying to parse server response!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return null;
    }
}