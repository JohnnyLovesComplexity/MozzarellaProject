package org.jlc.arar.mozzarella.waterfox;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

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

		try {
			System.out.println("Tentative de connexion");
			Socket con_serv = new Socket(InetAddress.getByAddress("127.0.0.1".getBytes()),80);
			System.out.println("Connexion etablie");

			//flux de sortie
			InputStream inp = con_serv.getInputStream();

			//flux d'entrée
			OutputStream op = con_serv.getOutputStream();


			BufferedReader in = new BufferedReader(new InputStreamReader(inp));
			PrintWriter printWriter = new PrintWriter(op);

			Scanner sc = new Scanner(System.in);

			Thread envoyer = new Thread(new Runnable() {
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
			envoyer.start();

			Thread recevoir = new Thread(new Runnable() {
				String message;
				@Override
				public void run() {
					try {
						message = in.readLine();
						while(message!=null){
							System.out.println("Serveur : " + message);
							message = in.readLine();
						}
					} catch (IOException e) {
						e.printStackTrace();
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

			/*String response = in.readLine();
			if (response.contains("ok")) {
				System.out.println("Connexion établie");
				System.out.println(response);
			} else {
				while (true) {
					System.out.print("La connexion n'a pas pu être établie.");
				}
			}*/

			con_serv.close();

		} catch (IOException e) {

			e.printStackTrace();
		}

	}
}
