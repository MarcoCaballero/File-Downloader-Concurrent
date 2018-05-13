package io.github.marcocab.downloader;

import static io.github.marcocab.downloader.ConcurrencyManager.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FileDownloadManager {

	private static final String FILE_URL_REGEXP = "^(https?|ftp)://.*$";
	private static final String FILE_NAME_REGEXP = "Fichero:"; //

	private FileDownloader fileDownloader;
	private ConcurrentMap<String, List<String>> parts;
	private int maximumThreadsNeeded;

	public FileDownloadManager() {
		parts = new ConcurrentHashMap<String, List<String>>();
	}

	public void startDownload() {
		fileDownloader = new FileDownloader(this.getMaximumThreadsNeeded());
		fileDownloader.process(parts);
		Method method;
		try {
			method = fileDownloader.getClass().getDeclaredMethod("download");
			createNThreads(this.getMaximumThreadsNeeded(), method, (Object) fileDownloader);
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		startThreadsAndWait();
	}

	public void addMultiPartFileFromSource(String filePartsIndexPath) {
		maximumThreadsNeeded = 0;
		int maximumNumberOfPieces = 0;
		List<String> partList = new ArrayList<>();
		try {
			URL url = new URL(filePartsIndexPath);
			Scanner scanner = new Scanner(url.openStream());
			String currentName = "";
			while (scanner.hasNext()) {
				String line = (String) scanner.nextLine();
				if (!line.matches(FILE_URL_REGEXP)) {
					updateParts(currentName, partList);
					partList = new ArrayList<>();
					currentName = line.replace(FILE_NAME_REGEXP, "");
					updateNeededThreads(maximumNumberOfPieces);
					maximumNumberOfPieces = 0;
				} else {
					partList.add(line);
					maximumNumberOfPieces++;
				}
			}
			updateParts(currentName, partList);
			scanner.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public int getMaximumThreadsNeeded() {
		return maximumThreadsNeeded;
	}

	public void setMaximumThreadsNeeded(int maximumThreadsNeeded) {
		this.maximumThreadsNeeded = maximumThreadsNeeded;
	}

	private void updateNeededThreads(int maximumNumberOfPieces) {
		if (this.maximumThreadsNeeded < maximumNumberOfPieces)
			this.maximumThreadsNeeded = maximumNumberOfPieces;
	}

	private void updateParts(String currentName, List<String> partList) {
		if (currentName != "")
			this.parts.put(currentName, partList);
	}

}
