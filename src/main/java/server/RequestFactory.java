package server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestFactory {
    private final String host;
    private final String endpoint;
    private final Map<String, String> queryParams;
    private final String method;
    private final Map<String, String> headers;
    private final String body;

    public RequestFactory() {
        this.host = null;
        this.endpoint = null;
        this.queryParams = new HashMap<>();
        this.method = null;
        this.headers = new HashMap<>();
        this.body = null;
    }

    private RequestFactory(
            String host,
            String endpoint,
            Map<String, String> queryParams,
            String method,
            Map<String, String> headers,
            String body
    ) {
        this.host = host;
        this.endpoint = endpoint;
        this.queryParams = queryParams;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public RequestFactory host(String host) {
        return new RequestFactory(host, endpoint, queryParams, method, headers, body);
    }

    public RequestFactory endpoint(String endpoint) {
        return new RequestFactory(host, endpoint, queryParams, method, headers, body);
    }

    public RequestFactory queryParam(String param, String value) {
        Map<String, String> queryParams = new HashMap<>(this.queryParams);
        if (value == null) {
            queryParams.remove(param);
        } else {
            queryParams.put(param, value);
        }
        return this.queryParams(queryParams);
    }

    public RequestFactory queryParams(Map<String, String> queryParams) {
        return new RequestFactory(host, endpoint, queryParams, method, headers, body);
    }

    public RequestFactory method(String method) {
        return new RequestFactory(host, endpoint, queryParams, method, headers, body);
    }

    public RequestFactory header(String header, String value) {
        Map<String, String> headers = new HashMap<>(this.headers);
        headers.put(header, value);
        return this.headers(headers);
    }

    public RequestFactory headers(Map<String, String> headers) {
        return new RequestFactory(host, endpoint, queryParams, method, headers, body);
    }

    public RequestFactory body(String body) {
        return new RequestFactory(host, endpoint, queryParams, method, headers, body);
    }

    public <T> RequestFactory body(T value) {
        String input = new Gson().toJson(value);
        RequestFactory nextFactory = this.body(input);
        if (this.headers.get("Content-Type") == null) {
            return nextFactory.header("Content-Type", "application/json; charset=UTF-8");
        }
        return nextFactory;
    }

    private InputStream rawRequest() throws IOException {
        if (this.host == null) {
            throw new RuntimeException("host not provided to RequestFactory");
        }
        if (this.method == null) {
            throw new RuntimeException("method not provided to RequestFactory");
        }
        String endpoint = this.endpoint == null ? "" : this.endpoint;
        String query = this.queryParams.isEmpty() ? "" : queryParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        String url = host + endpoint + query;

        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
        httpURLConnection.setRequestMethod(method);
        headers.forEach(httpURLConnection::setRequestProperty);
        httpURLConnection.setDoOutput(true);
        if (body != null) {
            try (OutputStream os = httpURLConnection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }
        int status = httpURLConnection.getResponseCode();
        if (status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE) {
            return httpURLConnection.getInputStream();
        }
        throw new IOException(status + ": " + httpURLConnection.getResponseMessage());
    }

    public <T> T request() throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.rawRequest()))) {
            Type type = new TypeToken<T>() {
            }.getType();
            if (type == new TypeToken<String>() {
            }.getType()) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
                return (T) response.toString();
            }
            JsonReader reader = new JsonReader(bufferedReader);

            return new Gson().fromJson(reader, type);
        }
    }
}
