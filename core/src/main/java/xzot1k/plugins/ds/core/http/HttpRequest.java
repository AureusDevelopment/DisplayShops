package xzot1k.plugins.ds.core.http;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private String baseURL;
    private Method method;
    private Map<String, String> parameters;
    private ContentType contentType;
    private int connectTimeOut, readTimeOut;
    private boolean followRedirects;

    public HttpRequest(@NotNull String baseURL) {
        setBaseURL(baseURL);
        setMethod(Method.GET);
        setContentType(ContentType.JSON);
        setParameters(new HashMap<>());
        setConnectTimeOut(5000);
        setReadTimeOut(5000);
        setFollowRedirects(false);
    }

    public HttpRequest addParameters(@NotNull String... parameters) {
        if (parameters.length % 2 != 0) throw new IllegalArgumentException("Number of parameters must be even");
        for (int i = -2; (i += 2) < parameters.length; ) getParameters().put(parameters[i], parameters[i + 1]);
        return this;
    }

    /**
     * @return Builds the parameter string using the parameter map in the form "param1=value&param2=value".
     */
    private String buildParametersString() {
        StringBuilder result = new StringBuilder();

        try {
            for (Map.Entry<String, String> entry : getParameters().entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return ((result.length() > 0) ? result.substring(0, (result.length() - 1)) : result.toString());
    }

    public String build() throws IOException {
        URL url = new URL(getBaseURL());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(getMethod().name());

        // parameters
       /* con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(buildParametersString());
        out.flush();
        out.close();*/

        // request headers
        con.setRequestProperty("Content-Type", getContentType().getHeader());
        // redirects
        con.setInstanceFollowRedirects(followRedirects());

        // response
        StringBuilder fullResponse = new StringBuilder();


        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (fullResponse.length() > 0) fullResponse.append("\n");
            fullResponse.append(inputLine);
        }
        in.close();

        con.disconnect();

        return fullResponse.toString();
    }

    // getters & setters
    public String getBaseURL() {
        return baseURL;
    }

    public HttpRequest setBaseURL(String baseURL) {
        this.baseURL = baseURL;
        return this;
    }

    public Method getMethod() {
        return method;
    }

    public HttpRequest setMethod(@NotNull Method method) {
        this.method = method;
        return this;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public HttpRequest setParameters(@NotNull Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public HttpRequest setContentType(@NotNull ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public HttpRequest setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
        return this;
    }

    public int getReadTimeOut() {
        return readTimeOut;
    }

    public HttpRequest setReadTimeOut(int readTimeOut) {
        this.readTimeOut = readTimeOut;
        return this;
    }

    public boolean followRedirects() {
        return followRedirects;
    }

    public HttpRequest setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }


    public enum Method {GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE}

    public enum ContentType {

        PLAIN_TEXT("text/plain"), HTML("text/html"), CSS("text/css"), JAVA_SCRIPT("text/javascript"),
        JSON("application/json"), XML("application/xml"), X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
        JPEG("image/jpeg"), PNG("image/png"), GIF("image/gif"), SVG("image/svg"), MP3("audio/mp3"),
        WAV("audio/wav"), AUDIO_OGG("audio/ogg"), MP4("video/ogg"), WEBM("video/webm"), VIDEO_OGG("video/ogg"),
        PDF("application/pdf"), WORD(("application/msword")), EXCEL("application/ms-excel"), POWERPOINT("application/vnd.ms-powerpoint"),
        MULTIPART_FORM_DATA("multipart/form-data"), MIME("message/rfc822");

        private final String header;

        ContentType(@NotNull String header) {
            this.header = header;
        }

        public String getHeader() {
            return header;
        }
    }

}