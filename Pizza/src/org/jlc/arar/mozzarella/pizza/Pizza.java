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
import java.util.function.Function;

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
			} catch (SocketException ex) {
				soc = new ServerSocket(8080);
			}
			log("Server open on port " + soc.getLocalPort());
			
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

				Thread t = /*createThread(com_cli, message.toString())*/new Thread(new ConnectionHandler(com_cli, s -> {
					log(s);
					return null;
				}));
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
				//answer = constructResponse(message, com_cli);
				response.write(answer);
				log("Answered \"" + answer +"\"");
				response.flush();
			}catch(IOException ex){
				ex.printStackTrace();
			}

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
