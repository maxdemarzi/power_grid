package com.maxdemarzi;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Validators {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HashMap getValidEquipmentIds(String body) throws IOException {
        HashMap input;

        if ( body == null) {
            throw Exceptions.invalidInput;
        }

        // Parse the input
        try {
            input = objectMapper.readValue(body, HashMap.class);
        } catch (Exceptions e) {
            throw Exceptions.invalidInput;
        }

        if (!input.containsKey("ids")) {
            throw Exceptions.missingIdsParameter;
        } else {
            Object statements = input.get("ids");
            if (statements instanceof List<?>) {
                if (((List) statements).isEmpty()) {
                    throw Exceptions.emptyIdsParameter;
                }
            } else {
                throw Exceptions.invalidStatementsParameter;
            }
        }
        return input;
    }
}
