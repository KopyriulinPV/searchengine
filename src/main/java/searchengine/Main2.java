package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.services.TravelingTheWeb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main2 {
    public static <RussianAnalayzer> void main(String[] args) throws IOException, InterruptedException {
        String url = "https://dimonvideo.ru/";
        String url2 = "https://yandex.ru/maps/213/moscow/";

        /*URL pathUrl = new URL(url);

        URLConnection qq = pathUrl.openConnection();
        Map<String, List<String>> ww = qq.getHeaderFields();

        System.out.println(ww);*/

       /* Document document = Jsoup.connect(url).get();
        System.out.println(document.outerHtml().toString());*/
       /* Document document = Jsoup.connect(url).get();
        System.out.println(docu);*/

       /*Connection.Response response = Jsoup.connect(url).
                userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 " +
                        "(KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                .timeout(150).execute();
        System.out.println(response.statusCode());

        Document document = Jsoup.connect(url).get();
        System.out.println(document);*/

        /*statusCode()*/

       /* URL pathUrl = new URL(url);
        System.out.println(pathUrl.getPath());

        System.out.println(pathUrl.getHost());
        System.out.println(pathUrl.equals(pathUrl.getHost()));

        Pattern pattern = Pattern.compile(pathUrl.getHost());

        Matcher matcher = pattern.matcher(url2);

        System.out.println(matcher.find());

        Document document1 = Jsoup.connect(url).get();
        Thread.sleep(150);
        System.out.println(document1.getAllElements().toString());*/

        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);

        String text = "Повторное появление леопарда в " +
                "Осетии позволяет предположить, что леопард постоянно обитает в " +
                "некоторых районах Северного Кавказа";



    }


}
