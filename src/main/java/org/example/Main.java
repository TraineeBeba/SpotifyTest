package org.example;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Recommendations;
import se.michaelthelin.spotify.model_objects.specification.Track;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.requests.data.browse.GetRecommendationsRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class Main {
    private static final String accessToken;
    private static KieServices kieServices = KieServices.Factory.get();
    private static KieSession kieSession = getKieSession();

    static {
        try {
            accessToken = SpotifyToken.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setAccessToken(accessToken)
            .build();
    private static final GetRecommendationsRequest getRecommendationsRequest = spotifyApi.getRecommendations()
          .limit(10)
//          .market(CountryCode.SE)
//          .max_popularity(50)
//          .min_popularity(10)
          .seed_artists("0LcJLqbBmaGUft1e9Mm8HV")
//          .seed_genres("electro")
//          .seed_tracks("01iyCAUm8EvOFqVWYJ3dVX")
//          .target_popularity(20)
            .build();

    public static void getRecommendations_Sync() {
        try {
            final Recommendations recommendations = getRecommendationsRequest.execute();

            for (Track track : recommendations.getTracks()) {
                for (ArtistSimplified artist : track.getArtists()) {
                    System.out.print(artist.getName() + ", ");
                }

                System.out.println("\n: " + track.getName());
            }

            System.out.println("Length: " + recommendations.getTracks().length);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void getRecommendations_Async() {
        try {
            final CompletableFuture<Recommendations> recommendationsFuture = getRecommendationsRequest.executeAsync();

            // Thread free to do other tasks...

            // Example Only. Never block in production code.
            final Recommendations recommendations = recommendationsFuture.join();

            System.out.println("Length: " + recommendations.getTracks().length);
        } catch (CompletionException e) {
            System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            System.out.println("Async operation cancelled.");
        }
    }

    private static KieFileSystem getKieFileSystem() {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        List<String> rules = Arrays.asList("org/example/test");
        for (String rule : rules) {
            kieFileSystem.write(ResourceFactory.newClassPathResource(rule));
        }
        return kieFileSystem;
    }

    public static KieSession getKieSession() {
        KieBuilder kb = kieServices.newKieBuilder(getKieFileSystem());
        kb.buildAll();

        KieRepository kieRepository = kieServices.getRepository();
        ReleaseId krDefaultReleaseId = kieRepository.getDefaultReleaseId();
        KieContainer kieContainer = kieServices.newKieContainer(krDefaultReleaseId);

        return kieContainer.newKieSession();
    }

    public static void main(String[] args) {

        getRecommendations_Sync();
        DroolTest test = new DroolTest();
        test.setAnswer(0);
        test.setAsk(10);
        kieSession.insert(test);
        kieSession.fireAllRules();
        System.out.println(test.answer);

//        getRecommendations_Async();
    }
}