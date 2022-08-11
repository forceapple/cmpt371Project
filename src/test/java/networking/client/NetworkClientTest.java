package networking.client;

import javafx.scene.paint.Color;
import kotlin.jvm.Volatile;
import networking.server.NetworkServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.*;

class NetworkClientTest {

    private static final int PORT = 7070;

    @BeforeAll
    public static void initServer() {
        NetworkServer server = new NetworkServer(PORT);
        server.setName("Server");
        server.setDaemon(true);
        server.start();
    }

    NetworkClient getNetworkClient() throws IOException {
        return new NetworkClient("localhost", Integer.toString(PORT));
    }

    @Test
    void testClient() throws IOException {
        assertThrows(IOException.class, () -> new NetworkClient("localhost", "0"));
        assertThrows(IllegalArgumentException.class, () -> new NetworkClient("localhost", "1000000"));
        assertThrows(IOException.class, () -> new NetworkClient("a", Integer.toString(PORT)));
        getNetworkClient();
    }

    @Test
    void registerColor() throws IOException, InterruptedException {
        int REPEAT_COUNT = 1000;
        NetworkClient client1 = getNetworkClient();
        NetworkClient client2 = getNetworkClient();

        Semaphore lock = new Semaphore(0);

        Runnable client1Blue = new Runnable() {
            @Override
            public void run() {
                client1.registerColor(Color.BLUE);
                lock.release();
            }
        };
        Runnable client1Red = new Runnable() {
            @Override
            public void run() {
                client1.registerColor(Color.RED);
                lock.release();
            }
        };
        Runnable client2Blue = new Runnable() {
            @Override
            public void run() {
                client2.registerColor(Color.BLUE);
                lock.release();
            }
        };
        Runnable client2Red = new Runnable() {
            @Override
            public void run() {
                client2.registerColor(Color.RED);
                lock.release();
            }
        };

        for(int i = 0; i < REPEAT_COUNT; i++) {

            Thread thread1;
            Thread thread2;

            if(i%2 == 0) {
                thread1 = new Thread(client1Blue);
                thread2 = new Thread(client2Blue);
            }
            else {
                thread1 = new Thread(client1Red);
                thread2 = new Thread(client2Red);
            }

            thread1.start();
            thread2.start();


            lock.acquire(2);


            assertNotSame(client1.clientColor, client2.clientColor);

            Thread.sleep(5);
        }
    }

    @Test
    void canvasSelection() throws IOException, InterruptedException {
        int REPEAT_COUNT = 10000;

        NetworkClient client1 = getNetworkClient();
        NetworkClient client2 = getNetworkClient();

        client1.registerColor(Color.BLACK);
        client2.registerColor(Color.WHITE);
        client1.startClient();
        client2.startClient();

        final boolean[] client1Drawing = {false};
        final boolean[] client2Drawing = {false};

        Semaphore lock = new Semaphore(0);

        Runnable client1GetCanvas = new Runnable() {
            @Override
            public void run() {
                client1Drawing[0] = client1.selectCanvasForDrawing(1);
                lock.release();
            }
        };

        Runnable client2GetCanvas = new Runnable() {
            @Override
            public void run() {
                client2Drawing[0] = client2.selectCanvasForDrawing(1);
                lock.release();
            }
        };

        for(int i = 0; i < REPEAT_COUNT; i++) {

            Thread thread1 = new Thread(client1GetCanvas);
            Thread thread2 = new Thread(client2GetCanvas);

            assertEquals(-1, client1.currentCanvasID);
            assertEquals(-1, client2.currentCanvasID);

            thread1.start();
            thread2.start();

            lock.acquire(2);

            assertNotSame(client1Drawing[0], client2Drawing[0]); // check that one of them actually got to draw
            assertFalse(client1.currentCanvasID == client2.currentCanvasID && client1.currentCanvasID != -1);

            client1.releaseCanvas();
            client2.releaseCanvas();
        }

    }
}





































