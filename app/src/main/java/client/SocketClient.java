package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import socket.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson;
    private String serverIp;
    private int serverPort;

    // Private constructor for Singleton pattern
    private SocketClient() {
        gson = new GsonBuilder().serializeNulls().create();
    }

    // Getter for the class instance (Singleton)
    public synchronized static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    // Set the server's IP address and port number
    public void setServer(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    // Open a socket connection to the server
    public boolean connectToServer() {
        try {
            socket = new Socket(serverIp, serverPort);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Close socket connection and all IO streams
    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send a serialized request to the server and return a response of a specific class
    public <T> T sendRequest(Request request, Class<T> responseClass) {
        try {
            if (socket == null) {
                // If the socket is not initialized, try to initialize it.
                if (!connectToServer()) {
                    // Handle connection failure
                    return null;
                }
            }

            if (outputStream == null) {
                // If the outputStream is not initialized, try to initialize it.
                outputStream = new DataOutputStream(socket.getOutputStream());
            }

            String jsonRequest = gson.toJson(request);
            outputStream.writeUTF(jsonRequest);
            outputStream.flush();

            String jsonResponse = inputStream.readUTF();
            return gson.fromJson(jsonResponse, responseClass);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
