package com.mordechai.socketclientserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        try {
            Scanner scanner = new Scanner(System.in);
            // establish connection with server
            int port = 5056;
            Socket socket;
            try {
                socket = new Socket(InetAddress.getByName("localhost"), port);
                System.out.println("Connected to server at port " + socket.getLocalPort() + "\n");
            } catch (Exception e) {
                throw new IllegalStateException("No server listening at port " + port);
            }
            // open input and output streams
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            // exchange information between client and client handler
            while (true) {
                System.out.println(inputStream.readUTF());
                String toSend = scanner.nextLine();
                outputStream.writeUTF(toSend);

                // close connection and break loop if client requests exit
                if (toSend.toLowerCase().equals("exit")) {
                    socket.close();
                    System.out.println("Connection to server terminated at port " + socket.getLocalPort() + "\n");
                    break;
                }

                // print data returned by server
                String received = inputStream.readUTF();
                System.out.println(received);
            }
            // close streams
            scanner.close();
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}