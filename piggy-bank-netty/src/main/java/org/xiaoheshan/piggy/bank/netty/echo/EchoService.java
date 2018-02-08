package org.xiaoheshan.piggy.bank.netty.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * echo简单服务器demo
 *
 * @author _Chf
 * @since 02-01-2018
 */
public class EchoService {

    private final int post;

    public EchoService(int post) {
        this.post = post;
    }

    public void start() throws Exception {
        /* 处理线程池，默认线程数为0 */
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            /* 单线程模型 */
            ChannelFuture future = new ServerBootstrap()
                    /* 若指定BossGroup和WorkGroup则为多线程模型 */
                    .group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(post)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    })
                    .bind()
                    .sync();
            System.out.println(EchoService.class.getName() + " started and listen on " + future.channel().localAddress());
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    /**
     * 服务器处理器
     */
    private static class EchoServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("Server received: " + msg);
            /* 写进缓存 */
            ctx.write(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            /* 读取完成后，刷到输出流 */
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new EchoService(65535).start();
    }

}
