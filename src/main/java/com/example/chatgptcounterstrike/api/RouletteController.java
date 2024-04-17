package com.example.chatgptcounterstrike.api;

import com.example.chatgptcounterstrike.dto.MyResponse;
import com.example.chatgptcounterstrike.service.OpenAiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roulette")
@CrossOrigin(origins = "*")
public class RouletteController {

    private final OpenAiService service;

    /**
     * This contains the message to the ChatGPT API, telling the AI how to should act in regard to the requests it gets.
     */
    final static String SYSTEM_MESSAGE = "You are a coach for a Counter Strike 2 Team."+
            " The user should provide you with a question about Counter Strike 2, and you should provide a detailed answer."+
            " The user can also ask for you to provide them with a strategy on a given map in the game."+
            " These are the only maps in the current map pool of Counter Strike 2: Mirage, Overpass, Ancient, Anubis, Vertigo, Inferno & Nuke."+
            " If the user asks a question about Counter Strike: Global Offensive, you should tell the user that Counter Strike: Global Offensive does not exists anymore.";

    /**
     * The controller called from the browser client.
     * @param service
     */
    public RouletteController(OpenAiService service) {
        this.service = service;
    }

    /**
     * Handles the request from the browser client.
     * @param about contains the input that ChatGPT uses to provide information about Counter-Strike.
     * @return the response from ChatGPT.
     */
    @GetMapping
    public MyResponse getRoulette(@RequestParam String about) {

        return service.makeRequest(about,SYSTEM_MESSAGE);
    }

}
