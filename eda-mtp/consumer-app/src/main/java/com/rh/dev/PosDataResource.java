package com.rh.dev;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.logging.Log;

@Path("/api/pos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)    
public class PosDataResource {

    public static record TransactionData(
        long id, 
        @JsonProperty("datetime")
        String dateTime,
        @JsonProperty("items-count")
        int itemsCount,
        @JsonProperty("total-amount")
        double totalAmount,
        @JsonProperty("operator-id")
        int operatorId,
        @JsonProperty("with-card")
        boolean withCard,
        @JsonProperty("with-cash")
        boolean withCash
    ) {};

    @POST
    @Path("/transaction")
    public Response addTransaction(TransactionData transaction) {
        Log.debug("transaction received: "+transaction);
        return Response.ok().build();
    }

    @POST
    @Path("/transactions")
    public Response addTransactions(List<TransactionData> transactions) {
        Log.debug(transactions.size() + " transaction(s) received: "+transactions);
        return Response.ok().build();
    }

}