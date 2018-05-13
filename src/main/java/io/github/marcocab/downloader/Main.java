package io.github.marcocab.downloader;

public class Main {
	private static final String MULTIPART_FILE_INDEX = "https://raw.githubusercontent.com/jesussanchezoro/PracticaPC/master/descargas.txt";

	public static void main(String... args) {
		FileDownloadManager fm = new FileDownloadManager();
		fm.addMultiPartFileFromSource(MULTIPART_FILE_INDEX);
		fm.startDownload();
		System.out.println(fm.getMaximumThreadsNeeded());
	}
}
