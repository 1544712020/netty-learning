package com.example.demo1.chat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class chatServer {

    public static void main(String[] args) throws IOException {
        // 创建选择器
        Selector selector = Selector.open();
        // 创建通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        System.out.println(InetAddress.getLocalHost());
        // 设置为非阻塞状态
        serverSocketChannel.configureBlocking(false);
        // 将accept事件绑定到selector上
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 阻塞在select上
            selector.select();
            // 获取选择器中的SelectionKey（1.读事件、2.写事件、3.连接事件、4.接受连接事件）
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            // 遍历selectKeys
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                // 如果是accept事件
                if (selectionKey.isAcceptable()) {
                    // 返回创建此键的通道
                    ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
                    // 接受该通道插座的连接
                    SocketChannel socketChannel = ssc.accept();
                    // 输出该通道插座连接到的远程地址
                    System.out.println("accept new conn: " + socketChannel.getRemoteAddress());
                    // 设置通道为非阻塞模式
                    socketChannel.configureBlocking(false);
                    //将通道注册到选择器上，并注册读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("测试用户是否连接...");
                    // 将聊天者的通道加入群聊
                    ChatHolder.join(socketChannel);
                }
                // 如果是读取事件
                else if (selectionKey.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    // 将数据读入到buffer中
                    int length = socketChannel.read(buffer);
                    if (length > 0) {
                        buffer.flip();
                        // buffer.remaining用于返回缓冲区中字节个数，用于设置字节数组的大小
                        byte[] bytes = new byte[buffer.remaining()];
                        // 将数据读入到byte数组中
                        buffer.get(bytes);

                        // 换行符会跟着消息一起传过来（使用replace将换行符取代为空格）
                        String content = new String(bytes, "UTF-8").replace("\r\n", "");
                        // 如果传输过来的数据字符串为quit则意味着退出群聊
                        if (content.equalsIgnoreCase("quit")) {
                            // 退出群聊
                            ChatHolder.quit(socketChannel);
                            selectionKey.cancel();
                            socketChannel.close();
                        }
                        // 如果传输过来的数据字符串补位quit则将消息发布到每个加入群聊的用户
                        else {
                            // 扩散
                            ChatHolder.propagate(socketChannel, content);
                        }
                    }
                }
                iterator.remove();
            }
        }
    }

    /**
     * 聊天者
     */
    private static class ChatHolder {
        private static final Map<SocketChannel, String> USER_MAP = new ConcurrentHashMap<>();

        /**
         * 加入群聊
         * @param socketChannel
         */
        public static void join(SocketChannel socketChannel) {
            // 有人加入就给他分配一个id（ThreadLocalRandom：与当前线程隔离的随机数生成器）
            String userId = "用户"+ ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            // 调用send方法发送一条消息
            send(socketChannel, "您的id为：" + userId + "\n\r");

            for (SocketChannel channel : USER_MAP.keySet()) {
                send(channel, userId + " 加入了群聊" + "\n\r");
            }

            // 将当前用户加入到map中
            USER_MAP.put(socketChannel, userId);
        }

        /**
         * 退出群聊
         * @param socketChannel
         */
        public static void quit(SocketChannel socketChannel) {
            String userId = USER_MAP.get(socketChannel);
            send(socketChannel, "您退出了群聊" + "\n\r");
            // 将退出群聊的用户从map中去除
            USER_MAP.remove(socketChannel);

            for (SocketChannel channel : USER_MAP.keySet()) {
                if (channel != socketChannel) {
                    send(channel, userId + " 退出了群聊" + "\n\r");
                }
            }
        }

        /**
         * 扩散说话的内容
         * @param socketChannel
         * @param content
         */
        public static void propagate(SocketChannel socketChannel, String content) {
            String userId = USER_MAP.get(socketChannel);
            for (SocketChannel channel : USER_MAP.keySet()) {
                if (channel != socketChannel) {
                    send(channel, userId + ": " + content + "\n\r");
                }
            }
        }

        /**
         * 发送消息
         * @param socketChannel
         * @param msg
         */
        private static void send(SocketChannel socketChannel, String msg) {
            try {
                // 分配一个新的写入字节缓冲区
                ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
                // 将需要传输的数据放入写入缓冲区
                writeBuffer.put(msg.getBytes());
                // 调换这个buffer的位置，并且设置当前位置为0，将缓存字节数组的下标调增为0，以便从头读取缓存区中的所有数据
                writeBuffer.flip();
                // 将缓存区中数据写入通道
                socketChannel.write(writeBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
