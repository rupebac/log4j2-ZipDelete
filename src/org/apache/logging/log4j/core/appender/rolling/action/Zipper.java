package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zipper class
 * @author Rubén Pérez
 */
public class Zipper {
	private File outputZip;

	private List<File> filesToZip= new ArrayList<File>();
	
	public Zipper(File outputZip) {
		this.outputZip = outputZip;
	}
	
	public void addFileToZip(File f) {
		if(!f.isFile()) {
			throw new IllegalStateException("Trying to fix a file that does not exist");
		}
		this.filesToZip.add(f);
	}
	
	public void zip() throws FileNotFoundException, IOException {
		if(this.filesToZip.isEmpty())
			return;
		FileOutputStream osZip = new FileOutputStream(outputZip);
		try {
			ZipOutputStream zos = new ZipOutputStream(osZip);
			try {
				for(File f: filesToZip) {
					addFileToZip(f, zos);
				}
			} finally {
				zos.close();
			}
		} finally {
			osZip.close();
		}
	}
	
	private static void addFileToZip(File fileToAdd, ZipOutputStream zos) throws IOException, FileNotFoundException {
		try (FileInputStream inputStream = new FileInputStream(fileToAdd)) {
			ZipEntry astraiaLic = new ZipEntry(fileToAdd.getName());
			zos.putNextEntry(astraiaLic);
			while (true) {
				int ch = inputStream.read();
				if (ch < 0)
					break;
				zos.write(ch);
			}
		}
	}
}
