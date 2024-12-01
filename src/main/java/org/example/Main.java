package org.example;

import org.example.algorithm.Server;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        Server udpServer = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Ctrl+C przechwycone! Zakończam działanie aplikacji...");
            udpServer.stop();
        }));



        InetAddress address = InetAddress.getByName("127.0.0."+args[0]);
        udpServer.start(address);

        while (true) {

        }
    }
}