package io.github.marcocab.downloader;

import static io.github.marcocab.downloader.ConcurrencyManager.enterMutex;
import static io.github.marcocab.downloader.ConcurrencyManager.exitMutex;
import static io.github.marcocab.downloader.ConcurrencyManager.getThreadName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

public class FileDownloader {

	private volatile ConcurrentMap<String, List<String>> parts;
	private static CountDownLatch latch = new CountDownLatch(1);
	private volatile int docsLeft;
	private volatile int fragPendiente;
	private volatile int hilosTerminados;
	private int maxConcurrentDownloads;

	public FileDownloader(int maxConcurrentDownloads) {
		this.maxConcurrentDownloads = maxConcurrentDownloads;
		this.parts = new ConcurrentHashMap<>();
	}

	public void process(ConcurrentMap<String, List<String>> parts) {
		this.parts = parts;
		docsLeft = 0;
		fragPendiente = 0;
		hilosTerminados = 0;
	}

	public void download() {

		while (true) {
			descargaFragmentos(docsLeft);

			boolean isReadyToMerge = false;

			enterMutex("hilosTerminados");
			hilosTerminados++;
			isReadyToMerge = hilosTerminados >= this.maxConcurrentDownloads;
			exitMutex("hilosTerminados");

			if (isReadyToMerge) {
				System.out.println("Merging!...." + getThreadName());
				System.out.println("Merging!...." + hilosTerminados);
				FileUtils.mergeFile("downloads", this.parts.keySet().toArray()[docsLeft].toString().trim());
				docsLeft++;
				fragPendiente = 0;
				hilosTerminados = 0;
				latch.countDown();
				latch = new CountDownLatch(1);
			} else {
				try {
					latch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (docsLeft == this.parts.keySet().size()) {
				break;
			}
		}

	}

	private void descargaFragmentos(int docDescargar) {
		while (true) {
			enterMutex("fragPendiente");
			if (fragPendiente >= this.parts.get(this.parts.keySet().toArray()[docDescargar]).size()) {
				exitMutex("fragPendiente");
				break;
			}

			int fragDescargar = fragPendiente;
			fragPendiente++;
			exitMutex("fragPendiente");
			downloadPart(fragDescargar);

		}
	}

	private void downloadPart(int partDescargar) {
		String currentDocName = this.parts.keySet().toArray()[docsLeft].toString();
		String urlPathName = this.parts.get(currentDocName).get(partDescargar);
		System.out.println("Thread: " + getThreadName() + " downloading: " + urlPathName + "...");
		Path pathOut = Paths.get("downloads/"
				+ urlPathName.replace("https://github.com/jesussanchezoro/PracticaPC/raw/master/", "").trim());
		if (!pathOut.toFile().exists())
			pathOut.toFile().mkdirs();
		FileUtils.downloadFileFromUrl(pathOut, urlPathName);
	}
}
