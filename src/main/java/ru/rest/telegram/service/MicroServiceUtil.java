package ru.rest.telegram.service;

import com.google.gson.Gson;
import ru.rest.telegram.config.BotSettings;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import ru.rest.telegram.model.Ticket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
public class MicroServiceUtil {
    public static ArrayList<Ticket> getExaminationTicketFromMicroservice(String type, int limit) {
        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BotSettings.getDataHost() + "tickets"))
                    .headers("content-type", "application/json")
                    //.POST(HttpRequest.BodyPublishers.noBody())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Type listType = new TypeToken<ArrayList<Ticket>>(){}.getType();
            return new Gson().fromJson(response.body(), listType);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
