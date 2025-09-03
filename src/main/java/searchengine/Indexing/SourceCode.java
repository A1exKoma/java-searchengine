package searchengine.Indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class SourceCode {
    public synchronized Connection.Response getSourceCode(String URL) throws InterruptedException, IOException {
        Thread.sleep(1000);
        return Jsoup.connect(URL)
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (SEAK, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                .timeout(10000)
                .execute();
    }
}