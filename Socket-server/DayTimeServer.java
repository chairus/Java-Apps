import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/*  ================================================
    This program creates a server that accepts
    connection from clients and sends to the client
    the current daytime.
    ================================================  */

    public class DayTimeServer {
        public final static int PORT = 3000; 

        public static void main(String[] args) {
            try(ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    try(Socket connection = serverSocket.accept()) {
                        Writer out = new OutputStreamWriter(connection.getOutputStream());
                        Date now = new Date();
                        out.write(now.toString() + "\r\n");
                        out.flush();
                    } catch (IOException ex) {
                        //ignore client
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }