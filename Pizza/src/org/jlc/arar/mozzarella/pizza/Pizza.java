package org.jlc.arar.mozzarella.pizza;

import fr.berger.enhancedlist.lexicon.Lexicon;
import fr.berger.enhancedlist.lexicon.LexiconBuilder;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Pizza extends Application implements Runnable {
	
	public enum State {
		INITIALIZED,
		STARTED,
		PAUSED,
		STOPPED;
		
		@SuppressWarnings("ConstantConditions")
		@Contract(pure = true)
		public static boolean isRunning(@NotNull State state) {
			if (state == null)
				throw new NullPointerException();
			
			return state == STARTED || state == PAUSED;
		}
		@Contract(pure = true)
		public boolean isRunning() {
			return isRunning(this);
		}
	}
	
	private State state;
	private Thread th_route;
	
	private ServerSocket soc;
	private Lexicon<Thread> connections;
	
	private BorderPane root;
	private ToolBar tb_buttons;
	private Button bt_start;
	private Button bt_pause_resume;
	private Button bt_stop;
	private Button bt_clear;
	private WebView loggerView;
	private WebEngine loggerEngine;
	private final String headContent =
			"<html>" +
					"<head>" +
						"<meta charset=\"utf-8\"/>" +
						"<style>" +
						"p {" +
						"font-family: Calibri, Verdana, Arial, serif;" +
						"}" +
						"</style>" +
					"</head>" +
					"<body>";
	private String currentContent;
	private final String footContent =
					"</body>" +
				"</html>";
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		setState(State.INITIALIZED);
		th_route = new Thread(this);
		setCurrentContent("");
		
		connections = new LexiconBuilder<>(Thread.class)
				.setAcceptNullValues(false)
				.setAcceptDuplicates(false)
				.createLexicon();
		
		root = new BorderPane();
		tb_buttons = new ToolBar();
		bt_start = new Button("Start");
		bt_pause_resume = new Button("Pause");
		bt_stop = new Button("Stop");
		bt_clear = new Button("Clear");
		loggerView = new WebView();
		loggerEngine = loggerView.getEngine();
		
		bt_start.setOnMouseClicked(event -> {
			if (getState() == State.INITIALIZED) {
				startServer();
			}
		});
		
		bt_pause_resume.setOnMouseClicked(event -> {
			if (getState() == State.STARTED)
				pauseServer();
			else if (getState() == State.PAUSED)
				resumeServer();
		});
		
		bt_stop.setCancelButton(true);
		bt_stop.setOnMouseClicked(event -> {
			stopServer();
		});
		
		bt_clear.setOnMouseClicked(event -> {
			currentContent = "";
			loggerEngine.loadContent("");
		});
		
		tb_buttons.getItems().addAll(bt_start, bt_pause_resume, bt_stop, new Separator(Orientation.VERTICAL), bt_clear);
		root.setTop(tb_buttons);
		root.setCenter(loggerView);
		
		primaryStage.setScene(new Scene(root, 640, 480));
		primaryStage.setTitle("[Server] Pizza \uD83C\uDF55");
		primaryStage.show();
		
		initServer();
		th_route.start();
	}
	
	@Override
	public void run() {
		try {
			soc = new ServerSocket(80);
			
			while (getState() == State.INITIALIZED);
			
			while (getState().isRunning()) {
				while (getState() == State.PAUSED) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				
				Socket com_cli = soc.accept();
				System.out.println("Connected");
				StringBuilder message = new StringBuilder();
				BufferedReader in = new BufferedReader(new InputStreamReader(com_cli.getInputStream()));

				String line = "";
				line = in.readLine();
				message.append(line);

				// Manage the client by creating a thread for this one (see createThread() ).
				Thread t = createThread(com_cli, message.toString());
				t.start();
				connections.add(t);

				for (int i = 0; i < connections.size(); i++) {
					if (connections.get(i) == null || !connections.get(i).isAlive() || connections.get(i).isInterrupted()) {
						connections.remove(i);
						i--;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		stopServer();
	}

	private static  String constructResponse(String message){
		String response = "";
		String url;

		url = message.split("\r\n")[0].replace("GET ","")
				.replace("HTTP/1.1","")
				.replace(" ","");
		System.out.println(url);

		if(!(url.equals("localhost/recette.txt")
				|| url.equals("localhost/pizza.png"))){
			System.out.println("Le chemin spécifié dans votre requête n'existe pas;\n");
			response = constructResponseHeader(404);
		}
		else if(message.contains("GET")){
			if(url.equals("localhost/recette.txt")){
				try {
					String content = FileGenerator.readContent("Pizza/site/recette.txt");
					response = constructResponseHeader(200);
					response += content + "\r\n";
				} catch (IOException e) {
					System.out.println("Chemin spécifié introuvable\n");
					response = constructResponseHeader(404);
				}
			}else if(url.equals("localhost/pizza.png"))
			{
				System.out.println("Envoie de pizza.png");
				//envoie de la reponse relative a l'image
			}
		}else if(message.contains("PUT")){
			System.out.println("Votre fichier a bien été ajouté au serveur");

		}else{
			response = "OK";
		}

		return response;
	}


	// Construct Response Header
	private static String constructResponseHeader(int responseCode) {
		StringBuilder stringBuilder = new StringBuilder();

		if (responseCode == 200) {

			stringBuilder.append("HTTP/1.1 200 OK\r\n");
			stringBuilder.append("Date:" + getTimeStamp() + "\r\n");
			stringBuilder.append("Server:localhost\r\n");
			stringBuilder.append("Content-Type: text/html\r\n");
			stringBuilder.append("\r\n");

		} else if (responseCode == 404) {

			stringBuilder.append("HTTP/1.1 404 Not Found\r\n");
			stringBuilder.append("Date:" + getTimeStamp() + "\r\n");
			stringBuilder.append("Server:localhost\r\n");
			stringBuilder.append("\r\n");
		} else if (responseCode == 304) {
			stringBuilder.append("HTTP/1.1 304 Not Modified\r\n");
			stringBuilder.append("Date:" + getTimeStamp() + "\r\n");
			stringBuilder.append("Server:localhost\r\n");
			stringBuilder.append("\r\n");
		}
		return stringBuilder.toString();
	}

	private static String getTimeStamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
		String formattedDate = sdf.format(date);
		return formattedDate;
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
		
		stopServer();
		if (th_route != null)
			th_route.interrupt();
		
		for (Thread connection : connections)
			connection.interrupt();
		
		if (soc != null) {
			try {
				soc.close();
			} catch (IOException ignored) { }
		}
		
		log("Shutting down...");
		log("Good bye.");
		Platform.exit();
		Runtime.getRuntime().halt(0);
	}
	
	protected Thread createThread(@NotNull Socket com_cli, @NotNull String message) {
		return new Thread(() -> {
			log("New connection: " + com_cli.getInetAddress().getHostName() + " port " + com_cli.getPort());
			
			log("data received: \"" + message + "\".");
			
			// Sending an answer
			String answer = "";
			boolean stopServer = false;
			try{
				BufferedWriter response = new BufferedWriter(new OutputStreamWriter(com_cli.getOutputStream()));
				answer = constructResponse(message.toString());
				response.write(answer);
				log("Answered \"" + answer +"\"");
				response.flush();
			}catch(IOException ex){
				ex.printStackTrace();
			}

			/*try {
				if (message.toLowerCase().equals("time")) {
					SimpleDateFormat sdf = new SimpleDateFormat("hh-MM-ss");
					answer = sdf.format(new Date());
				} else if (message.toLowerCase().equals("exit")) {
					answer = "Shutting down...";
					stopServer = true;
				} else if (message.toLowerCase().equals("hello world!"))
					answer = "Hello back!";
				else
					answer = message;
				
				connection.answer(answer);
				log("Answered \"" + answer + "\"");
			} catch (OperationNotSupportedException ex) {
				ex.printStackTrace();
				log(ex.getMessage());
			}
			finally {
				if (stopServer)
					stopServer();
			}*/

			try {
				com_cli.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	@SuppressWarnings("ConstantConditions")
	public synchronized void log(@NotNull String log) {
		if (log == null)
			throw new NullPointerException();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd'/'MM'/'yyyy 'at' HH:mm:ss");
		String message = '[' + sdf.format(new Date()) + "] " + log;
		
		System.out.println(message);
		
		message = message.replace("[", "<b>[<span style=\"color: red;\">");
		message = message.replace(" at ", "</span> at <span style=\"color: blue;\">");
		message = message.replace("]", "</span>]</b>");
		
		addCurrentContent(
				"<p>" +
				message +
				"</p>"
		);
		
		if (loggerEngine != null) {
			Platform.runLater(() -> loggerEngine.loadContent(
					headContent +
					getCurrentContent() +
					footContent
			));
		}
	}
	
	/* THREAD CONTROL */
	
	public void initServer() {
		if (getState() != State.STOPPED) {
			setState(State.INITIALIZED);
			bt_start.setDisable(false);
			bt_pause_resume.setDisable(true);
			bt_pause_resume.setText("Pause");
			bt_stop.setDisable(true);
		}
	}
	
	@SuppressWarnings("Duplicates")
	public void startServer() {
		if (getState() != State.STOPPED) {
			setState(State.STARTED);
			bt_start.setDisable(true);
			bt_pause_resume.setDisable(false);
			bt_pause_resume.setText("Pause");
			bt_stop.setDisable(false);
			log("Server ready to process!");
		}
	}
	
	public void pauseServer() {
		if (getState() != State.INITIALIZED && getState() != State.STOPPED) {
			setState(State.PAUSED);
			bt_start.setDisable(true);
			bt_pause_resume.setDisable(false);
			bt_pause_resume.setText("Resume");
			bt_stop.setDisable(false);
			log("Server is paused");
		}
	}
	
	@SuppressWarnings("Duplicates")
	public void resumeServer() {
		if (getState() != State.INITIALIZED && getState() != State.STOPPED) {
			setState(State.STARTED);
			bt_start.setDisable(true);
			bt_pause_resume.setDisable(false);
			bt_pause_resume.setText("Pause");
			bt_stop.setDisable(false);
			log("Server is ready!");
		}
	}
	
	public void stopServer() {
		setState(State.STOPPED);
		bt_start.setDisable(true);
		bt_stop.setDisable(true);
		bt_pause_resume.setDisable(true);
		log("Server is stopping...");
		Platform.exit();
	}
	
	/* GETTERS & SETTERS */
	
	protected synchronized State getState() {
		return state;
	}
	
	public synchronized void setState(@NotNull State state) {
		if (state == null)
			throw new NullPointerException();
		
		this.state = state;
	}
	
	public synchronized String getCurrentContent() {
		return currentContent;
	}
	
	public synchronized void setCurrentContent(String currentContent) {
		this.currentContent = currentContent;
	}
	
	public synchronized void addCurrentContent(String currentContent) {
		setCurrentContent(getCurrentContent() + currentContent);
	}
}
