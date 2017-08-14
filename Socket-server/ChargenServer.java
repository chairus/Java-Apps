import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.io.IOException;

public class ChargenServer {
    public static final int DEFAULT_PORT = 3000;

    public static void main(String[] args) {

        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = DEFAULT_PORT;
        }
        System.out.println("Listening for connections on port " + port);

        byte[] rotation = new byte[95*2];
        for (byte i = ' '; i <= '~'; i++) {
            rotation[i - ' '] = i;
            rotation[i + 95 - ' '] = i;
        }

        // Setup server to listen to a port and be non-blocking and also setup selector
        ServerSocketChannel serverChannel;
        Selector selector;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    if (key.isAcceptable()) {   // serverChannel is ready(i.e. new client connection)
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        System.out.println("Accepted connection from " + client);
                        client.configureBlocking(false);
                        SelectionKey key2 = client.register(selector, SelectionKey.OP_WRITE);
                        ByteBuffer buffer = ByteBuffer.allocate(74);
                        buffer.put(rotation, 0, 72);
                        buffer.put((byte) '\r');
                        buffer.put((byte) '\n');
                        buffer.flip();
                        key2.attach(buffer);
                    } else if (key.isWritable()) {  // socketChannel is ready(i.e. connected client ready to receive data)
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        if (!buffer.hasRemaining()) {
                            // Refill the buffer with the next line
                            buffer.rewind();
                            // Get the old first character
                            int first = buffer.get();
                            // Get ready to change the data in the buffer
                            buffer.rewind();
                            // Find the new first characters position in rotation
                            int position = first - ' ' + 1;
                            // copy the data from rotation into the buffer
                            buffer.put(rotation, position, 72);
                            // Store a line break at the end of the buffer
                            buffer.put((byte) '\r');
                            buffer.put((byte) '\n');
                            // Prepare the buffer for writing
                            buffer.flip();
                        }
                        client.write(buffer);
                    }
                } catch (IOException e) {   // When client has disconnected, close the connection
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        }
    }
}