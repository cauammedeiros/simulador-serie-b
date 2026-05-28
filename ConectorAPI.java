import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConectorAPI {
    private static final String KEY = "0cdf85ef9cmsh7a4b544a6e46f70p1598e9jsnb3b9eb7f22f0";
    private static final String HOST = "sofascore6.p.rapidapi.com";

    public static ArrayList<Time> buscarDadosSerieB() {
        ArrayList<Time> listaTimes = new ArrayList<>();
        String urlAPI = "https://sofascore6.p.rapidapi.com/api/sofascore/v1/unique-tournament/season/standings?season_id=89840&unique_tournament_id=390&type=total";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlAPI)).header("x-rapidapi-key", KEY).header("x-rapidapi-host", HOST).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();

            Pattern p = Pattern.compile("\\{\"position\":(.*?)\"promotion\"");
            Matcher m = p.matcher(json);
            while (m.find()) {
                String bloco = m.group(0);
                String nome = corrigirUnicode(extrairValor(bloco, "\"name\":\"(.*?)\""));
                int pontos = Integer.parseInt(extrairValor(bloco, "\"points\":(\\d+)"));
                int vitorias = Integer.parseInt(extrairValor(bloco, "\"wins\":(\\d+)"));
                listaTimes.add(new Time(nome, pontos, vitorias));
            }
        } catch (Exception e) { System.out.println("Erro na tabela: " + e.getMessage()); }
        return listaTimes;
    }

    public static ArrayList<Partida> buscarJogosRestantes() {
        ArrayList<Partida> listaJogos = new ArrayList<>();
        HashSet<String> jogosRegistrados = new HashSet<>(); 
        
        HttpClient client = HttpClient.newHttpClient();
        // Percorre todas as rodadas; o filtro interno de status cuida de excluir o passado
        for (int r = 11; r <= 38; r++) {
            String urlAPI = "https://sofascore6.p.rapidapi.com/api/sofascore/v1/unique-tournament/season/round/matches?round=" + r + "&season_id=89840&unique_tournament_id=390";
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlAPI)).header("x-rapidapi-key", KEY).header("x-rapidapi-host", HOST).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();

                Pattern pJogo = Pattern.compile("\\{\"id\":\\d+,\"name\":(.*?)\"finalResultOnly\"");
                Matcher mJogo = pJogo.matcher(json);
                while (mJogo.find()) {
                    String blocoJogo = mJogo.group(0);
                    // FILTRO AUTOMÁTICO: Só adiciona se o status NÃO for "finished"
                    String status = extrairValor(blocoJogo, "\"status\":\\{\"type\":\"(.*?)\"");
                    if ("finished".equals(status)) continue;

                    String mandante = corrigirUnicode(extrairValor(blocoJogo, "\"homeTeam\":\\{.*?\"name\":\"(.*?)\""));
                    String visitante = corrigirUnicode(extrairValor(blocoJogo, "\"awayTeam\":\\{.*?\"name\":\"(.*?)\""));
                    
                    if (mandante.equals("0") || visitante.equals("0") || mandante.equals(visitante)) continue;

                    String chave = r + "-" + mandante + "-" + visitante;
                    if (!jogosRegistrados.contains(chave)) {
                        jogosRegistrados.add(chave);
                        listaJogos.add(new Partida(mandante, visitante, r));
                    }
                }
            } catch (Exception e) { }
        }
        return listaJogos;
    }

    private static String extrairValor(String t, String e) {
        Matcher m = Pattern.compile(e).matcher(t);
        return m.find() ? m.group(1) : "0";
    }

    private static String corrigirUnicode(String t) {
        return t.replace("\\u00e3", "ã").replace("\\u00e1", "á").replace("\\u00e9", "é")
                .replace("\\u00ed", "í").replace("\\u00f3", "ó").replace("\\u00fa", "ú")
                .replace("\\u00e2", "â").replace("\\u00f4", "ô").replace("\\u00ea", "ê").replace("\\u00e7", "ç");
    }
}