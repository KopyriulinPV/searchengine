package searchengine.services;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServicesImpl implements  SearchServices {
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    LemmaFinder lemmaFinder;
    @Autowired
    public SearchServicesImpl(PageRepository pageRepository, SiteRepository siteRepository,
                              LemmaRepository lemmaRepository, IndexRepository indexRepository,
                              LemmaFinder lemmaFinder) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
    }
    /**
     * запускаем поиск
     */
    @Override
    public SearchResponse getSearch(String query, String offset,
                                    String limit, String site) throws IOException {
        Set<String> formationLemmaSet = formationLemmaSet(query);
        List<Lemma> listSortedAscendingLemmaFrequency = listSortedAscendingLemmaFrequency(formationLemmaSet, site);
        List<Page> listPageWithLowestFrequency = listPageWithLowestFrequency(listSortedAscendingLemmaFrequency);
        Set<Page> listOfPagesFilteredByTheFollowingLemmas = listOfPagesFilteredByTheFollowingLemmas(listSortedAscendingLemmaFrequency,
                listPageWithLowestFrequency);
        LinkedHashMap<Page, Float> sortedMapDescendingRelativeRelevance = sortedMapDescendingRelativeRelevance(listOfPagesFilteredByTheFollowingLemmas);
        return formationSearchResponse(sortedMapDescendingRelativeRelevance, query);
    }
    /**
     * формируем SearchResponse
     */
     private SearchResponse formationSearchResponse(LinkedHashMap<Page, Float> sortedMapDescendingRelativeRelevance,
                                                    String query) {
         List<searchengine.dto.search.FoundPageData> listData = new ArrayList<>();
         for (Map.Entry<Page, Float> entry : sortedMapDescendingRelativeRelevance.entrySet()) {
            searchengine.dto.search.FoundPageData dataItem = new searchengine.dto.search.FoundPageData();
            dataItem.setSite(entry.getKey().getSite().getUrl());
            dataItem.setSiteName(entry.getKey().getSite().getName());
            dataItem.setUri(entry.getKey().getPath());
            try {
                Document document = Jsoup.connect(entry.getKey().getSite().getUrl() + entry.getKey().getPath()).get();
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Elements elements = document.select("head > title");
                dataItem.setTitle(elements.text());
                String stringDocument = document.getAllElements().toString();
                SnippetService snippetService = new SnippetServiceImpl(lemmaFinder);
                String snippet = snippetService.getSnippet(query, stringDocument);
                if (snippet == null) {
                    continue;
                }
                dataItem.setSnippet(snippet);
            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }
            dataItem.setRelevance(entry.getValue());
            listData.add(dataItem);
        }
         SearchResponse searchResponse = new SearchResponse();
        if (listData.size() == 0) {
            searchResponse.setResult(false);
            searchResponse.setError("Задан пустой поисковый запрос");
        } else {
            searchResponse.setResult(true);
            searchResponse.setCount(listData.size());
            searchResponse.setData(listData);
        }
        return searchResponse;
    }
    /**
     * Формируем Set String лемм
     */
    private Set<String> formationLemmaSet (String query) throws IOException {
        Set<String> formationLemmaSet = lemmaFinder.getLemmaSet(query);
        return formationLemmaSet;
    }
    /**
     * получаем List<Lemma> отсортированных по возрастанию частоты встречаемости
     */
    private List<Lemma> listSortedAscendingLemmaFrequency (Set<String> formationLemmaSet, String site) {
        List<Lemma> lemmaList = new ArrayList<>();
        for (String lemmaString : formationLemmaSet) {
            List<Lemma> temporaryLemmaList = new ArrayList<>();
            if (site == null) {
                List<Lemma> qq = lemmaRepository.findByLemma(lemmaString);
                for (Lemma lemma1 : qq) {
                    temporaryLemmaList.add(lemma1);
                }
                Comparator<Lemma> comparator = Comparator.comparing(obj -> obj.getFrequency());
                Collections.sort(temporaryLemmaList, comparator.reversed());
                if (temporaryLemmaList.size() > 0) {
                     lemmaList.add(temporaryLemmaList.get(0));
                }
            } else {
                List<Site> sites = siteRepository.findAllContains(site);
                Site modelSite = sites.get(0);
                List<Lemma> qq = lemmaRepository.findAllContains(lemmaString, modelSite.getId());
                for (Lemma lemma1 : qq) {
                    temporaryLemmaList.add(lemma1);
                }
                Comparator<Lemma> comparator = Comparator.comparing(obj -> obj.getFrequency());
                Collections.sort(temporaryLemmaList, comparator.reversed());
                if (temporaryLemmaList.size() > 0) {
                    lemmaList.add(temporaryLemmaList.get(0));
                }
            }
        }
        Comparator<Lemma> comparator = Comparator.comparing(obj -> obj.getFrequency());
        Collections.sort(lemmaList, comparator);
        return lemmaList;
    }
    /**
     * По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается, получаем List<Page>
     */
    private List<Page> listPageWithLowestFrequency(List<Lemma> listSortedAscendingLemmaFrequency) throws IOException {
        List<Page> listPage = new ArrayList<>();
        List<Index> listIndex = new ArrayList<>();
        if (listSortedAscendingLemmaFrequency.size() > 0) {
            listIndex = indexRepository.findByLemma_id(listSortedAscendingLemmaFrequency.get(0).getId());
        }
        for (Index index : listIndex) {
            Optional<Page> pageOptional = pageRepository.findById(index.getPage().getId());
            if (!(pageOptional == null)) {
                listPage.add(pageOptional.get());
            }
        }
        return listPage;
    }
    /**
     * Ищем соответствия следующей леммы из списка полученного из метода listPageWithLowestFrequency, а затем повторяем
     * операцию по каждой следующей лемме.
     */
    private Set<Page> listOfPagesFilteredByTheFollowingLemmas(List<Lemma> listSortedAscendingLemmaFrequency,
                                                              List<Page> listPageWithLowestFrequency) {
        Set<Page> setPage = new HashSet<>();
        if (listSortedAscendingLemmaFrequency.size() == 1) {
            for (Page page : listPageWithLowestFrequency) {
                setPage.add(page);
            }
        }
               for (int i = 1; i < listSortedAscendingLemmaFrequency.size(); i++) {
                for (Page page : listPageWithLowestFrequency) {
                    Index index = indexRepository.findAllContains(listSortedAscendingLemmaFrequency.get(i).getId(), page.getId());
                    if (!(index == null)) {
                        setPage.add(index.getPage());
                    }
                }
                if (!(setPage.size() == 0)) {
                listPageWithLowestFrequency.clear();

                for (Page page : setPage) {
                    listPageWithLowestFrequency.add(page);
                  }
                }
                if (i < (listSortedAscendingLemmaFrequency.size() - 1)) {
                    setPage.clear();
                }
            }
       return setPage;
    }
    /**
     * получаем максимальную абсолютную релевантность
     */
    private int searchMaxRank(Set<Page> setPage) {
    List<Integer> searchMaxRank = new ArrayList<>();
        for (Page page : setPage) {
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
        return maxRank;
    }
    /**
     * сортирует страницы по убыванию релевантности
     */
    private LinkedHashMap<Page, Float> sortedMapDescendingRelativeRelevance(Set<Page> setPage) {

    Map<Page, Float> rankAmountsForEachPage = new HashMap<>();
        for (Page page : setPage) {
        float qq = 0f;
        List<Index> indexes = indexRepository.findByPage_id(page.getId());
        for (Index index : indexes) {
            qq += index.getLemma_rank();
        }
        float rr = qq/searchMaxRank(setPage);
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

        return sortedMap;
    }
}




