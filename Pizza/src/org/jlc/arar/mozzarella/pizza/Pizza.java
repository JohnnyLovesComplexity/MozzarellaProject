package org.jlc.arar.mozzarella.pizza;

import fr.berger.enhancedlist.Couple;
import fr.berger.enhancedlist.lexicon.Lexicon;
import fr.berger.enhancedlist.lexicon.LexiconBuilder;
import org.jlc.arar.mozzarella.Connection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jlc.arar.mozzarella.ConnectionData;

import javax.naming.OperationNotSupportedException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
			Platform.exit();
		});
		
		bt_clear.setOnMouseClicked(event -> {
			currentContent = "";
			loggerEngine.loadContent("");
		});
		
		tb_buttons.getItems().addAll(bt_start, bt_pause_resume, bt_stop, bt_clear);
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
			Connection connection = new Connection(InetAddress.getByName(ConnectionData.serverIP), ConnectionData.serverPort);
			
			Couple<String, DatagramPacket> received;
			
			while (getState() == State.INITIALIZED);
			
			while (getState().isRunning()) {
				while (getState() == State.PAUSED);
				
				received = null;
				
				try {
					received = connection.receive(ConnectionData.serverPort);
				} catch (Exception ignored) { }
				
				if (received != null) {
					Thread t = createThread(new Connection(connection), received);
					t.start();
					connections.add(t);
				}
				
				for (int i = 0; i < connections.size(); i++) {
					if (connections.get(i) == null || !connections.get(i).isAlive() || connections.get(i).isInterrupted()) {
						connections.remove(i);
						i--;
					}
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		try {
			Pizza.this.stop();
		} catch (Exception ignored) { }
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
		
		stopServer();
		if (th_route != null)
			th_route.interrupt();
		
		for (Thread connection : connections)
			connection.interrupt();
		
		log("Shutting down...");
		log("Good bye.");
		Platform.exit();
		Runtime.getRuntime().halt(0);
	}
	
	protected Thread createThread(@NotNull Connection connection, @NotNull Couple<String, DatagramPacket> received) {
		return new Thread(() -> {
			String data = null;
			if (received.getY() != null)
				log("New connection: " + received.getY().getAddress().getHostName() + " port " + received.getY().getPort());
			
			if (received.getX() != null)
				data = received.getX();
			
			if (data != null) {
				log("data received: \"" + data + "\".");
				
				// Sending an answer
				String answer = "";
				boolean stopServer = false;
				try {
					if (data.toLowerCase().equals("time")) {
						SimpleDateFormat sdf = new SimpleDateFormat("hh-MM-ss");
						answer = sdf.format(new Date());
					} else if (data.toLowerCase().equals("exit")) {
						answer = "Shutting down...";
						stopServer = true;
					} else if (data.toLowerCase().equals("hello world!"))
						answer = "Hello back!";
					else
						answer = data;
					
					connection.answer(answer);
					log("Answered \"" + answer + "\"");
				} catch (OperationNotSupportedException ex) {
					ex.printStackTrace();
					log(ex.getMessage());
				}
				finally {
					if (stopServer)
						stopServer();// setRunning(false);
				}
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
