package org.jlc.arar.mozzarella;

import java.io.*;

public class FileGenerator {

	private FileGenerator() { }
	
	@SuppressWarnings("Duplicates")
	public static boolean generateFile(String content, String pathfile) {
		FileWriter fw = null;
		BufferedWriter bw = null;
		
		boolean problem = false;
		
		try {
			fw = new FileWriter(pathfile);
			bw = new BufferedWriter(fw);
			
			bw.write(content);
		} catch (IOException ex) {
			ex.printStackTrace();
			problem = true;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				problem = true;
			}
			
			try {
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				problem = true;
			}
		}
		
		return !problem;
	}


	
	public static String readContent(String pathfile) throws IOException {
		StringBuilder content = new StringBuilder();
		
		FileReader fr = null;
		BufferedReader br = null;
		
		try {
			fr = new FileReader(pathfile);
			br = new BufferedReader(fr);
			
			String line;
			while ((line = br.readLine()) != null)
				content.append(line)
						.append("\n");
		} catch (IOException ex) {
			throw ex;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			try {
				if (fr != null)
					fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return content.toString();
	}

	public static byte[] readContentImage(String pathfile) throws IOException {
		//1- Cree un objet file pointant sur ton image
		File imageFile = new File(pathfile);
		//2- Cree un stream afin de pouvoir lire dans les donnees binaires
		FileInputStream imageFileStr = new FileInputStream(imageFile);
		//3- Recupere la taille du buffer
		int longueurDuFichier = imageFileStr.available();
		//4- Recupere le bytes[] complet
		byte[] buffer = new byte[longueurDuFichier];
		imageFileStr.read(buffer);
		return  buffer;
	}
}
