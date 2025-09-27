package api;

import static spark.Spark.*;
import controller.GameFacade;

/**
 * Initializes and configures the embedded web server for the REST API.
 */
public class WebServer {

    /**
     * Starts the web server and defines the API routes.
     *
     * @param gameFacade The GameFacade to be used by the controllers.
     * @param port       The port for the web server to listen on.
     */
    public static void start(GameFacade gameFacade, int port) {
        port(port);
        new ServerSynchronizationController(gameFacade);
    }
}