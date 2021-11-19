package com.example.demo2.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NettyTest {

    private static final int PORT = 8080;

    public static void main(String[] args) {

        ChannelFuture f = null;

        //1:声明线程池
        // 创建bossGroup用于处理Accept事件（一般声明一个线程）
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 创建workerGroup用于处理读写事件
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        //2:创建服务端引导类（用来集成所有配置）
        // 一种是服务端引导类（ClientBootstrap ），一种是客户端引导类（ServerBootstrap ）
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        //3:设置线程池（用于说明Netty以什么样的线程模式运行）
        serverBootstrap.group(bossGroup, workerGroup);

        //4:设置ServerSocketChannel类型（设置Netty以什么样的IO模型运行）
        serverBootstrap.channel(NioServerSocketChannel.class);

        //5:设置参数（可选）
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 100);

        //6:设置Handler（可选）
        serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));

        //7:编写并设置子Handler（Netty 中的 Handler 分成两种，一种叫做 Inbound，一种叫做 Outbound。）

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                // 可以添加多个子Handler
                p.addLast(new LoggingHandler(LogLevel.INFO));
                // p.addLast(new EchoServerHandler());
            }
        });

        class EchoServerHandler extends ChannelInboundHandlerAdapter {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                // 读取数据后写回客户端
                ctx.write(msg);
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) {
                ctx.flush();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                cause.printStackTrace();
                ctx.close();
            }

        }

        //8:绑定端口
        try {
            f = serverBootstrap.bind(PORT).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //9:等待服务端端口关闭
        try {
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //10:优雅地关闭线程池
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

    }

}
