package org.jlc.arar.mozzarella.waterfox;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jlc.arar.mozzarella.FileGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.SortedMap;

public class Waterfox{
	
	private BorderPane root;
	
	private ToolBar tb_bar;
	private TextField tf_url;
	private Button bt_go;
	private Button bt_refresh;
	
	private WebView w_view;
	private WebEngine w_engine;



	public static void main(String[] args) {
		System.out.println("Connexion...");
		try{
			Socket con_serv = new Socket(InetAddress.getByName("127.0.0.1"),80);
			try {

				System.out.println("Connected");

				//flux de sortie
				InputStream inp = con_serv.getInputStream();

				//flux d'entrée
				OutputStream op = con_serv.getOutputStream();


				BufferedReader in = new BufferedReader(new InputStreamReader(inp));
				PrintWriter printWriter = new PrintWriter(op);

				Scanner sc = new Scanner(System.in);

				printWelcome();
				System.out.println("Vous souhaitez :\n" +
						"1 : Acceder au fichier avis-recette.txt\n" +
						"2 : Acceder à l'image head.jpg\n" +
						"3 : Deposer votre propre fichier");
				int i = sc.nextInt();
				String request = "";
				switch (i){
					case 1 :
						getRequest(printWriter,con_serv, "/Pizza/site/avis-recette.txt ");
						break;
					case 2 :
						getRequest(printWriter,con_serv, "/Pizza/site/images/head.jpg");
						break;
					case 3 :
						putRequest(printWriter, con_serv, "MozzarellaWaterfox/file/blackhole.jpg");
						break;
				}
				String message = "";

				try {
					String line = in.readLine();
					while(line !=null && !line.equals(" ")){
						message += "\r\n" + line;
						line = in.readLine();
					}
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Response Recieved!!\n" + message);
				if(i == 1)
				{
					assert message != null;
					String[] lines = message.split("\r\n");
					boolean suivante = false;
					StringBuilder content = new StringBuilder();
					for (String line:lines) {
						if(line.toLowerCase().contains("text/html"))
							suivante = true;
						else if(suivante){
							content.append(line);
						}
					}
					FileGenerator.generateFile(content.toString(),"MozzarellaWaterfox/receveided/avis-recette.txt");
				}
				else if(i == 2)
				{
					assert message != null;
					String[] lines = message.split("\r\n");
					StringBuilder result = new StringBuilder();
					boolean found = false;
					for (String line: lines) {
						if(found)
							result.append(line).append("\r\n");
						if(line.contains("Content-Length"))
							found = true;
					}
							/*int i =0;
							StringBuilder content = new StringBuilder();
							while(content.toString().contains("\r\n\r\n")){
								content.append(message.charAt(i));
								i++;
							}
							String result = message.replace(content.toString(),"");*/

					FileGenerator.generateFile(result.toString(),"MozzarellaWaterfox/receveided/head.jpg");
				}
				printWriter.close();
				try {
					con_serv.close();
					System.out.println("Serveur déconecté");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Error");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}catch(java.net.ConnectException ce){
			System.out.println("La connexion a échouée.\n");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void printWelcome()
	{
		System.out.println("--------");
		System.out.println("Bienvenue !");
		System.out.println("--------");
		System.out.println("Serveur WEB : Par Valentin Berger, Léa Chemoul, Philippine Cluniat, Amal Ben Ismail");
		System.out.println("--------");
	}

	private static void putRequest(PrintWriter request, Socket socket, String imgPath){

		/*System.out.println("Name of the file to send :");
		Scanner sc2 = new Scanner(System.in);
		String name = sc2.nextLine();*/
		try{
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			File file = new File(imgPath);
			byte[] b = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(b);
			fis.close();
			dos.write("PUT /blacckhole.jpg HTTP/1.1\r\n".getBytes());
			dos.write("Content-length: ".getBytes());
			dos.writeInt((int) file.length());
			dos.write("\r\n".getBytes());
			dos.flush();
			dos.write(b, 0, b.length);
			//dos.write("\r\n\r\n".getBytes());
			dos.flush();

			/*String path = "MozzarellaWaterfox/file/"+name;
			String contentOfFile = FileGenerator.readContent(path);
			request.print("PUT /" + name + " HTTP/1.1\r\n");
			request.print("Accept-Language: en-us\r\n");
			request.print("Connection: Keep-Alive\r\n");
			request.print("Content-type: text/html\r\n");
			request.print("Content-Length: \r\n");

			System.out.println("PUT Request Header Sent!");

			// Send the Data to be PUT
			request.print(contentOfFile);
			request.print("\r\n\r\n");
			request.flush();

			System.out.println("PUT Data Sent!");*/

		}catch (IOException ex){
			System.out.println("File doesn't exist.");
		}
	}


	private static void getRequest(PrintWriter request, Socket sock,String path){

    	if(path.contains("jpg")){
			DataOutputStream bw = null;
			try {
				bw = new DataOutputStream(sock.getOutputStream());
				bw.writeBytes("GET "+path+" HTTP/1.1\r\n");
				bw.writeBytes("Host: www.pizza.com:80\r\n");
				bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			request.print("GET " + path + " HTTP/1.1\r\n");
			request.print("Accept-Language: en-us\r\n");
			request.print("Connection: Keep-Alive\r\n");
			request.print("\r\n\r\n");

			request.flush();
		}
		System.out.println("GET Request Header Sent!");

	}
}
