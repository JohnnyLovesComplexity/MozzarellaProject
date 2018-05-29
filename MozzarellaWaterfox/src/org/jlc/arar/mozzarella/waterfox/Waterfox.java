package org.jlc.arar.mozzarella.waterfox;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jlc.arar.mozzarella.FileGenerator;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Waterfox extends Application {
	
	private BorderPane root;
	
	private ToolBar tb_bar;
	private TextField tf_url;
	private Button bt_go;
	private Button bt_refresh;
	
	private WebView w_view;
	private WebEngine w_engine;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		root = new BorderPane();
		tf_url = new TextField();
		bt_go = new Button("Go");
		bt_refresh = new Button("Refresh");
		w_view = new WebView();
		w_engine = w_view.getEngine();

		System.out.println("Connexion...");
		try{
			Socket con_serv = new Socket(InetAddress.getByName(/*"192.168.43.204"*/"127.0.0.1"),8080);
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
						"2 : Acceder à l'image pizza.png\n" +
						"3 : Deposer votre propre fichier");
				int i = sc.nextInt();
				String request = "";
				switch (i){
					case 1 :
						getRequest(printWriter, "/avis-recette.txt ");
						break;
					case 2 :
						getRequest(printWriter, "/3-fromages.jpg ");
						break;
					case 3 :
						putRequest(printWriter);
						break;
				}
			/*Thread envoyer = new Thread(new Runnable() {
				String message;
				@Override
				public void run() {
					while(true){
						message = sc.nextLine();
						printWriter.println(message);
						printWriter.flush();
					}
				}
			});
			envoyer.start();*/


				Thread recevoir = new Thread(new Runnable() {
					String message ="";
					@Override
					public void run() {
						try {
							String line;
							while((line = in.readLine()) !=null){
								message += "\r\n" + line;
							}

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
                        if(i == 2)
                        {
                            assert message != null;
                            String[] lines = message.split("\r\n");
                            boolean suivante = false;
                            StringBuilder content = new StringBuilder();
                            for (String line:lines) {
                                if(line.toLowerCase().contains(""))
                                    suivante = true;
                                else if(suivante){
                                    content.append(line);
                                }
                            }
                            FileGenerator.generateFile(content.toString(),"MozzarellaWaterfox/receveided/3-fromages.jpg");
                        }
						System.out.println("Serveur déconecté");
						printWriter.close();
						try {
							con_serv.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});

				recevoir.start();


			} catch (IOException e) {

				e.printStackTrace();
			}

		}catch(java.net.ConnectException ce){
			System.out.println("La connexion a échouée.\n");
		}

	}

	private void printWelcome()
	{
		System.out.println("--------");
		System.out.println("Bienvenue !");
		System.out.println("--------");
		System.out.println("Serveur WEB : Par Valentin Berger, Léa Chemoul, Philippine Cluniat, Amal Ben Ismail");
		System.out.println("Derniere version : 27/05/2018");
		System.out.println("--------");
	}

	private static void putRequest(PrintWriter request){

		System.out.println("Name of the file to send :");
		Scanner sc2 = new Scanner(System.in);
		String name = sc2.nextLine();
		try{
			String contentOfFile = FileGenerator.readContent("/"+name);
			String path = "/new.txt";
			request.print("PUT " + path + " HTTP/1.1\r\n"); // "+path+"
			request.print("Accept-Language: en-us\r\n");
			request.print("Connection: Keep-Alive\r\n");
			request.print("Content-type: text/html\r\n");
			request.print("Content-Length: 0\r\n");
			request.print("\r\n");

			System.out.println("PUT Request Header Sent!");

			// Send the Data to be PUT
			request.println(contentOfFile);
			request.flush();

			System.out.println("PUT Data Sent!");

		}catch (IOException ex){
			System.out.println(name + "doesn't exist.");
		}


	}


	private static void getRequest(PrintWriter request, String path){

		request.print("GET " + path + " HTTP/1.1\r\n");
		request.print("Accept-Language: en-us\r\n");
		request.print("Connection: Keep-Alive\r\n");
		request.print("\r\n\r\n");

		request.flush();
		System.out.println("GET Request Header Sent!");

	}
}
