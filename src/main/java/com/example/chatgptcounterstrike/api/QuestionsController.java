package com.example.chatgptcounterstrike.api;


import com.example.chatgptcounterstrike.dto.MyResponse;
import com.example.chatgptcounterstrike.service.OpenAiService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/questions")
@CrossOrigin(origins = "*")
public class QuestionsController {

    @Value("${app.bucket_capacity}")
    private int BUCKET_CAPACITY;

    @Value("${app.refill_amount}")
    private int REFILL_AMOUNT;

    @Value("${app.refill_time}")
    private int REFILL_TIME;

    private final OpenAiService service;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * This contains the message to the ChatGPT API, telling the AI how to should act in regard to the requests it gets.
     */
    final static String SYSTEM_MESSAGE = "You are a coach for a Counter Strike 2 Team."+
            " The user should provide you with a question about Counter Strike 2, and you should provide a detailed answer."+
            " These are the only maps in the current map pool of Counter Strike 2: Mirage, Overpass, Ancient, Anubis, Vertigo, Inferno & Nuke.";

    public QuestionsController(OpenAiService service) {
        this.service = service;
    }

    /**
     * Creates the bucket for handling IP-rate limitations.
     * @return bucket
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(BUCKET_CAPACITY, Refill.greedy(REFILL_AMOUNT, Duration.ofMinutes(REFILL_TIME)));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Returns an existing bucket via ket or creates a new one.
     * @param key the IP address
     * @return bucket
     */
    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Handles the request from the browser.
     * @param about contains the input that ChatGPT uses to provide information about Counter-Strike.
     * @param request the HTTP request used
     * @return the response from ChatGPT.
     */
    @GetMapping
    public MyResponse getQuestion(@RequestParam String about, HttpServletRequest request) {

        // Get the IP address of the client
        String ip = request.getRemoteAddr();
        // Get or create the bucket for the given IP/key
        Bucket bucket = getBucket(ip);
        // Does the request adhere to the IP-rate limitations?
        if (!bucket.tryConsume(1)) {
            // If not, tell the client "Too many requests"
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later");
        }

        return service.makeRequest(about,SYSTEM_MESSAGE);
    }
}
