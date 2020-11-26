package xyz.liut.bingwallpaper.http;

import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.BaseTestCase;

/**
 * Create by liut
 * 2020/11/2
 */
public class HttpClientTest extends BaseTestCase {

    public static final String API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";
    public static final String URL = "https://cn.bing.com/th?id=OHR.TorngatsMt_ROW2999822673_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp\",\"urlbase\":\"/th?id=OHR.TorngatsMt_ROW2999822673";

    @Test
    public void doRequest() {
        Response<String> resp = HttpClient.getInstance().doRequest(Method.get, API);
        System.out.println(resp);
    }

    @Test
    public void download() {
        Response<File> resp = HttpClient.getInstance().download(URL, "build/test.jpg", true);
        System.out.println(resp);
    }
}
