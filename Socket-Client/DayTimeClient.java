import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/*  ===================================================================================
    This program uses 'sockets' to retrieve date-time information from the NIST server
    =================================================================================== */

public class DayTimeClient {
    public static void main(String[] args) {
        String hostname = args.length > 0 ? args[0] : "time.nist.gov";

        /*  ==========================================
            For java 7 onwards use try-with-resources 
            ========================================== */
        System.out.println("Connecting to the server...");
        try (Socket socket = new Socket(hostname, 13)) {
            socket.setSoTimeout(15000);
            System.out.println("CONNECTED");
            // read from the socket

            // create InputStream to start reading bytes from the socket
            InputStream in = socket.getInputStream();
            StringBuilder time = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(in, "ASCII");
            for (int c = reader.read(); c != -1; c = reader.read()) {
                time.append((char) c);
            }
            System.out.println(time);
        } catch (IOException ex) {
            System.err.println("Could not connect to time.nsit.gov");
            System.err.println(ex);
        }

        /*  ==========================================
            For java 6 use the try-catch-finally block 
            ========================================== */
        /*
        Socket socket = null;
        try {
            // connect to server and read from socket
            socket = new Socket("time.nist.gov", 13);
            // read from socket
        } catch (IOException ex) {
            System.err.println("Could not connect to time.nist.gov");
            System.err.println(ex);
        } finally {
            // close socket connection
            socket.close();
        }
        */
        
    }
}