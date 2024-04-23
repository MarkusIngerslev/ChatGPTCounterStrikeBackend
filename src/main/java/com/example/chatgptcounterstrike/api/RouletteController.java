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
@RequestMapping("/api/v1/roulette")
@CrossOrigin(origins = "*")
public class RouletteController {

    @Value("${app.bucket_capacity}")
    private int BUCKET_CAPACITY;

    @Value("${app.refill_amount}")
    private int REFILL_AMOUNT;

    @Value("${app.refill_time}")
    private int REFILL_TIME;

    // The service that handles the requests to the ChatGPT API.
    private final OpenAiService service;

    // The buckets that contain the rate limitations for the requests.
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    final static String SYSTEM_MESSAGE = "You are a coach for a Counter Strike 2 Team."+
            " The user will ask you to provide them with a strategy in with they can use."+
            " These are the only maps in the current map pool of Counter Strike 2: Mirage, Overpass, Ancient, Anubis, Vertigo, Inferno & Nuke."+
            " If the user asks a question about Counter Strike: Global Offensive, you should tell the user that Counter Strike: Global Offensive does not exists anymore.";

    public RouletteController(OpenAiService service) {
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

    @GetMapping
    public MyResponse getRoulette(@RequestParam String about, HttpServletRequest request) {

        // Get the IP address of the client
        String ip = request.getRemoteAddr();
        // Get or create the bucket for the given IP/key
        Bucket bucket = getBucket(ip);
        // Does the request adhere to the IP-rate limitations?
        if (!bucket.tryConsume(1)) {
            // If not, tell the client "Too many requests"
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
        }

        return service.makeRequest(about,SYSTEM_MESSAGE);
    }

}
