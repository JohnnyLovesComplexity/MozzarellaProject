package org.jlc.arar.mozzarella.waterfox;
import org.jlc.arar.mozzarella.FileGenerator;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Waterfox{

    static byte data [];
    static String path;
    static boolean quitter = false;

    private String adress = "127.0.0.1";
    private String im = "rose.jpg";
    private String fic = "avis-recette.txt";


    public Waterfox() {
        System.out.println("Connexion...");
        try{
            Socket con_serv = new Socket(InetAddress.getByName(adress),80);
            try {

                System.out.println("Connected");

                //flux de sortie
                InputStream inp = con_serv.getInputStream();

                //flux d'entrée
                OutputStream op = con_serv.getOutputStream();


                //BufferedReader in = new BufferedReader(new InputStreamReader(inp));
                DataInputStream indata = new DataInputStream(con_serv.getInputStream());
                PrintWriter printWriter = new PrintWriter(op);

                Scanner sc = new Scanner(System.in);

                printWelcome();

                System.out.println("Vous souhaitez :\n" +
                        "1 : Acceder a un fichier du serveur\n" +
                        "2 : Acceder à une image du serveur\n" +
                        "3 : Envoyer un fichier au serveur\n" +
                        "4 : Quitter l'échange\n");
                int i = sc.nextInt();
                String request = "";
                System.out.println("Saisissez le fichier que vous voulez recevoir/envoyer.");
                String name = sc.next();

                switch (i) {
                    case 1:
                        fic = name;
                        path = "/Pizza/site/" + fic;
                        getRequest(printWriter, con_serv, path);
                        break;
                    case 2:
                        im = name;
                        path = "/Pizza/site/images/"+im;
                        getRequest(printWriter, con_serv, path);
                        break;
                    case 3 :
                        fic = name;
                        putRequest(printWriter,con_serv,"MozzarellaWaterfox/file/"+fic);
                        break;
                    case 4:
                        quitter = true;
                        con_serv.close();
                        break;
                }
                if(!quitter){
                    StringBuilder answer = new StringBuilder();
                    String temp;
                    String length = "";
                    String file;
                    String content;
                    String line = indata.readLine();
                    if(line != null){
                        String stat_line[] = line.split(" ");

                        if (stat_line.length > 1) {
                            if (stat_line[1].equals("200")) {
                                answer = new StringBuilder(line + "\n");
                                while (!(temp = indata.readLine()).equals("")) {

                                    if (temp.contains("Length"))
                                        if (temp.split(": ").length > 1)
                                            length = (temp.split(": "))[1];

                                    answer.append(temp).append("\n");
                                }
                                System.out.println("Received: \n" + answer);
                                data = new byte[Integer.parseInt(length)];
                                indata.readFully(data);

                                String nomFichierSplit = "recu.txt";
                                System.out.println(request);
                                if (request.split("/").length > 2)
                                    nomFichierSplit = request.split("/")[2].split(" ")[0];
                                if(i==1)
                                    nomFichierSplit = fic;
                                else if(i==2)
                                    nomFichierSplit = im;
                                FileGenerator.generateFile(new String(data),"MozzarellaWaterfox/receveided/" + nomFichierSplit);

                                System.out.println("Sucess.");
                            }else if(stat_line[1].equals("201")) {
                                answer = new StringBuilder(line + "\n");
                                while (!(temp = indata.readLine()).equals("")) {
                                    answer.append(temp).append("\n");
                                }
                                System.out.println("Received: \n" + answer);

                                System.out.println("Sucess.");
                            }else {
                                answer = new StringBuilder(line + "\n");
                                while (!(temp = indata.readLine()).equals("")) {
                                    answer.append(temp).append("\n");
                                }
                                System.out.println("Received: \n" + answer);
                            }
                        }
                    }
                }

            }catch (IOException e) {
                e.printStackTrace();
            }

        }catch(java.net.ConnectException ce){
            System.out.println("La connexion a échouée.\n");
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

    private static void putRequest(PrintWriter request, Socket socket, String path){

		/*System.out.println("Name of the file to send :");
		Scanner sc2 = new Scanner(System.in);
		String name = sc2.nextLine();*/
        try{
            File file = new File(path);
			/*DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			byte[] b = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(b);
			fis.close();
			dos.write(("PUT " + imgPath.replace("MozzarellaWaterfox/file/","") + " HTTP/1.1\r\n").getBytes());
			dos.write("Content-length: ".getBytes());
			dos.writeInt((int) file.length());
			dos.write("\r\n".getBytes());
			dos.flush();
			dos.write(b, 0, b.length);
			//dos.write("\r\n\r\n".getBytes());
			dos.flush();*/
            PrintStream out_data = new PrintStream(socket.getOutputStream());
            send(out_data,file);

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

    private static boolean send(PrintStream out_data, File f) {
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

            out_data.print("PUT "  + f.getName() + " HTTP/1.1\r\n");
            out_data.print("Content-Length: " + data.length + "\r\n");
            out_data.print("\r\n");
            out_data.write(data, 0, data.length);
            out_data.print("\r\n");


            out_data.flush();

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

}
