package io.github.marcocab.downloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrencyManager {

	private static final String DEFAULT_MUTEX_NAME = "ConcurrencyManager";
	private static final CountDownLatch latch = new CountDownLatch(1);
	private static final ConcurrentMap<String, Thread> threads = new ConcurrentHashMap<String, Thread>();
	private static final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();
	private static int counter = -1;

	public static void createNThreads(int numberOfThreads, Method method, Object obj) {
		for (int i = 0; i < numberOfThreads; i++) {
			createThread(method, obj);
		}
	}

	public static void createThread(final Method method, Object obj) {
		String threadName = "ConcurrencyManager_Thread_" + ++counter;
		final Thread t = createThread(threadName, method, obj);
		threads.put(threadName, t);
	}

	public static void sleepRandom(long millis) {
		sleep((long) (Math.random() * millis));
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void enterMutex() {
		enterMutex(DEFAULT_MUTEX_NAME);
	}

	public static void exitMutex() {
		exitMutex(DEFAULT_MUTEX_NAME);
	}

	public static void enterMutex(String mutexId) {
		ReentrantLock lock;
		synchronized (locks) {
			lock = locks.get(mutexId);
			if (lock == null) {
				lock = new ReentrantLock();
				locks.put(mutexId, lock);
			}
		}
		lock.lock();
	}

	public static void exitMutex(String mutexId) {
		ReentrantLock lock = locks.get(mutexId);
		synchronized (locks) {
			if (lock == null)
				throw new RuntimeException(String.format("MutexId: " + mutexId + " has not been declared yet."));

			if (!lock.isHeldByCurrentThread())
				throw new RuntimeException("The thread (" + getThreadName() + ") is trying to unlock mutex:(" + mutexId
						+ ") but other thread is holding the Mutex");
		}
		lock.unlock();
	}

	public static void startThreadsAndWait() {
		long startTime = System.currentTimeMillis();
		for (Thread t : threads.values()) {
			t.start();
		}

		latch.countDown();

		for (Thread t : threads.values()) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		long duration = System.currentTimeMillis() - startTime;

		System.out.println("\nConcurrent code spend " + duration + " millis");
	}

	public static String getThreadName() {
		return Thread.currentThread().getName();
	}

	public static void println(String text) {
		sleepRandom(10);
		System.out.println(text);
		sleepRandom(10);
	}

	private static Thread createThread(String threadName, final Method execMethod, Object obj) {
		final Thread t = new Thread(threadName) {
			@SuppressWarnings("deprecation")
			public void run() {
				try {
					latch.await();
					sleepRandom(10);
					execMethod.invoke(obj);
				} catch (Exception e) {

					synchronized (threads) {
						for (Thread thread : threads.values()) {
							if (thread != Thread.currentThread() && thread.isAlive()) {
								thread.stop();
							}
						}
						if (e instanceof InvocationTargetException) {
							Throwable originalException = e.getCause();
							System.out.format("Exception in Thread [%s]\n", Thread.currentThread().getName());

							originalException.printStackTrace();
						} else {
							e.printStackTrace();
						}
						System.exit(1);

					}
				}
			}
		};
		return t;
	}
}
