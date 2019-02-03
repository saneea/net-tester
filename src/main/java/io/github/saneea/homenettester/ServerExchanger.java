package io.github.saneea.homenettester;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerExchanger extends BlockExchanger {

	private ServerExchanger(Socket socket) {
		super(socket);
	}

	private static int MAX_BLOCK_EXCHANGE_COUNT = 255;

	public static void main(String[] args) throws IOException, InterruptedException {

		Integer port = Integer.getInteger("port");
		if (port == null) {
			System.err.println("Please specify JVM property 'port'");
			System.exit(1);
		}

		try (ServerSocket ss = new ServerSocket(port)) {
			while (true) {
				Socket socket = ss.accept();
				new Thread(() -> {
					try {
						new ServerExchanger(socket).exchange();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		}

	}

	@Override
	protected int negotiateBlockExchangeCount(InputStream inputStream, OutputStream outputStream) throws IOException {
		int offeredBlockExcangeCount = inputStream.read();
		// System.out.println("Client asked exchange " +
		// offeredBlockExcangeCount + " blocks");
		final int blockExchangeCount = offeredBlockExcangeCount <= MAX_BLOCK_EXCHANGE_COUNT//
				? offeredBlockExcangeCount //
				: MAX_BLOCK_EXCHANGE_COUNT;
		// System.out.println("Say client server is ready to exchange " +
		// blockExchangeCount + " blocks");
		outputStream.write(blockExchangeCount);
		return blockExchangeCount;
	}

}
