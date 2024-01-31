package searchengine.services;
import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import searchengine.repositories.SiteRepository;

@Service
@Data
public class TravelingTheWeb extends RecursiveTask<String> {
    private List<String> linksFromTags = new ArrayList<>();

    private Site site;
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private  String url;

    public static boolean indexingStop;


    public TravelingTheWeb(Site site, PageRepository pageRepository,
                           SiteRepository siteRepository, String Url) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.url = Url;
    }

    public TravelingTheWeb() {
    }

    String regex = "http[s]?://[^#, \\s]*\\.?[a-z]*\\.ru[^#,\\s]*";
    public int i = 0;


    @Override
    protected String compute() {
        try {
            Document document = Jsoup.connect(url).get();
            Thread.sleep(150);
            Elements elements = document.select("body").select("a");
            elements.forEach(element -> {
                linksFromTags.add(element.absUrl("href"));
            });

                for (int j = 0; j < linksFromTags.size(); j++) {

                    if (linksFromTags.get(j).matches(regex)) {
                        URL pathUrl = new URL(linksFromTags.get(j));
                        URL pathUrl2 = new URL(url);

                        Pattern pattern = Pattern.compile(pathUrl2.getHost());
                        Matcher matcher = pattern.matcher(linksFromTags.get(j));
                        if ((pageRepository.findByPath(pathUrl.getPath()) == null) && matcher.find()) {
                            if (indexingStop == true) {
                                return "Индексация остановлена пользователем";
                            }
                            TravelingTheWeb action = new TravelingTheWeb(site, pageRepository,
                                    siteRepository, linksFromTags.get(j));
                            action.fork();
                            site.setStatus_time(LocalDateTime.now());
                            siteRepository.saveAndFlush(site);

                            Page pageEntity = new Page();
                            pageEntity.setPath(pathUrl.getPath());

                            pageEntity.setSite(site);
                            Connection.Response code = Jsoup.connect(linksFromTags.get(j)).execute();
                            pageEntity.setCode(code.statusCode());

                            /*Document document1 = Jsoup.connect(linksFromTags.get(j)).get();
                            document1.getAllElements().toString()*/
                            pageEntity.setContent("Надо вставить строку выше!!!");
                            if ((pageRepository.findByPath(pathUrl.getPath()) == null)) {
                                pageRepository.saveAndFlush(pageEntity);
                            }
                            System.out.println("\n" + "ВОТ ЭТА ССЫЛКА " + url + " " +
                                    Thread.currentThread().getName() + " Печатаем Path из БД "
                                    + pageRepository.findByPath(pathUrl.getPath()).getPath() + "\n");

                            action.join();
                        }
                    }
                }


        } catch (Exception ex) {
            ex.printStackTrace();
            String error = ex.toString();
            return error;
        }

        return null;
    }
}




/*package searchengine.services;

        import lombok.Data;
        import org.hibernate.SessionBuilder;
        import org.jsoup.Connection;
        import org.jsoup.Jsoup;
        import org.jsoup.nodes.Document;
        import org.jsoup.select.Elements;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Service;
        import searchengine.model.Page;
        import searchengine.model.Site;
        import searchengine.repositories.PageRepository;

        import java.net.MalformedURLException;
        import java.net.URL;
        import java.time.LocalDateTime;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.List;
        import java.util.concurrent.RecursiveTask;
        import java.util.logging.Logger;

        import searchengine.repositories.SiteRepository;

        import javax.websocket.Session;

@Service
@Data
public class TravelingTheWeb extends RecursiveTask<String> {
    private List<String> linksFromTags = new ArrayList<>();

    private Site site;

    private String url;
    private PageRepository pageRepository;

    private SiteRepository siteRepository;

    public TravelingTheWeb(Site site, String url, PageRepository pageRepository, SiteRepository siteRepository) {
        this.site = site;

        this.url = url;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    public TravelingTheWeb() {

    }

    String regex = "http[s]?://[^#, \\s]*\\.?[a-z]*\\.ru[^#,\\s]*";
    public int i = 0;


    @Override
    protected String compute() {
        try {

            Document document = Jsoup.connect(url).get();
            Thread.sleep(150);
            Elements elements = document.select("body").select("a");
            elements.forEach(element -> {
                linksFromTags.add(element.absUrl("href"));
            });

            *//*linksFromTags.size()*//*
            for (int j = 0; j < 2; j++) {

                if (linksFromTags.get(j).matches(regex)) {
                    URL pathUrl = new URL(linksFromTags.get(j));
                    if ((pageRepository.findByPath(pathUrl.getPath()) == null)) {
                        TravelingTheWeb action = new TravelingTheWeb(site, site.getUrl(), pageRepository, siteRepository);
                        action.fork();
                        Page pageEntity = new Page();
                        pageEntity.setPath(pathUrl.getPath());
                        pageEntity.setSite(site);
                        Connection.Response code = Jsoup.connect(linksFromTags.get(j)).
                                userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 " +
                                        "(KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                                .execute();
                        pageEntity.setCode(code.statusCode());

                        pageEntity.setContent("text HTML page");
                        pageRepository.saveAndFlush(pageEntity);
                        System.out.println("\n" + Thread.currentThread().getName() + " Печатаем Path из БД "
                                + pageRepository.findByPath(pathUrl.getPath()).getPath() + "\n");
                        return action.join();
                    }
                }


            }


        } catch (Exception ex) {
            ex.printStackTrace();
            String error = ex.toString();
            return error;
        }

        return null;
    }
}*/






