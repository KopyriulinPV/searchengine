package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
public class SearchServicesImpl implements SearchServices {
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

    @Override
    public SearchResponse getSearch(String query, String offset,
                                    String limit, String site) throws IOException {
        Set<String> lemmaSet = createLemmaSet(query);
        List<Lemma> listInAscendingFrequency = sortListInAscendingFrequency(lemmaSet, site);
        List<Page> listForTheLowestFrequencyLemma = createListForTheLowestFrequencyLemma(listInAscendingFrequency, query);
        Set<Page> listOfPagesFilteredByTheFollowingLemmas = createListOfPagesFilteredByTheFollowingLemmas(listInAscendingFrequency,
                listForTheLowestFrequencyLemma, query);
        LinkedHashMap<Page, Float> mapDescendingRelativeRelevance = sortMapDescendingRelativeRelevance(listOfPagesFilteredByTheFollowingLemmas);
        return createSearchResponse(mapDescendingRelativeRelevance, query, limit, offset);
    }

    private Set<String> createLemmaSet(String query) throws IOException {
        return lemmaFinder.collectLemmas(query).keySet();
    }

    private List<Lemma> sortListInAscendingFrequency(Set<String> lemmaSet, String site) {
        List<Lemma> lemmaList = new ArrayList<>();
        for (String lemmaString : lemmaSet) {
            if (site == null) {
                List<Lemma> lemmaList1 = lemmaRepository.findByLemma(lemmaString);
                addLemmaToLemmaList(lemmaList1, lemmaList);
            } else {
                List<Site> sites = siteRepository.findAllContains(site);
                Site modelSite = sites.get(0);
                List<Lemma> qq = lemmaRepository.findByLemma(lemmaString);
                List<Lemma> lemmaList1 = qq.stream().filter(r -> r.getSite().equals(modelSite)).toList();
                addLemmaToLemmaList(lemmaList1, lemmaList);
            }
        }
        Comparator<Lemma> comparator = Comparator.comparing(obj -> obj.getFrequency());
        Collections.sort(lemmaList, comparator);
        return lemmaList;
    }

    private void addLemmaToLemmaList(List<Lemma> lemmaList1, List<Lemma> lemmaList) {
        for (Lemma lemma1 : lemmaList1) {
            lemmaList.add(lemma1);
        }
    }

    private List<Page> createListForTheLowestFrequencyLemma(List<Lemma> listInAscendingFrequency,
                                                            String query) throws IOException {
        List<Page> listPage = new ArrayList<>();
        List<Index> listIndex = new ArrayList<>();
        if (getCountWordsQuery(query) == 1 && listInAscendingFrequency.size() > 0) {
            for (Lemma lemma : listInAscendingFrequency) {
                List<Index> listIndex1 = indexRepository.findByLemma_id(lemma.getId());
                addPageOnList(listIndex1, listPage);
            }
            return listPage;
        }

        if (listInAscendingFrequency.size() > 0) {
            List<Lemma> newLemmaList = new ArrayList<>();
            listInAscendingFrequency.stream().
                    filter(q -> q.getLemma().equals(listInAscendingFrequency.get(0).getLemma())).forEach(q -> newLemmaList.add(q));
            newLemmaList.forEach(q -> listIndex.addAll(indexRepository.findByLemma_id(q.getId())));
        }
        addPageOnList(listIndex, listPage);
        return listPage;
    }

    private void addPageOnList(List<Index> listIndex, List<Page> listPage) {
        for (Index index : listIndex) {
            Optional<Page> pageOptional = pageRepository.findById(index.getPage().getId());
            if (!(pageOptional == null)) {
                listPage.add(pageOptional.get());
            }
        }
    }

    private int getCountWordsQuery(String query) throws IOException {
        String[] countWords = query.split(" ");
        return countWords.length;
    }

    /**
     * Ищем пересечения на одной Page для следующих лемм из списка полученного из метода createListForTheLowestFrequencyLemma.
     * Получаем Set<Page>, в котором Page содержит все слова из query.
     * Если нет Page со всеми словами из query, то returne выдаст пустой Set<Page>.
     */
    private Set<Page> createListOfPagesFilteredByTheFollowingLemmas(List<Lemma> listInAscendingFrequency,
                                                                    List<Page> listForTheLowestFrequencyLemma,
                                                                    String query) throws IOException {
        Set<Page> setPage = new HashSet<>();
        if (getCountWordsQuery(query) == 1) {
            for (Page page : listForTheLowestFrequencyLemma) {
                setPage.add(page);
            }
            return setPage;
        }
        for (Page page : listForTheLowestFrequencyLemma) {
            List<Lemma> lemmas = new ArrayList<>();
            listInAscendingFrequency.stream().
                    filter(q -> q.getSite().equals(page.getSite())).
                    forEach(q -> lemmas.add(q));
            int q = addPageOnList(lemmas, page);
            if (q == lemmas.size()) {
                setPage.add(page);
            }
        }
        return setPage;
    }

    private int addPageOnList(List<Lemma> lemmas, Page page) {
        int q = 0;
        for (Lemma lemma : lemmas) {
            List<Index> indexList = lemma.getIndexes();
            for (Index index : indexList) {
                if (index.getPage().equals(page)) {
                    q++;
                    break;
                }
            }
        }
        return q;
    }

    private LinkedHashMap<Page, Float> sortMapDescendingRelativeRelevance(Set<Page> setPage) {
        Map<Page, Float> rankAmountsForEachPage = new HashMap<>();
        List<Index> allIndex = indexRepository.findAll();
        for (Page page : setPage) {
            float qq = 0f;
            List<Index> indexes = allIndex.stream().filter(q -> q.getPage().equals(page)).toList();
            for (Index index : indexes) {
                qq += index.getLemma_rank();
            }
            float rr = qq / searchMaxRank(setPage);
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

    private SearchResponse createSearchResponse(LinkedHashMap<Page, Float> sortMapDescendingRelativeRelevance,
                                                String query, String limit, String offset) throws IOException {
        int offset1 = Integer.parseInt(offset);
        int limit1 = Integer.parseInt(limit);
        List<searchengine.dto.search.FoundPageData> listData = new ArrayList<>();
        List<searchengine.dto.search.FoundPageData[]> listData2 = new ArrayList<>();
        List<List<searchengine.dto.search.FoundPageData>> listData3 = new ArrayList<>();

        for (Map.Entry<Page, Float> entry : sortMapDescendingRelativeRelevance.entrySet()) {
            searchengine.dto.search.FoundPageData dataItem = new searchengine.dto.search.FoundPageData();
            dataItem.setSite(entry.getKey().getSite().getUrl());
            dataItem.setSiteName(entry.getKey().getSite().getName());
            dataItem.setUri(entry.getKey().getPath());
            String stringContent = entry.getKey().getContent();
            Document html = Jsoup.parse(stringContent);
            String title = html.title();
            dataItem.setTitle(title);
            SnippetService snippetService = new SnippetServiceImpl(lemmaFinder);
            String snippet = snippetService.getSnippet(query, stringContent);
            if (snippet == null) {
                continue;
            }
            dataItem.setSnippet(snippet);
            dataItem.setRelevance(entry.getValue());
            listData.add(dataItem);
        }
        List<searchengine.dto.search.FoundPageData> listDataInResponse = new ArrayList<>();
        int startIndex = offset1;
        int finalIndex = (startIndex + limit1) - 1;
        for (int i = 0; i < listData.size(); i++) {
            if ((startIndex <= i) && (i <= finalIndex)) {
                listDataInResponse.add(listData.get(i));
            }
        }
        SearchResponse searchResponse = new SearchResponse();
        if (listData.size() == 0) {
            searchResponse.setResult(false);
            searchResponse.setError("Задан пустой поисковый запрос");
        } else {
            searchResponse.setResult(true);
            searchResponse.setCount(listData.size());
            searchResponse.setData(listDataInResponse);
        }
        return searchResponse;
    }
}




