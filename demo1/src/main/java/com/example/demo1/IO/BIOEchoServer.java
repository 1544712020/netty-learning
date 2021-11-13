package com.example.demo1.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BIOEchoServer {

    public static void main(String[] args) throws IOException {
        // 启动服务端，绑定8001端口
        ServerSocket serverSocket = new ServerSocket(8001);

        System.out.println("server start");

        while (true) {
            // 开始接受客户端连接
            Socket socket = serverSocket.accept();

            System.out.println("one client conn" + socket);

            // 启动线程处理连接数据
            new Thread(()->{
                try {
                    // 读取数据（input和output是相对于客户端的）
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg;
                    // 使用readLine读取服务端的每一行数据
                    while ((msg = reader.readLine()) != null) {
                        System.out.println("receive msg：" + msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

}
