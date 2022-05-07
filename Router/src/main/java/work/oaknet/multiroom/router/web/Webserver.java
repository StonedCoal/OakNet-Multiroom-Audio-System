package work.oaknet.multiroom.router.web;

import manifold.ext.props.rt.api.var;
import spark.Spark;

public class Webserver {

    @var static Webserver instance;

    @var Websocket socket;

    public Websocket getSocket() {
        return socket;
    }

    public Webserver(){
        instance = this;

        socket = new Websocket();
        Spark.staticFiles.location("/static");
        Spark.port(8080);

        // Websockets
        Spark.webSocket("/ws", socket);

        // HTTP GET stuff
        Spark.redirect.get("", "/");
        Spark.redirect.get("/", "/index.html");
    }
}
