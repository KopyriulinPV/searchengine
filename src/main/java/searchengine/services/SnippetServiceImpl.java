package searchengine.services;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Service
public class SnippetServiceImpl implements SnippetService {
    LemmaFinder lemmaFinder;
    public SnippetServiceImpl(LemmaFinder lemmaFinder) {
        this.lemmaFinder = lemmaFinder;
    }
    /**
     * получаем сниппет
     */
    public String getSnippet(String query, String stringDocument) throws IOException {
        List<String> listOfCorrectQueryWordForms = listOfCorrectQueryWordForms(query);
        List<Integer> indexesLemma = indexesLemma(listOfCorrectQueryWordForms, stringDocument);
        List<String> snippetList = new ArrayList<>();
        for (String word : listOfCorrectQueryWordForms) {
            for (Integer index : indexesLemma) {
                String regex = "[//<&>\"\"]";
                int indexEnd = 0;
                for (int i = index; i < (index + 120); i++) {
                    Character tt = stringDocument.charAt(i);
                    String ww = tt.toString();
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(ww);
                    if (matcher.find()) {
                        indexEnd = i;
                        break;
                    }
                    if (i == (index + 119)) {
                        indexEnd = i;
                    }
                }
                int indexStart = 0;
                for (int j = index; j > (index - 120); --j) {
                    Character tt = stringDocument.charAt(j);
                    String ww = tt.toString();
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(ww);
                    if (matcher.find()) {
                        indexStart = j;
                        break;
                    }
                    if (j == (index - 119)) {
                        indexStart = j;
                    }
                }
                purgeText(indexStart, indexEnd, stringDocument, word, snippetList);
            }
        }
        if (snippetList.size() == 0) {
            return null;
        }
        return snippetList.get(0);
    }
    /**
     * чистка текста, выделение жирным
     */
    private String purgeText(int indexStart, int indexEnd, String stringDocument, String word, List<String> snippetList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int q = indexStart; q <= indexEnd; q++) {
            Character kk = stringDocument.charAt(q);
            stringBuilder.append(kk);
        }
        String snippet1 = stringBuilder.toString().trim();
        String snippet = snippet1.replaceAll("[//<&>\"\"\n]+", "");
        String regex2 = word;
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
            snippetList.add(finalSnippet.trim());
        }
        Comparator<String> comparator = Comparator.comparing(obj -> obj.length());
        Collections.sort(snippetList, comparator.reversed());
        return null;
    }
    /**
     * получаем слова из query с помощью леммизатора
     */
    private List<String> listOfCorrectQueryWordForms(String query) throws IOException {
        List<String> listOfCorrectQueryWordForms = new ArrayList<>();
        String[] words = query.split("\\s");
        for (String w : words) {
            Set<String> formationLemmaSet = lemmaFinder.getLemmaSet(w);
            listOfCorrectQueryWordForms.add(formationLemmaSet.stream().toList().get(0));
        }
        return listOfCorrectQueryWordForms;
    }
    /**
     * получаем индексы первого символа слова, которое ищем
     */
    private List<Integer> indexesLemma(List<String> listOfCorrectQueryWordForms, String stringDocument) throws IOException {
        List<Integer> indexesLemma = new ArrayList<>();
        for (String correctQueryWord : listOfCorrectQueryWordForms) {
            int index = stringDocument.indexOf(correctQueryWord);
            while (index != -1) {
                indexesLemma.add(index);
                index = stringDocument.indexOf(correctQueryWord, index + 1);
            }
        }
        return indexesLemma;
    }
}
