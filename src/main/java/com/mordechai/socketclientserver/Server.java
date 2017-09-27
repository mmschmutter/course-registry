package com.mordechai.socketclientserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        // try to open JSON file from command line argument path
        File dataFile;
        if (args.length == 1) {
            dataFile = new File(args[0]);
        } else {
            throw new IllegalArgumentException("Invalid path input");
        }
        // throw error if JSON file does not exist at command line argument path
        if (!dataFile.exists() || !dataFile.getName().endsWith(".json")) {
            throw new IllegalArgumentException("No JSON file exists at that path");
        }
        int port = 5056;
        ServerSocket serverSocket;
        // try to start server at port 5056
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started at port " + serverSocket.getLocalPort());
        } catch (Exception e) {
            throw new IllegalStateException("Server already listening at port " + port);
        }
        // run infinite loop to listen for client requests
        while (true) {
            Socket socket = null;
            try {
                // socket to receive incoming client requests
                socket = serverSocket.accept();
                System.out.println("New client connected at port " + socket.getPort());
                // open input and output streams
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                // create new thread for client handler
                Thread thread = new ClientHandler(socket, inputStream, outputStream, dataFile);
                thread.start();
                System.out.println("Created new client handler on " + thread.getName());
            } catch (Exception e) {
                socket.close();
                e.printStackTrace();
            }
        }
    }
}