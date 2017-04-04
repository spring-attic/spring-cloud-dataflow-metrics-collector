package org.springframework.cloud.stream.app.metrics.collector;

/**
 * Created by mpollack on 4/4/17.
 */
public class Greeting {


    private final long id;
    private final String content;

    public Greeting(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
