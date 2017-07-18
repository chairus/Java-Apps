import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

public class DictClient {
    public static void main(String[] args) {
        final String HOSTNAME = "dict.org";
        final int PORT = 2628;
        final int TIMEOUT = 15000;
        System.out.println("Connecting to \"" + HOSTNAME + "\"..." );

        try (Socket socket = new Socket(HOSTNAME, PORT)) {
            socket.setSoTimeout(TIMEOUT);
            System.out.println("CONNECTED");
            // read and write to socket
            OutputStream out = socket.getOutputStream();
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            writer = new BufferedWriter(writer);
            
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            
            for (String word: args) {
                define(word, writer, reader);
            }

            writer.write("quit\r\n");
            writer.flush();

        } catch (IOException ex) {
            System.err.println("Could not connect to \"dict.org\"");
            System.err.println(ex);
        }

    }

    static void define(String word, Writer writer, BufferedReader reader)
    throws IOException, UnsupportedEncodingException {
        writer.write("DEFINE fd-eng-lat " + word + "\r\n");
        writer.flush();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.startsWith("250 ")) { // OK
                return;
            } else if (line.startsWith("552 ")) { // no match
                System.out.println("No definition found for " + word);
                return;
            } else if (line.startsWith("550 ")) {
                System.out.println("Invalid database");
                return;
            }
            else if (line.matches("\\d\\d\\d .*")) continue;
            else if (line.trim().equals(".")) continue;
            else System.out.println(line);
        }
    }
}