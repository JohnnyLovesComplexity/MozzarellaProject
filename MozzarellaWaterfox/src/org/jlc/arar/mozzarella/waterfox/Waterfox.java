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
		TextField field = new TextField();
        Button buttonS = new Button("Submit");

        // Create a WebView
        WebView browser = new WebView();
        // Get WebEngine via WebView
        WebEngine webEngine = browser.getEngine();

        VBox root = new VBox();
        HBox hb = new HBox();
        hb.getChildren().addAll(field, buttonS);
        root.setPadding(new Insets(5));
        root.setSpacing(5);
        root.getChildren().addAll(hb, browser);

        Scene scene = new Scene(root);

        primaryStage.setTitle("Mozzarella Waterfox");
        primaryStage.setScene(scene);
        primaryStage.setWidth(450);
        primaryStage.setHeight(300);

        primaryStage.show();

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
						"2 : Acceder à l'image 3-fromages.jpg\n" +
						"3 : Deposer votre propre fichier");
				int i = sc.nextInt();
				String request = "";
				switch (i){
					case 1 :
						getRequest(printWriter,con_serv, "/avis-recette.txt ");
						break;
					case 2 :
						getRequest(printWriter,con_serv, "/3-fromages.jpg ");
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
								if(i==2){
									DataInputStream inIm = new DataInputStream(con_serv.getInputStream());
									OutputStream dos = new FileOutputStream("MozzarellaWaterfox/3-fromages.jpg");
									int count;
									byte[] buffer = new byte[18000];
									boolean eohFound = false;
									while ((count = inIm.read(buffer)) != -1)
									{
										if(!eohFound){
											String string = new String(buffer, 0, count);
											int indexOfEOH = string.indexOf("\r\n\r\n");
											if(indexOfEOH != -1) {
												count = count-indexOfEOH-4;
												buffer = string.substring(indexOfEOH+4).getBytes();
												eohFound = true;
											} else {
												count = 0;
											}
										}
										dos.write(buffer, 0, count);
										dos.flush();
									}

								}else{
									message += "\r\n" + line;
								}
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
                            /*assert message != null;
                            String[] lines = message.split("\r\n");
                            boolean suivante = false;
                            StringBuilder content = new StringBuilder();
                            for (String line:lines) {
                                if(line.toLowerCase().contains("image/jpg"))
                                    suivante = true;
                                else if(suivante){
                                    content.append(line);
                                }
                            }
                            FileGenerator.generateFile(content.toString(),"MozzarellaWaterfox/receveided/3-fromages.jpg");*/
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
			String path = "MozzarellaWaterfox/"+name;
			String contentOfFile = FileGenerator.readContent(path);
			request.print("PUT /" + name + " HTTP/1.1\r\n"); // "+path+"
			request.print("Accept-Language: en-us\r\n");
			request.print("Connection: Keep-Alive\r\n");
			request.print("Content-type: text/html\r\n");
			request.print("Content-Length: 15\r\n");

			System.out.println("PUT Request Header Sent!");

			// Send the Data to be PUT
			request.print(contentOfFile);
			request.print("\r\n");
			request.flush();

			System.out.println("PUT Data Sent!");

		}catch (IOException ex){
			System.out.println(name + " doesn't exist.");
		}


	}


	private static void getRequest(PrintWriter request, Socket sock,String path){

    	if(path.contains("jpg")){
			DataOutputStream bw = null;
			try {
				bw = new DataOutputStream(sock.getOutputStream());
				bw.writeBytes("GET "+path+" HTTP/1.1\r\n\r\n");
				bw.writeBytes("Host: www.pizza.com:80\r\n\r\n");
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
