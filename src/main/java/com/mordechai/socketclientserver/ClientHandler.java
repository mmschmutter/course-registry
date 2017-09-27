package com.mordechai.socketclientserver;

import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

class ClientHandler extends Thread {
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;
    private JsonParser parser;
    private File dataFile;
    private BufferedReader reader;
    private JsonObject jsonObject;

    ClientHandler(Socket socket, DataInputStream inputStream, DataOutputStream outputStream, File dataFile) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.parser = new JsonParser();
        this.dataFile = dataFile;
        try {
            this.reader = new BufferedReader(new FileReader(this.dataFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.jsonObject = parser.parse(reader).getAsJsonObject();
    }

    @Override
    public void run() {
        String[] request;
        while (true) {
            try {
                // ask client for request
                outputStream.writeUTF("Please enter your request or type 'exit' to terminate connection:");
                // receive answer from client
                request = inputStream.readUTF().split("\\s+");
                // exit request
                if (request[0].toLowerCase().equals("exit")) {
                    // close socket
                    socket.close();
                    System.out.println("Connection to client terminated at port " + this.socket.getPort() + "\n");
                    break;
                }
                //get request
                else if (request[0].toLowerCase().equals("get")) {
                    // check for valid get parameters
                    if (request.length == 2 && request[1].length() == 3) {
                        String result = "CRN   Course Name\n";
                        JsonObject major = jsonObject.getAsJsonObject(request[1].toUpperCase());
                        // check if major code exists in JSON file
                        if (major != null) {
                            for (Map.Entry<String, JsonElement> crn : major.entrySet()) {
                                result += crn.getKey() + " " + crn.getValue().getAsJsonObject().get("Name").getAsString() + "\n";
                            }
                        } else {
                            result = "No such major code exists\n";
                        }
                        outputStream.writeUTF(result);
                    } else {
                        outputStream.writeUTF("Invalid course lookup syntax\n");
                    }
                }
                // add request
                else if (request[0].toLowerCase().equals("add")) {
                    // check for valid add parameters
                    if (request.length > 3 && request[1].length() == 3 && isInt(request[2])) {
                        // check if CRN already exists
                        boolean exists = false;
                        for (String major : jsonObject.keySet()) {
                            if (jsonObject.getAsJsonObject(major).has(request[2])) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            outputStream.writeUTF("Course with that CRN already exists");
                        } else {
                            backupCurrent();
                            // build course name
                            String courseName = "";
                            for (int i = 3; i < request.length; i++) {
                                courseName += request[i] + " ";
                            }
                            courseName = courseName.trim();
                            String nameJson = "{\"Name\":\"" + courseName + "\"}";
                            // check if major code already exists
                            if (jsonObject.has(request[1].toUpperCase())) {
                                JsonElement jsonName = parser.parse(nameJson);
                                // add CRN and course name
                                jsonObject.getAsJsonObject(request[1].toUpperCase()).add(request[2], jsonName);
                            } else {
                                String crnJson = "{\"" + request[2] + "\":" + nameJson + "}";
                                JsonElement jsonCrn = parser.parse(crnJson);
                                // add major code, CRN, and course name
                                jsonObject.add(request[1].toUpperCase(), jsonCrn);
                            }
                            writeToDataFile();
                            outputStream.writeUTF("Course entry added\n");
                        }
                    } else {
                        outputStream.writeUTF("Invalid course entry syntax\n");
                    }
                }
                // delete request
                else if (request[0].toLowerCase().equals("delete")) {
                    // check for valid delete parameters
                    if (request.length == 2 && isInt(request[1])) {
                        String majorCode = null;
                        // check which major code contains the CRN
                        for (String major : jsonObject.keySet()) {
                            if (jsonObject.getAsJsonObject(major).has(request[1])) {
                                majorCode = major;
                                break;
                            }
                        }
                        // check if CRN exists
                        if (majorCode != null) {
                            backupCurrent();
                            // delete course
                            jsonObject.getAsJsonObject(majorCode).remove(request[1]);
                            writeToDataFile();
                            outputStream.writeUTF("Course entry deleted\n");
                        } else {
                            outputStream.writeUTF("No such CRN exists\n");
                        }
                    } else {
                        outputStream.writeUTF("Invalid course deletion syntax\n");
                    }
                }
                // any other request
                else {
                    outputStream.writeUTF("Invalid request\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // close streams
            this.inputStream.close();
            this.outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // backup current version of JSON file with timestamp
    private void backupCurrent() throws IOException {
        String originalName = dataFile.getName().substring(0, dataFile.getName().lastIndexOf("."));
        String timeStamp = new SimpleDateFormat("[yyyy-MM-dd HH.mm.ss]").format(new Date());
        File backupFile = new File(dataFile.getParent() + "/" + originalName + " " + timeStamp + ".json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(jsonObject, writer);
        writer.close();
    }

    private void writeToDataFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(jsonObject, writer);
        writer.close();
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}