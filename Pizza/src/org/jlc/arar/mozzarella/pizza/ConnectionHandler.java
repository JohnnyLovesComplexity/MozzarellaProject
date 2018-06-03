package org.jlc.arar.mozzarella.pizza;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jlc.arar.mozzarella.FileGenerator;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Function;

/**
 * Class to manage a connection for the server.
 * This class has been made with the help of Stack Overflow community
 * Source: https://stackoverflow.com/questions/28629669/java-tcp-simple-webserver-problems-with-response-codes-homework?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
 */
public class ConnectionHandler implements Runnable {

	public enum Code {
		CONTINUE(100, "HTTP/1.0 100 Continue"),
		OK(200, "HTTP/1.0 200 OK"),
		CREATED(200, "HTTP/1.0 200 Created"),
		NOT_MODIFIED(304, "HTTP/1.0 304 Not Modified"),
		BAD_REQUEST(400, "HTTP/1.0 400 Bad Request"),
		FORBIDDEN(403, "HTTP/1.0 400 Forbidden"),
		NOT_FOUND(404, "HTTP/1.0 404 Not Found"),
		INTERNAL_SERVER_ERROR(500, "HTTP/1.0 500 Internal Server Error");

		private int code;
		private String message;

		Code(int code, String message) {
			setCode(code);
			setMessage(message);
		}

		/* GETTERS & SETTERS */

		@Contract(pure = true)
		public int getCode() {
			return code;
		}

		private void setCode(int code) {
			this.code = code;
		}

		@Contract(pure = true)
		@NotNull
		public String getMessage() {
			return message;
		}

		private void setMessage(@NotNull String message) {
			this.message = message;
		}
	}

	private Socket so_client;
	private DataInputStream in_data;
	private PrintStream out_data;

	@Nullable
	private Function<String, Void> onLog;

	public ConnectionHandler(@NotNull Socket so_client, @Nullable Function<String, Void> onLog) throws IOException {
		setClient(so_client);

		in_data = new DataInputStream(so_client.getInputStream());
		out_data = new PrintStream(so_client.getOutputStream());
		setOnLog(onLog);
		
		tryLog("New connection: " + so_client.getInetAddress().getHostName() + " port " + so_client.getPort());
	}
	public ConnectionHandler(@NotNull Socket so_client) throws IOException {
		this(so_client, null);
	}

	@Override
	public void run() {
		if (in_data == null)
			throw new NullPointerException();

		boolean keepRunning = true;
		String line = "";
		try {
			line = in_data.readLine();
			if(line != null){
				String[] parts = line.split(" ");
				if (parts[0].equals("GET")) {
					// Get the file wanted by the client
					String url = parts[1];

					if (url.startsWith("/"))
						url = "." + url;

					tryLog("The clients wants \"" + url + "\"");

					File f = new File(url);

					if (!f.exists())
						sendError(Code.NOT_FOUND, f.toString());
					else {
						send(f);
						so_client.close();
						tryLog("\"" + url + "\" sent!");
						keepRunning = false;
					}
				}
				else if (parts[0].equals("PUT")) {
					String url = parts[1];

					tryLog("The clients wants to add \"" + url + "\"");

					File file = new File("Pizza/site/"+url);
					FileOutputStream fos = new FileOutputStream(file);
					line = in_data.readLine();
					int length = Integer.parseInt(line.replace("Content-length: ","").replace("\r\n",""));
					byte[] b = new byte[length];
					in_data.readFully(b);
					String received = new String(b);
					String content = "";
					boolean found = false;
					int i = 0;
					while(i<received.length()) {
						if(found)
							fos.write(received.charAt(i));
						if(content.contains("Content-length"))
							found = true;
						i++;
					}
					sendError(Code.CREATED);
					//fos.write(b, 0 , b.length);
				}
			}
			} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/* CONNECTION HANDLER METHODS */

	private boolean send(@NotNull File f) {
		if (f == null)
			throw new NullPointerException();

		FileInputStream in = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] raw = new byte[4096];
		int size;

		try {
			in = new FileInputStream(f);

			while ((size = in.read(raw)) >= 0)
				baos.write(raw, 0, size);

			baos.flush();
			baos.close();

			byte[] data = baos.toByteArray();

			out_data.print(Code.OK.getMessage());
			out_data.print("");

			out_data.print("Server: Pizza Web Server");
            out_data.print("Content-Type: " + MimeTypeManager.parse(f) + "\r\n"); // TODO: get the mimetype
            out_data.print("Content-Length: " + data.length + "\r\n");
            out_data.print("\r\n");
            out_data.write(data, 0, data.length);
			out_data.print("\r\n");


			out_data.flush();
			out_data.close();

			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void sendError(@NotNull Code code, @Nullable String filename) {
		if (code == null)
			throw new NullPointerException();
		
		String log = "Error " + code.getCode() + (filename != null ? ": \"" + filename + "\"" : "") + ".";

		if (out_data != null) {
			out_data.print(code.getMessage());
			out_data.print("Server: Pizza Web Server");
			out_data.print("Content-Type: text/html\r\n");
			out_data.print("Content-Length: 0\r\n");
			out_data.print("\r\n");
			out_data.print("\r\n");
			out_data.flush();
			out_data.close();
			
			log += " Error sent.";
		}
		tryLog(log);
	}
	public void sendError(@NotNull Code code) {
		sendError(code, null);
	}

	/* GETTERS & SETTERS */

	@Contract(pure = true)
	public Socket getClient() {
		return so_client;
	}

	public void setClient(Socket so_client) {
		this.so_client = so_client;
	}

	@Nullable
	@Contract(pure = true)
	public Function<String, Void> getOnLog() {
		return onLog;
	}

	public void setOnLog(@Nullable Function<String, Void> onLog) {
		this.onLog = onLog;
	}

	public void tryLog(String message) {
		if (getOnLog() != null)
			getOnLog().apply(message);
	}
}
