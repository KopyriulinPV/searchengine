package searchengine.services;

import lombok.Data;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Service
public class SearchServicesImpl implements  SearchServices {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;



    @Override
    public SearchResponse getSearch(String query, String offset, String limit, String site) throws IOException {

        LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
        //НАЧАЛО Это Map лемм, которые мы получаем из query
        Map<String, Integer> lemmaFinderMap = lemmaFinder.collectLemmas(query);
        //КОНЕЦ Это Map лемм, которые мы получаем из query

        //НАЧАЛО формируем set лемм из lemmaFinderMap(был получен из query)
        Set<String> lemmaSet = new HashSet<>();
        for (Map.Entry<String, Integer> entry : lemmaFinderMap.entrySet()) {
            String lemmaString = entry.getKey();
            lemmaSet.add(lemmaString);
        }
        //КОНЕЦ формируем set лемм из lemmaFinderMap(был получен из query)

        //НАЧАЛО на основании полученного set лемм (из query) формируем Map<String, Integer>
        // lemmaFrequency и Set<Integer> frequencySetLemmas сопоставляя леммы (из query) с БД
        Map<String, Integer> lemmaFrequency = new HashMap<>();
        Set<Integer> frequencySetLemmas = new HashSet<>();
        for (String lemma : lemmaSet) {
            List<Lemma> qq = lemmaRepository.findByLemma(lemma);
            int ww = 1;
            for (Lemma lemma1 : qq) {
                if (ww <= lemma1.getFrequency()) {
                    ww = lemma1.getFrequency();
                }
            }
            frequencySetLemmas.add(ww);
            lemmaFrequency.put(lemma, ww);
        }
        //КОНЕЦ на основании полученного set лемм (из query) формируем Map<String, Integer>
        // lemmaFrequency и Set<Integer> frequencySetLemmas сопоставляя леммы (из query) с БД

        //НАЧАЛО сортируем Set<Integer> frequencySetLemmas и находим лемму с наименьшим
        // Frequency  int lowestFrequencyLemma
        frequencySetLemmas.stream().sorted();
        List<Integer> listSortedFrequency = frequencySetLemmas.stream().toList();
        int lowestFrequencyLemma =  listSortedFrequency.get(0);
        //КОНЕЦ сортируем Set<Integer> frequencySetLemmas и находим лемму с наименьшим
        // Frequency  int lowestFrequencyLemma

        //НАЧАЛО сортировка lemmaFrequency по возрастанию frequency
        Map<String, Integer> listSortedLemmaFrequency = lemmaFrequency.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new AssertionError(); },
                        LinkedHashMap::new));
        //КОНЕЦ сортировка lemmaFrequency по возрастанию frequency

        //НАЧАЛО формирует listPage список отфильтрованных Page по частоте лемм
        List<Page> listPage = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : listSortedLemmaFrequency.entrySet()) {

            if (lowestFrequencyLemma == entry.getValue()) {
                List<Lemma> rr = lemmaRepository.findByLemma(entry.getKey());
                for (Lemma lemma : rr) {
                    if (site == null) {
                        addPage(lemma, listPage);
                    } else if (lemma.getSite().getId() == siteRepository.findAllContains(site).get(0).getId()) {
                        addPage(lemma, listPage);
                    }
                }
            }
        }

        List<Page> sortedListPage = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : listSortedLemmaFrequency.entrySet()) {
            for (Page page : listPage) {
                List<Lemma> lemmas = lemmaRepository.findByLemma(entry.getKey());
                for (Lemma lemma : lemmas) {
                    List<Index> indices = indexRepository.findByLemma_id(lemma.getId());
                    for (Index index : indices) {
                        if (index.getPage().getId() == page.getId()) {
                            sortedListPage.add(index.getPage());
                        }
                     }
                }
            }
            if (!(sortedListPage.size() == 0)) {
            listPage.clear();
            for (Page page : sortedListPage) {
                listPage.add(page);
            }
            sortedListPage.clear();
            }
        }

        //КОНЕЦ формирует listPage список отфильтрованных Page по частоте лемм


        //НАЧАЛО получаем наибольший rank
        List<Integer> searchMaxRank = new ArrayList<>();
        for (Page page : listPage) {
            int qq = 0;
            List<Index> indexes = indexRepository.findByPage_id(page.getId());
            for (Index index : indexes) {
                qq += index.getLemma_rank();
            }
            searchMaxRank.add(qq);
        }
        Iterator<Integer> iterator = searchMaxRank.stream().iterator();
        int maxRank = 0;
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            if (maxRank <= next) {
                maxRank = next;
            }
        }
        //КОНЕЦ получаем наибольший rank

        //НАЧАЛО вычисляем относительную релевантность для каждой страницы и сортируем
        // Map по убыванию относительной релевантности, получаем sortedMap
        Map<Page, Float> rankAmountsForEachPage = new HashMap<>();
        for (Page page : listPage) {
            float qq = 0f;
            List<Index> indexes = indexRepository.findByPage_id(page.getId());
            for (Index index : indexes) {
                qq += index.getLemma_rank();
            }
            float rr = qq/maxRank;
            rankAmountsForEachPage.put(page, rr);
        }
        LinkedHashMap<Page, Float> sortedMap = rankAmountsForEachPage.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors
                        .toMap(Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new));
        //КОНЕЦ вычисляем относительную релевантность для каждой страницы и сортируем
        // Map по убыванию относительной релевантности, получаем sortedMap

       List<searchengine.dto.search.Data> listData = new ArrayList<>();

        //НАЧАЛО перебираем page, формируем SearchResponse
        for (Map.Entry<Page, Float> entry : sortedMap.entrySet()) {
           searchengine.dto.search.Data dataItem = new searchengine.dto.search.Data();
           dataItem.setSite(entry.getKey().getSite().getUrl());
           dataItem.setSiteName(entry.getKey().getSite().getName());
           dataItem.setUri(entry.getKey().getPath());
           try{
               Document document = Jsoup.connect(entry.getKey().getSite().getUrl() + entry.getKey().getPath()).get();
               try {
                   Thread.sleep(150);
               } catch (InterruptedException e) {
                   throw new RuntimeException(e);
               }
               Elements elements = document.select("head > title");

               dataItem.setTitle(elements.text());

               String stringDocument = document.getAllElements().toString();
               String fatLemma = null;

               for (Map.Entry<String, Integer> entry1 : listSortedLemmaFrequency.entrySet()) {
                   Pattern pattern = Pattern.compile(entry1.getKey());
                   Matcher matcher = pattern.matcher(stringDocument);
                   if (matcher.find()) {
                       if (!(stringDocument.indexOf(entry1.getKey()) == -1)) {
                           dataItem.setSnippet(snippet(entry1.getKey(), document));
                       }
                   }



              }
           }
           catch (Exception ex){
               ex.toString();
               continue;
           }

           dataItem.setRelevance(entry.getValue());
           listData.add(dataItem);
       }

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setCount(574);
        searchResponse.setData(listData);

        return searchResponse;
    }
    //КОНЕЦ перебираем page, формируем SearchResponse



    private void addPage(Lemma lemma, List<Page> listPage) {
        List<Index> pp = indexRepository.findByLemma_id(lemma.getId());
        for (Index yy : pp) {
            listPage.add(yy.getPage());
        }
    }

    private String snippet(String word, Document document) {

        String qq = document.getAllElements().toString();

        String regex = "[//<&>\"\"]";
        int indexEnd = 0;
        for (int i = qq.indexOf(word); i < (qq.indexOf(word) + 120); i++) {

            Character tt = qq.charAt(i);
            String ww = tt.toString();
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(ww);
            if (matcher.find()) {
                indexEnd = i;
                break;
            }
        }

        int indexStart = 0;
        for (int j = qq.indexOf(word); j > (qq.indexOf(word) - 120); --j) {
            Character tt = qq.charAt(j);
            String ww = tt.toString();
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(ww);
            if (matcher.find()) {
                indexStart = j;
                break;
            }
        }
        if ((indexStart == 0)&&((qq.indexOf(word) - 120) > 0)) {
            indexStart = qq.indexOf(word) - 120;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int q = indexStart; q <= indexEnd; q++) {
            Character kk = qq.charAt(q);
            stringBuilder.append(kk.toString());

        }
        String snippet1 = stringBuilder.toString();
        //НАЧАЛО очищаем snippet от мусора
        String snippet = snippet1.replaceAll("[//<&>\"\"\n]+", "");
        //КОНЕЦ очищаем snippet от мусора


        String regex2 = word ;
        /*+ "[^,\\s]+"*/
        Pattern pattern = Pattern.compile(regex2);
        Matcher matcher = pattern.matcher(snippet);
        String finalSnippet = "";
        while (matcher.find()) {

            int start = matcher.start();
            String beginIndexToEndIndexFirst = snippet.substring(0, start);

            int end = matcher.end();
            String beginIndexToEndIndexSecond = snippet.substring(end);

            finalSnippet = beginIndexToEndIndexFirst + "<b>" + snippet.substring(start, end) +
                    "</b>" + beginIndexToEndIndexSecond;

        }
        return finalSnippet;
    }
}




