package io.github.saneea.homenettester;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public abstract class BlockExchanger {

	private static int BYTES_BLOCK_SIZE = 1024 * 1024;

	private static Random RANDOM = new Random();

	private final Socket socket;

	private long dataWriteTime;
	private long dataReadTime;
	private long dataWriteSize;
	private long dataReadSize;
	private int blockExchangeCount;

	BlockExchanger(Socket socket) {
		this.socket = socket;
	}

	public void exchange() throws InterruptedException {
		OutputStream outputStream;
		InputStream inputStream;
		try {
			outputStream = socket.getOutputStream();
			inputStream = socket.getInputStream();
			blockExchangeCount = negotiateBlockExchangeCount(inputStream, outputStream);
			exchangeBlocks(outputStream, inputStream);
		} catch (Exception e) {
			dataWriteSize = 0;
			dataReadSize = 0;
			e.printStackTrace();
		}
	}

	protected abstract int negotiateBlockExchangeCount(InputStream inputStream, OutputStream outputStream)
			throws IOException;

	public long getDataWriteTime() {
		return dataWriteTime;
	}

	public long getDataReadTime() {
		return dataReadTime;
	}

	public long getDataWriteSize() {
		return dataWriteSize;
	}

	public long getDataReadSize() {
		return dataReadSize;
	}

	private void exchangeBlocks(OutputStream outputStream, InputStream inputStream) throws InterruptedException {
		Thread t = receiveInBackground(inputStream);

		sendBlocks(outputStream);

		t.join();
	}

	private void sendBlocks(OutputStream outputStream) {
		byte[] bytes = new byte[BYTES_BLOCK_SIZE];
		dataWriteTime = 0;
		dataWriteSize = 0;
		for (int i = 0; i < blockExchangeCount; ++i) {
			RANDOM.nextBytes(bytes);
			long startTimeForWriteBlock = System.currentTimeMillis();
			try {
				outputStream.write(bytes);
				dataWriteSize += bytes.length;
			} catch (Exception e) {
				dataWriteSize = 0;
				e.printStackTrace();
			} finally {
				dataWriteTime += (System.currentTimeMillis() - startTimeForWriteBlock);
			}
		}
	}

	private Thread receiveInBackground(InputStream inputStream) {
		Thread t = new Thread(() -> {
			byte[] bytes = new byte[BYTES_BLOCK_SIZE];
			dataReadSize = 0;
			int wasRead;
			long startTimeForRead = System.currentTimeMillis();
			try {
				while (dataReadSize < blockExchangeCount * BYTES_BLOCK_SIZE//
						&& (wasRead = inputStream.read(bytes)) != -1) {
					dataReadSize += wasRead;
				}

			} catch (IOException e) {
				dataReadSize = 0;
				e.printStackTrace();
			} finally {
				dataReadTime = System.currentTimeMillis() - startTimeForRead;
			}
		});

		t.start();
		return t;
	}

}
