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
import org.jlc.arar.mozzarella.FileGenerator;

import java.io.*;
import java.lang.reflect.MalformedParametersException;
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
			// Try to open the port 80, but as it is usually occupied, try to connect to 8080 if failed.
			try {
				soc = new ServerSocket(80);
				log("Server open on port 80");
			} catch (SocketException ex) {
				soc = new ServerSocket(8080);
				log("Server open on port 8080");
			}
			
			// Wait for "start"
			while (getState() == State.INITIALIZED);
			
			// Main loop
			while (getState().isRunning()) {
				
				// Pause
				while (getState() == State.PAUSED) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				
				// Accept a connection (it will block this thread until a connection arrived)
				Socket com_cli = soc.accept();
				System.out.println("Connected");
				StringBuilder message = new StringBuilder();
				BufferedReader in = new BufferedReader(new InputStreamReader(com_cli.getInputStream()));

				String line = "";
				line = in.readLine();
				while(line!=null && !line.equals("")){
                    message.append(line); // Tester "\r\n" comme valeur sentinnelle
                    line = in.readLine();
                }

				// Manage the client by creating a thread for this one (see createThread()).
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
	
	/**
	 * Create a response for the client according to the value of {@code message}.
	 * @param message The message that the client gave
	 * @return An answer for the client.
	 */
	private static String constructResponse(String message, Socket sock) throws MalformedParametersException {
		String response = "";
		String url;
		
		StringBuilder beginning = new StringBuilder();
		int i = 0;
		while (i < message.length() && !beginning.toString().endsWith("HTTP/1.1")) {
			beginning.append(message.charAt(i));
			i++;
		}
		
		String[] parts = beginning.toString().split("\r\n");
		
		if (parts.length == 0)
			throw new MalformedParametersException("\"" + message + "\" is not a valid message from the client.");

		url = parts[0].replace("GET ","")
				.replace("PUT ", "")
				.replace("HTTP/1.1","")
				.replace(" ","");
		System.out.println("Pizza.constructResponse> " + url);

		// If the message is a PUT
		if(message.contains("PUT")){
			String[] receveided = message.split("\r\n");
			boolean text = false;
			StringBuilder sended = new StringBuilder();
			for (String line:receveided) {
				if(line.toLowerCase().contains("content-length"))
					text = true;
				else if(text){
					sended.append(line);
				}
			}
			FileGenerator.generateFile(sended.toString(),"Pizza/site" + url);
			response = constructResponseHeader(201,0);
		}
		// If the message is a GET
		else if(message.contains("GET")){
			// If the file does not exist...
			if(!(url.equals("/avis-recette.txt")
					|| url.equals("/3-fromages.jpg"))){
				System.out.println("Pizza.constructResponse> File does not exist...");
				response = constructResponseHeader(404,0);
			}else if(url.equals("/avis-recette.txt")){
				try {
					String content = FileGenerator.readContent("Pizza/site/avis-recette.txt");
					response = constructResponseHeader(200,1);
					response += content + "\r\n";
				} catch (IOException e) {
					System.out.println("Chemin spécifié introuvable\n");
					response = constructResponseHeader(404,0);
				}
			}else if(url.equals("/3-fromages.jpg"))
			{
				try {
					System.out.println("Sending> 3-fromages.jpg");
					response = constructResponseHeader(200,2);
					String content = FileGenerator.readContent("Pizza/site" + url);
					sendImage(sock, message, url);
					response += content + "\r\n";
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			System.out.println("The request can't be interpreted");
			response = constructResponseHeader(404,0);
		}

		return response;
	}


	// Construct Response Header
	private static String constructResponseHeader(int responseCode, int dem) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if (responseCode == 200) {
			if(dem == 1){
                stringBuilder.append("HTTP/1.1 200 OK\r\n");
                stringBuilder.append("Date:" + getCurrentTime() + "\r\n");
                stringBuilder.append("Server:wwww.pizza.com\r\n");
                stringBuilder.append("Content-Type: text/html\r\n");
                stringBuilder.append("\r\n\r\n");
            }else{
                stringBuilder.append("HTTP/1.1 200 OK\r\n");
                stringBuilder.append("Date:" + getCurrentTime() + "\r\n");
                stringBuilder.append("Server:wwww.pizza.com\r\n");
                stringBuilder.append("Accept-Ranges: bytes\r\n");
                stringBuilder.append("Content-Type: image/jpg\r\n");
                stringBuilder.append("\r\n\r\n");
            }


		} else if (responseCode == 404) {
			
			stringBuilder.append("HTTP/1.1 404 Not Found\r\n");
			stringBuilder.append("Date:" + getCurrentTime() + "\r\n");
			stringBuilder.append("Server:wwww.pizza.com\r\n");
			stringBuilder.append("\r\n\r\n");
		} else if (responseCode == 304) {
			stringBuilder.append("HTTP/1.1 304 Not Modified\r\n");
			stringBuilder.append("Date:" + getCurrentTime() + "\r\n");
			stringBuilder.append("Server:wwww.pizza.com\r\n");
			stringBuilder.append("\r\n\r\n");
		}else if(responseCode == 201){
			stringBuilder.append("HTTP/1.1 201 Created\r\n");
			stringBuilder.append("Date:" + getCurrentTime() + "\r\n");
			stringBuilder.append("Server:wwww.pizza.com\r\n");
			stringBuilder.append("Content-Location: /new.html\n\r\n");
			stringBuilder.append("\r\n\r\n");
		}
		return stringBuilder.toString();
	}

	private static void sendImage(Socket sock,String message, String path){
		try {
			String content = FileGenerator.readContent("Pizza/site" + path);
			BufferedWriter response = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			response.write(constructResponseHeader(200,0));
			response.write(content);
			response.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}


		/*DataOutputStream bw = null;
		try {
			byte[] content = FileGenerator.readContentImage("Pizza/site" + path);
			bw = new DataOutputStream(sock.getOutputStream());
			bw.writeBytes("HTTP/1.1 200 OK\r\n");
			bw.writeBytes("Date:" + getCurrentTime() + "\r\n");
			bw.writeBytes("Server:wwww.pizza.com\r\n");
			bw.writeBytes("Accept-Ranges: bytes\r\n");
			bw.writeBytes("Content-Type: image/jpg\r\n");
			bw.write(content);
			bw.writeBytes("\r\n");
			bw.writeBytes("\r\n\r\n");
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

	}

	private static String getCurrentTime() {
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
	
	/**
	 * Create a thread that will manage an answer for the client
	 * @param com_cli The socket of the client
	 * @param message The message that the client gave
	 * @return Return a thread that contain a runnable to manage the client. You have to start it.
	 */
	protected Thread createThread(@NotNull Socket com_cli, @NotNull final String message) {
		return new Thread(() -> {
			log("New connection: " + com_cli.getInetAddress().getHostName() + " port " + com_cli.getPort());
			
			log("data received: \"" + message + "\".");
			
			// Sending an answer
			String answer = "";
			boolean stopServer = false;
			try{
				BufferedWriter response = new BufferedWriter(new OutputStreamWriter(com_cli.getOutputStream()));
				answer = constructResponse(message, com_cli);
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
