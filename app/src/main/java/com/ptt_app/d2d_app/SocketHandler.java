package com.ptt_app.d2d_app;

import java.net.Socket;
import java.util.ArrayList;

public class SocketHandler {
    private static Socket socket;
    private static ArrayList<Socket> sockets = new ArrayList<>();

    public static synchronized Socket getSocket(){
        return socket;
    }

    public static synchronized ArrayList<Socket> getSockets(){
        return sockets;
    }

    public static synchronized void setSocket(Socket socket){
        SocketHandler.socket = socket;
        SocketHandler.sockets.add(socket);
    }
}
