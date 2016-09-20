/*
 * Copyright 2016 Copyright 2016 Akamai Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akamai.edgegrid.signer;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


/**
 * Library-agnostic representation of an HTTP request. This object is immutable, so you probably
 * want to build an instance using {@link RequestBuilder}. Extenders of
 * {@link AbstractEdgeGridRequestSigner} will need to build one of these as part of their
 * implementation.
 *
 * @author mgawinec@akamai.com
 * @author mmeyer@akamai.com
 */
public class Request implements Comparable<Request> {

    private final byte[] body;
    private final String method;
    private final URI uriWithQuery;
    private final Map<String, String> headers;

    private Request(RequestBuilder b) {
        this.body = b.body;
        this.method = b.method;
        this.headers = b.headers;
        this.uriWithQuery = b.uriWithQuery;
    }

    /**
     * Returns a new builder. The returned builder is equivalent to the builder
     * generated by {@link RequestBuilder}.
     *
     * @return a fresh {@link RequestBuilder}
     */
    public static RequestBuilder builder() {
        return new RequestBuilder();
    }

    @Override
    public int compareTo(Request that) {
        return new CompareToBuilder()
                .append(this.body, that.body)
                .append(this.headers, that.headers)
                .append(this.method, that.method)
                .append(this.uriWithQuery, that.uriWithQuery)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;
        final Request that = (Request) o;
        return compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, headers, method, uriWithQuery);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
                .append("body", body)
                .append("headers", headers)
                .append("method", method)
                .append("uriWithQuery", uriWithQuery)
                .build();
    }

    byte[] getBody() {
        return body;
    }

    Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    String getMethod() {
        return method;
    }

    URI getUriWithQuery() {
        return uriWithQuery;
    }

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link Request#builder()}.
     */
    public static class RequestBuilder implements Builder<Request> {

        private byte[] body = new byte[]{};
        private Map<String, String> headers = new HashMap<>();
        private String method;
        private URI uriWithQuery;

        /**
         * Sets a content of HTTP request body. If not set, body is empty by default.
         *
         * @param requestBody a request body, in bytes
         * @return reference back to this builder instance
         */
        public RequestBuilder body(byte[] requestBody) {
            Validate.notNull(body, "body cannot be blank");
            this.body = Arrays.copyOf(requestBody, requestBody.length);
            return this;
        }

        /**
         * <p>
         * Adds a single header for an HTTP request. This can be called multiple times to add as
         * many headers as needed.
         * </p>
         * <p>
         * <i>NOTE: All header names are lower-cased for storage. In HTTP, header names are
         * case-insensitive anyway, and EdgeGrid does not support multiple headers with the same
         * name. Forcing to lowercase here improves our chance of detecting bad requests early.</i>
         * </p>
         *
         * @param headerName a header name
         * @param value a header value
         * @return reference back to this builder instance
         * @throws RequestSigningException if a duplicate header name is encountered
         */
        public RequestBuilder header(String headerName, String value) throws RequestSigningException {
            Validate.notEmpty(headerName, "headerName cannot be empty");
            Validate.notEmpty(value, "value cannot be empty");
            headerName = headerName.toLowerCase();
            if (this.headers.containsKey(headerName)) {
                throw new RequestSigningException("Duplicate header found: " + headerName);
            }
            headers.put(headerName, value);
            return this;
        }

        /**
         * <p>
         * Sets headers of HTTP request. The {@code headers} parameter is copied so that changes
         * to the original {@link Map} will not impact the stored reference.
         * </p>
         * <p>
         * <i>NOTE: All header names are lower-cased for storage. In HTTP, header names are
         * case-insensitive anyway, and EdgeGrid does not support multiple headers with the same
         * name. Forcing to lowercase here improves our chance of detecting bad requests early.</i>
         * </p>
         *
         * @param headers a {@link Map} of headers
         * @return reference back to this builder instance
         * @throws RequestSigningException if a duplicate header name is encountered
         */
        public RequestBuilder headers(Map<String, String> headers) throws RequestSigningException {
            Validate.notNull(headers, "headers cannot be null");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Sets HTTP method: GET, PUT, POST, DELETE. Mandatory to set.
         *
         * @param method an HTTP method
         * @return reference back to this builder instance
         */
        public RequestBuilder method(String method) {
            Validate.notBlank(method, "method cannot be blank");
            this.method = method;
            return this;
        }

        /**
         * Sets absolute URI of HTTP request including query string. Mandatory to set.
         *
         * @param uriWithQuery a {@link URI}
         * @return reference back to this builder instance
         */
        public RequestBuilder uriWithQuery(URI uriWithQuery) {
            Validate.notNull(uriWithQuery, "uriWithQuery cannot be blank");
            this.uriWithQuery = uriWithQuery;
            return this;
        }

        /**
         * Returns a newly-created immutable HTTP request.
         */
        @Override
        public Request build() {
            Validate.notNull(body, "body cannot be blank");
            Validate.notBlank(method, "method cannot be blank");
            Validate.notNull(uriWithQuery, "uriWithQuery cannot be blank");
            return new Request(this);
        }

    }

}
