package io.github.saneea.homenettester;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientExchanger extends BlockExchanger {

	private static double BYTES_PER_MILLIS_TO_MB_PER_SECONDS_RATIO = //
			1000d// millis -> seconds
					/ 1024d// bytes -> kB
					/ 1024d;// kb -> mB

	private final int blocksCount;

	private ClientExchanger(Socket socket, int blocksCount) {
		super(socket);
		this.blocksCount = blocksCount;
	}

	public static void main(String[] args) {

		String host = System.getProperty("host");
		if (host == null) {
			System.err.println("Please specify JVM property 'host'");
			System.exit(1);
		}

		Integer port = Integer.getInteger("port");
		if (port == null) {
			System.err.println("Please specify JVM property 'port'");
			System.exit(1);
		}

		Integer delayAfterPing = Integer.getInteger("delayAfterPing");
		if (delayAfterPing == null) {
			System.err.println("Please specify JVM property 'delayAfterPing'");
			System.exit(1);
		}

		Integer blocksCount = Integer.getInteger("blocksCount");
		if (blocksCount == null) {
			System.err.println("Please specify JVM property 'blocksCount'");
			System.exit(1);
		}

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleWithFixedDelay(() -> {
			try {
				fetchStatistic(host, port, blocksCount);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}, 0, delayAfterPing, TimeUnit.SECONDS);
	}

	public static void fetchStatistic(String host, int port, int blocksCount)
			throws UnknownHostException, IOException, InterruptedException {

		long downloadTime;
		long downloadSize;
		long uploadTime;
		long uploadSize;

		try (Socket socket = new Socket(host, port)) {
			BlockExchanger exchanger = new ClientExchanger(socket, blocksCount);
			exchanger.exchange();
			downloadTime = exchanger.getDataReadTime();
			downloadSize = exchanger.getDataReadSize();
			uploadTime = exchanger.getDataWriteTime();
			uploadSize = exchanger.getDataWriteSize();
		}

		String downloadSpeed = calcSpeedString(downloadSize, downloadTime);
		String uploadSpeed = calcSpeedString(uploadSize, uploadTime);
		String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

		System.out.println(String.format("%s: down: %s, up: %s", time, downloadSpeed, uploadSpeed));
	}

	private static String calcSpeedString(long downloadSize, long downloadTime) {
		double speed = (downloadTime == 0)//
				? 0//
				: (double) downloadSize / (double) downloadTime;

		speed *= BYTES_PER_MILLIS_TO_MB_PER_SECONDS_RATIO;

		return String.format("%.2f MiB/s", speed);
	}

	@Override
	protected int negotiateBlockExchangeCount(InputStream inputStream, OutputStream outputStream) throws IOException {
		// System.err.println("Ask server exchange " + blocksCount + " blocks");
		outputStream.write(blocksCount);
		final int blockExchangeCount = inputStream.read();
		// System.err.println("Server ready to exchange " +
		// blockExchangeCount + " blocks");
		return blockExchangeCount;
	}

}
