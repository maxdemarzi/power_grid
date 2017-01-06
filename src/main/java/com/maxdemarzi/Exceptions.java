package com.maxdemarzi;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Exceptions extends WebApplicationException {
    public Exceptions(int code, String error)  {
        super(new Throwable(error), Response.status(code)
                .entity("{\"error\":\"" + error + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build());

    }

    public static Exceptions invalidInput = new Exceptions(400, "Invalid Input");

    public static Exceptions missingIdsParameter = new Exceptions(400, "Missing ids Parameter.");
    public static Exceptions invalidStatementsParameter = new Exceptions(400, "Invalid statements Parameter.");
    public static Exceptions emptyIdsParameter = new Exceptions(400, "Empty statements Parameter.");

    public static Exceptions equipmentNotFound = new Exceptions(400, "Equipment not found.");
    public static Exceptions equipmentTypeNotFound = new Exceptions(400, "Equipment Type not found.");

    public static Exceptions missingKeyParameter = new Exceptions(400, "Missing key Parameter.");
    public static Exceptions missingValueParameter = new Exceptions(400, "Missing value Parameter.");
    public static Exceptions nullKeyParameter = new Exceptions(400, "Key Parameter cannot be null.");
    public static Exceptions nullValueParameter = new Exceptions(400, "Value Parameter cannot be null.");
    public static Exceptions missingConditionsParameter = new Exceptions(400, "Missing conditions Parameter.");


}