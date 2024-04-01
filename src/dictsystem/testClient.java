package dictsystem;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class testClient {
    public static void main(String[] args) throws IOException {
        // config and start connection
        ServerSocket s = new ServerSocket(50000);
        while (true) {
            Socket s1 = s.accept();

            // get communication stream
            OutputStream s1out = s1.getOutputStream();
            DataOutputStream dos = new DataOutputStream(s1out);

            // send message
            dos.writeUTF("Hi There");

            // close connection
            dos.close();
            s1out.close();
            s1.close();
        }
    }
}
