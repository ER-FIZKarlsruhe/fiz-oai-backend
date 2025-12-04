/*
 * Copyright 2025 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;

public class SetResumptionTokenCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class TokenPayload {

        public final String pagingState; // Cassandra paging state
        public final int cursor;

        @JsonCreator
        public TokenPayload(@JsonProperty("ps") String pagingState, @JsonProperty("c") int cursor) {
            this.pagingState = pagingState;
            this.cursor = cursor;
        }
    }



    public static String encode(String pagingState, int cursor) {
        try {
            TokenPayload payload = new TokenPayload(pagingState, cursor);
            byte[] json = MAPPER.writeValueAsBytes(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode resumption token", e);
        }
    }

    public static TokenPayload decode(String token) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(token);
            return MAPPER.readValue(json, TokenPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid resumption token", e);
        }
    }

}
