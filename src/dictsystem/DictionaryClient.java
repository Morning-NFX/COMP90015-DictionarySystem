package dictsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;

public class DictionaryClient {
    // GUI Components

    JFrame frame = new JFrame("Dictionary System [Client]");

    Panel searchPanel = new Panel();
    JTextField searchKeyWord = new JTextField("Type to start...");
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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   //TODO: modify to close Socket connection
        frame.setSize(610, 550);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        setComponentsStyle();
        addComponentsToContainer();

        try {
            socket = new Socket(serverAddress, serverPort);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error when trying to connect with server!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
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
}
