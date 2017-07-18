
/* ==================================================
    This program scans the first 1024 port on a 
    specified host and checks which port has a TCP 
    server hosting that particular port.
   ================================================== */

import java.net.Socket;
import java.io.IOException;
import java.net.UnknownHostException;

public class PortScanner {
    public static void main(String[] args) {
        final String HOST = args.length > 0 ? args[0] : "localhost";

        for (int i = 1; i <= 30000; i++) {
            try (Socket socket = new Socket(HOST,i)) {
                System.out.println("There is a server on port " + i + " of " + HOST);
            } catch (UnknownHostException ex) {
                System.err.println("Could not connect to " + HOST);
                System.err.println(ex);
                break;
            } catch (IOException ex) {
                // a TCP server is hosting this port
            }
        }
        
    }
}