package dictsystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import org.json.simple.JSONObject;

public class DictionaryClient {
    // GUI Components

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

        // start program GUI
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

    private void addActionListeners() {
        searchBtn.addActionListener(e -> {
            // Perform search operation
        });

        // add a new word
        addBtn.addActionListener(e -> {
            JDialog addDialog = new JDialog(frame, "Add Word", true);
            addDialog.setSize(400, 300);
            addDialog.setLocationRelativeTo(frame);

            JTextField wordField = new JTextField();
            wordField.setPreferredSize(new Dimension(200, 30));

            JTextArea meaningField = new JTextArea();
            meaningField.setLineWrap(true);
            meaningField.setWrapStyleWord(true);
            JScrollPane meaningScrollPane = new JScrollPane(meaningField);
            meaningScrollPane.setPreferredSize(new Dimension(250, 60));

            // add new word
            JButton executeBtn = new JButton("Add");
            executeBtn.addActionListener(ev -> {
                // Perform add operation
                // Get the word and its meaning from the text fields
                String newWord = wordField.getText();
                String newMeaning = meaningField.getText();

                if (newWord.isEmpty() || newMeaning.isEmpty()) {
                    JOptionPane.showMessageDialog(addDialog, "Please enter the word and corresponding meaning!", "Warning", JOptionPane.WARNING_MESSAGE);
                } else {
                    // TODO: Add the word and its meaning to your dictionary
                    this.sendToServer("add", newWord, newMeaning);
                    addDialog.dispose();
                }


            });

            // close dialog
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(ev -> {
                addDialog.dispose();
            });

            addDialog.setLayout(new GridLayout(3, 2));
            addDialog.add(new JLabel("Word:"));
            addDialog.add(wordField);
            addDialog.add(new JLabel("Meaning:"));
            addDialog.add(meaningScrollPane);
            addDialog.add(executeBtn);
            addDialog.add(cancelBtn);
            addDialog.setVisible(true);
        });

        removeBtn.addActionListener(e -> {
            JDialog addDialog = new JDialog(frame, "Remove Word", true);
            addDialog.setSize(400, 300);
            addDialog.setLocationRelativeTo(frame);

            // TODO: get word and meaning
            JTextField wordField = new JTextField();
            wordField.setPreferredSize(new Dimension(200, 30));
            wordField.setEditable(false);

            JTextArea meaningField = new JTextArea();
            meaningField.setLineWrap(true);
            meaningField.setWrapStyleWord(true);
            meaningField.setEditable(false);
            JScrollPane meaningScrollPane = new JScrollPane(meaningField);
            meaningScrollPane.setPreferredSize(new Dimension(250, 60));

            //add new word
            JButton executeBtn = new JButton("Remove");
            executeBtn.addActionListener(ev -> {
                // Perform delete operation

                addDialog.dispose();
            });

            //close dialog
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(ev -> {
                addDialog.dispose();
            });

            addDialog.setLayout(new GridLayout(3, 2));
            addDialog.add(new JLabel("Word:"));
            addDialog.add(wordField);
            addDialog.add(new JLabel("Meaning:"));
            addDialog.add(meaningScrollPane);
            addDialog.add(executeBtn);
            addDialog.add(cancelBtn);
            addDialog.setVisible(true);
        });

        updateBtn.addActionListener(e -> {
            JDialog addDialog = new JDialog(frame, "Remove Word", true);
            addDialog.setSize(400, 300);
            addDialog.setLocationRelativeTo(frame);

            // TODO: get word
            JTextField wordField = new JTextField();
            wordField.setPreferredSize(new Dimension(200, 30));
            wordField.setEditable(false);

            JTextArea meaningField = new JTextArea();
            meaningField.setLineWrap(true);
            meaningField.setWrapStyleWord(true);
            JScrollPane meaningScrollPane = new JScrollPane(meaningField);
            meaningScrollPane.setPreferredSize(new Dimension(250, 60));

            //add new word
            JButton executeBtn = new JButton("Update");
            executeBtn.addActionListener(ev -> {
                // Perform delete operation
                String meaning = meaningField.getText();

                if (meaning.isEmpty()) {
                    JOptionPane.showMessageDialog(addDialog, "Please enter new meaning!", "Warning", JOptionPane.WARNING_MESSAGE);
                } else {

                    addDialog.dispose();
                }
            });

            //close dialog
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(ev -> {
                addDialog.dispose();
            });

            addDialog.setLayout(new GridLayout(3, 2));
            addDialog.add(new JLabel("Word:"));
            addDialog.add(wordField);
            addDialog.add(new JLabel("Meaning:"));
            addDialog.add(meaningScrollPane);
            addDialog.add(executeBtn);
            addDialog.add(cancelBtn);
            addDialog.setVisible(true);
        });
    }

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
}

// TODO: 在client尝试与server交互式，发现server不在了。进行提示并关闭程序