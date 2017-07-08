package de.digineo.wifitools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sends collected data as batches to the server
 */
public class ResultSender implements Runnable {

    private static final int resultsPerRequest = 500;
    private static final String apiEndpoint = "http://localhost:3000/";

    ConcurrentLinkedQueue<JSONObject> queue = new ConcurrentLinkedQueue<JSONObject>();

    void enqueue(JSONObject obj){
        queue.add(obj);
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Vector<JSONObject> currentBatch = new Vector<JSONObject>();

        while(currentBatch.size() < resultsPerRequest) {
            JSONObject obj = queue.poll();
            if (obj == null) {
                break;
            }
            currentBatch.add(obj);
        }

        if (currentBatch.isEmpty()){
            // nothing to be sent
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("results", new JSONArray(currentBatch));
        } catch(JSONException ex){
            throw new RuntimeException(ex);
        }

        try {
            URL url = new URL(apiEndpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(body.toString());
            wr.flush();

            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                System.out.println("" + sb.toString());
            } else {
                System.out.println(con.getResponseMessage());
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
