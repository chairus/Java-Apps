import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*  ===========================================================
    This program is a DayTime server that sends back to clients
    the current daytime. It uses threads to handle multiple
    connections to the server.
    ===========================================================  */

    public class MultiThreadedDayTimeServer {
        // port where the server would be listening
        public final static int PORT = 3000;
        public final static int NTHREADS = 50;
        // create a pool of threads
        static ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);
        public static void main(String[] args) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while(true) {
                    try {
                        Socket connection = serverSocket.accept();
                        Callable<Void> task = new DayTimeThread(connection);
                        pool.submit(task);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private static class DayTimeThread implements Callable<Void> {
            private Socket connection;

            public DayTimeThread (Socket connection) {
                this.connection = connection;
            }

            @Override
            public Void call() {
                try {
                    Writer out = new OutputStreamWriter(connection.getOutputStream());
                    Date now = new Date();
                    out.write(now.toString() + "\r\n");
                    out.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        connection.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                return null;
            }
        }
    }