import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.IOException;

public class ChargenClient {
    public final static int DEFAULT_PORT = 19;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java ChargenClient host [port]");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = DEFAULT_PORT;
        }

        try {
            SocketAddress address = new InetSocketAddress(args[0], port);
            SocketChannel client = SocketChannel.open(address);
            client.configureBlocking(false);    // Make the reading of the channel non-blocking

            ByteBuffer buffer = ByteBuffer.allocate(74);

            WritableByteChannel output = Channels.newChannel(System.out);

            while (true) {
                // Put code here whatever you want it to do every loop 

                int n = client.read(buffer);
                if (n > 0) {
                    buffer.flip();
                    output.write(buffer);
                    buffer.clear();
                } else if (n == -1) {
                    break;
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}