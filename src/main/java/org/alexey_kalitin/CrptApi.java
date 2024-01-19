package org.alexey_kalitin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final Lock lock = new ReentrantLock();
    private final long interval;
    private final int requestLimit;
    private int requestCount;
    private long lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.interval = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
    }

    public void createGoodsIntroductionDocument(Object document, String signature) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();

            // Сброс счетчика, если прошло достаточное время
            if (currentTime - lastRequestTime >= interval) {
                requestCount = 0;
                lastRequestTime = currentTime;
            }

            // Ожидание, если превышен лимит запросов
            while (requestCount >= requestLimit) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                currentTime = System.currentTimeMillis();
                if (currentTime - lastRequestTime >= interval) {
                    requestCount = 0;
                    lastRequestTime = currentTime;
                }
            }
            try {

                HttpURLConnection conn = getHttpURLConnection();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String responseLine;
                StringBuilder response = new StringBuilder();

                while ((responseLine = reader.readLine()) != null) {
                    response.append(responseLine);
                }
                reader.close();
                System.out.println(response);

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Увеличение счетчика запросов
            requestCount++;
            lastRequestTime = currentTime;
        } finally {
            lock.unlock();
        }
    }

    private static HttpURLConnection getHttpURLConnection() throws IOException {
        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = " {\"description\": { \"participantInn\": \"string\" }," +
                "\"doc_id\": \"string\", \"doc_status\": \"string\", " +
                "\"doc_type\": \"LP_INTRODUCE_GOODS\", 109 \"importRequest\": true, " +
                "\"owner_inn\": \"string\", \"participant_inn\": \"string\", " +
                "\"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", " +
                "\"production_type\": \"string\", " +
                "\"products\": [ { \"certificate_document\": \"string\", " +
                "\"certificate_document_date\": \"2020-01-23\", " +
                "\"certificate_document_number\": \"string\", " +
                "\"owner_inn\": \"string\", \"producer_inn\": \"string\", " +
                "\"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", " +
                "\"uit_code\": \"string\", \"uitu_code\": \"string\" } ], " +
                "\"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}\n";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }
}

