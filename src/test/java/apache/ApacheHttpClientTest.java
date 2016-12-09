package apache;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class ApacheHttpClientTest {

	private CloseableHttpClient client;

	private AtomicInteger success = new AtomicInteger();
	private AtomicInteger failure = new AtomicInteger();

	@Before
	public void setupHttpClient() {

		SocketConfig socketConfig = SocketConfig.custom()
				.setSoTimeout(1000) // ms
				.build();

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(1000)
				.setConnectTimeout(2000)
				.setSocketTimeout(2000)
				.build();

		this.client = HttpClientBuilder.create()
				.setMaxConnPerRoute(20)
				.setMaxConnTotal(20)
				.setDefaultSocketConfig(socketConfig)
				.setDefaultRequestConfig(requestConfig)
				.build();

	}

	@Test
	public void shouldWork() throws InterruptedException {


		List<Runnable> runnables = new ArrayList<>();

		for (int i = 0; i < 20; i++) {

			Runnable reqExecutor = () -> {
				for (int i1 = 0; i1 < 1000; i1++) {
					try (CloseableHttpResponse resp = client.execute(new HttpPost("https://example.com/"))) {
						Thread.sleep((long) (Math.random() * 5000));
						System.out.println("Req success (" + resp.getStatusLine().getStatusCode() + ") " + success.incrementAndGet());


					} catch (Exception e) {
						System.out.println("Req failed (" + e.getMessage() + ") " + failure.incrementAndGet());

					}
				}
			};

			runnables.add(reqExecutor);


		}

		assertConcurrent("Keep it running", runnables, 10000);


	}


	public static void assertConcurrent(final String message, final List<Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
		final int numThreads = runnables.size();
		final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
		final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		try {
			final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
			final CountDownLatch afterInitBlocker = new CountDownLatch(1);
			final CountDownLatch allDone = new CountDownLatch(numThreads);
			for (final Runnable submittedTestRunnable : runnables) {
				threadPool.submit(() -> {
					allExecutorThreadsReady.countDown();
					try {
						afterInitBlocker.await();
						submittedTestRunnable.run();
					} catch (final Throwable e) {
						exceptions.add(e);
					} finally {
						allDone.countDown();
					}
				});
			}
			// wait until all threads are ready
			assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
			// start all test runners
			afterInitBlocker.countDown();
			assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
		} finally {
			threadPool.shutdownNow();
		}
		assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
	}

}
