package Server;

import static com.ielena.myapplication.ChatActivity.chatActivity;
import static com.ielena.myapplication.ChatActivity.messageTo;
import static com.ielena.myapplication.Mediator.databaseManager;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.ielena.myapplication.Chat;
import com.ielena.myapplication.Mediator;
import com.ielena.myapplication.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class User extends Thread {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private boolean running = true;
    public boolean close = false;
    private Thread receiverThread;
    private Thread writerThread;
    private Handler mainHandler;
    private Context context;
    public static Chat chatContext;

    final String SERVER_IP = "34.175.206.223"; // Dirección IP del servidor
    final int SERVER_PORT = 12345; // Puerto en el que el servidor está escuchando

    public User(int clientId, Context context) {
        this.context = context;
        mainHandler = new Handler(Looper.getMainLooper());
        Future<ArrayList<Integer>> groupsFuture;

        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            groupsFuture = databaseManager.checkUserGroupsAsync(clientId);
            ArrayList<Integer> groups = groupsFuture.get();

            // Enviar el identificador del cliente al servidor
            writer.println(clientId + "" + groups);

            // Si se ha creado correctamente, se deja userError en false
            Mediator.userError = false;

            // Iniciar hilo para recibir mensajes
            startReceiverThread();

        } catch (IOException e) {
            // Se reinicia el login si no se puede conectar al servidor
            showError("Error de Conexión", "Error al acceder a tu cuenta, inténtelo de nuevo");
            Mediator.userError = true;

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            while (running) {
                try {
                    if (reader.ready()) {
                        String message = reader.readLine();
                        if (message != null) {
                            messageQueue.put(message); // Añadir mensaje a la cola
                        } else {
                            running = false; // Terminar bucle si la conexión se cierra
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (IOException | InterruptedException e) {
                    running = false;
                }
            }
        });
        receiverThread.start();
    }

    private void notifyChatActivity() {
        if (chatContext!= null) chatContext.updateRecyclerView();
    }

    public boolean waitForUpdate() {
        if (running) {
            try {
                Thread.sleep(10000);
                notifyChatActivity(); // Notificar la actualización del RecyclerView
                return true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    // MENSAJE A CONTACTOS
    public void sendMessage(int targetClientId, ArrayList<Integer> groups, String message) {
        writerThread = new Thread(() -> {
            if (writer != null) {
                writer.println(targetClientId + "" + groups + ":" + message);
            } else {
                System.out.println("No se ha establecido la conexión con el servidor.");
            }
        });
        writerThread.start();
    }

    // MENSAJES A GRUPOS
    public void sendMessageGroup(int targetClientId, String message) {
        writerThread = new Thread(() -> {
            if (writer != null) {
                writer.println(targetClientId + ":" + message);
            } else {
                System.out.println("No se ha establecido la conexión con el servidor.");
            }
        });
        writerThread.start();
    }

    public boolean receiveMessage() {
        String message = "";
        // Si close está en true, quiere decir que el usuario ha cerrado la aplicación, su sesión ha terminado
        if (close) {
            return false;
        }

        // Esperar a recibir el mensaje
        try {
            message = messageQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mainHandler.post(() -> {
            if (messageTo.equals("isContact")) {
                Mediator.loadChat(chatActivity);
            } else if (messageTo.equals("isGroup")) {
                Mediator.loadChatGroup(chatActivity);
            }
        });

        // Si el mensaje no está vacío se reproduce el sonido
        if (!message.equals("")) {
            // Reproducir sonido
            playNotificationSound();
        }
        notifyChatActivity(); // Notificar la actualización del RecyclerView

        return true;
    }

    private void playNotificationSound() {
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.message1);
        MediaPlayer mediaPlayer = MediaPlayer.create(context, soundUri);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        }
    }

    public void disconnectFromServer() {
        running = false;
        receiverThread.interrupt(); // Interrumpir el hilo
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        mainHandler.post(() -> Toast.makeText(context, title + ": " + message, Toast.LENGTH_LONG).show());
    }
}