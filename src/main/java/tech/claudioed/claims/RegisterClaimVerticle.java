package tech.claudioed.claims;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;

/**
 * @author claudioed on 2019-03-28.
 * Project claims
 */
public class RegisterClaimVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterClaimVerticle.class);

  @Override
  @SneakyThrows
  public void start() {
    final String natsHost = System.getenv("NATS_HOST");
    final String natsUser = System.getenv("NATS_USER");
    final String natsPass = System.getenv("NATS_PASS");
    final String mongoHost = System.getenv("MONGO_HOST");

    LOGGER.info(" NATS HOST " + natsHost);
    LOGGER.info(" MONGO HOST " + mongoHost);
    JsonObject mongoConfig = new JsonObject()
      .put("connection_string", mongoHost)
      .put("db_name", "CLAIMS");
    Connection natsConnection = Nats.connect(new Options.Builder().userInfo(natsUser,natsPass).server(natsHost).build());
    final MongoClient mongoClient = MongoClient.createShared(this.vertx, mongoConfig);
    natsConnection.createDispatcher((message ) -> {
      LOGGER.info(" Receiving message {} ",new String(message.getData(), StandardCharsets.UTF_8));
      final ClaimRequest claimRequest = Json
        .decodeValue(new String(message.getData(), StandardCharsets.UTF_8), ClaimRequest.class);
      final Claim claim = Claim.from(claimRequest);
      mongoClient.insert("claims",claim.json(),res ->{
        if(res.succeeded()){
          LOGGER.info("claims registered successfully !!!");
        }else{
          LOGGER.error("Error to insert in database !!");
        }
      });
    });
    natsConnection.subscribe("request-claims");
  }

}