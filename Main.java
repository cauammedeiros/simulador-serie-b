import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== MOTOR DE ANÁLISE INTEGRADA COM TABELA GERAL ===");
        
        ArrayList<Time> timesReais = ConectorAPI.buscarDadosSerieB();
        ArrayList<Partida> todasPartidas = ConectorAPI.buscarJogosRestantes();

        if (timesReais.isEmpty() || todasPartidas.isEmpty()) {
            System.out.println("Erro ao carregar dados da API.");
            return;
        }

        System.out.println("\nProcessando matrizes combinatórias e calculando médias finais...");

        // Mapas para armazenar os resultados matemáticos exatos de cada equipe
        HashMap<String, Double> mediasPontosFinais = new HashMap<>();
        HashMap<String, Double> vitoriasEstimadasFinais = new HashMap<>();
        HashMap<String, double[]> distribuicoesPorTime = new HashMap<>();

        HashMap<String, Integer> mapaPontosOutros = new HashMap<>();
        for (Time t : timesReais) {
            mapaPontosOutros.put(t.getNome(), t.getPontos());
        }

        // 1. Executa o algoritmo de Programação Dinâmica para cada um dos 20 times
        for (Time timeAlvo : timesReais) {
            ArrayList<Partida> jogosDoTime = new ArrayList<>();
            for (Partida p : todasPartidas) {
                if (p.getTimeMandante().equals(timeAlvo.getNome()) || p.getTimeVisitante().equals(timeAlvo.getNome())) {
                    jogosDoTime.add(p);
                }
            }

            int pontosAtuais = timeAlvo.getPontos();
            int vitoriasAtuais = timeAlvo.getVitorias();
            int jogosRestantes = jogosDoTime.size();
            int pontosMaximosPossiveis = pontosAtuais + (jogosRestantes * 3);
            
            double[] distribuicaoPontos = new double[pontosMaximosPossiveis + 1];
            double[] distribuicaoVitorias = new double[jogosRestantes + 1];
            distribuicaoPontos[pontosAtuais] = 1.0;
            distribuicaoVitorias[0] = 1.0;

            double vitoriasEsperadasNesteBloco = 0.0;

            for (Partida jogo : jogosDoTime) {
                boolean souMandante = jogo.getTimeMandante().equals(timeAlvo.getNome());
                String adversario = souMandante ? jogo.getTimeVisitante() : jogo.getTimeMandante();
                int ptsAdversario = mapaPontosOutros.getOrDefault(adversario, 1);

                double forcaMeuTime = Math.max(pontosAtuais, 1) * (souMandante ? 1.15 : 1.0);
                double forcaAdversario = Math.max(ptsAdversario, 1) * (souMandante ? 1.0 : 1.15);
                double soma = forcaMeuTime + forcaAdversario;

                double probEmpate = 0.26;
                double probVitoria = (forcaMeuTime / soma) * (1.0 - probEmpate);
                double probDerrota = (forcaAdversario / soma) * (1.0 - probEmpate);

                vitoriasEsperadasNesteBloco += probVitoria;

                double[] proximaDistribuicao = new double[pontosMaximosPossiveis + 1];
                for (int p = pontosAtuais; p <= pontosMaximosPossiveis; p++) {
                    if (distribuicaoPontos[p] > 0) {
                        if (p + 3 <= pontosMaximosPossiveis) proximaDistribuicao[p + 3] += distribuicaoPontos[p] * probVitoria;
                        if (p + 1 <= pontosMaximosPossiveis) proximaDistribuicao[p + 1] += distribuicaoPontos[p] * probEmpate;
                        proximaDistribuicao[p] += distribuicaoPontos[p] * probDerrota;
                    }
                }
                distribuicaoPontos = proximaDistribuicao;
            }

            double mediaPontosFinal = 0;
            for (int p = 0; p <= pontosMaximosPossiveis; p++) {
                mediaPontosFinal += p * distribuicaoPontos[p];
            }

            mediasPontosFinais.put(timeAlvo.getNome(), mediaPontosFinal);
            vitoriasEstimadasFinais.put(timeAlvo.getNome(), vitoriasAtuais + vitoriasEsperadasNesteBloco);
            distribuicoesPorTime.put(timeAlvo.getNome(), distribuicaoPontos);
        }

        // 2. Criar uma lista duplicada ordenada por critérios de desempate para a Tabela Geral
        ArrayList<Time> tabelaClassificacao = new ArrayList<>(timesReais);
        Collections.sort(tabelaClassificacao, (t1, t2) -> {
            double pts1 = mediasPontosFinais.get(t1.getNome());
            double pts2 = mediasPontosFinais.get(t2.getNome());
            // Critério 1: Média de pontos estimados
            if (Math.abs(pts2 - pts1) > 0.001) {
                return Double.compare(pts2, pts1);
            }
            // Critério 2: Desempate por Vitórias Estimadas
            return Double.compare(vitoriasEstimadasFinais.get(t2.getNome()), vitoriasEstimadasFinais.get(t1.getNome()));
        });

        // 3. Montagem do HTML com Sistema de Abas Alternáveis
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n")
            .append("  <meta charset=\"UTF-8\">\n")
            .append("  <title>Painel de Projeções Avançadas - Série B</title>\n")
            .append("  <style>\n")
            .append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f4f6f9; padding: 40px; color: #333; margin: 0; }\n")
            .append("    .container { max-width: 950px; margin: 0 auto; background: white; padding: 40px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }\n")
            .append("    h1 { text-align: center; color: #111; margin-bottom: 25px; }\n")
            .append("    /* Menu de Abas */\n")
            .append("    .tabs { display: flex; border-bottom: 2px solid #eaeaea; margin-bottom: 30px; justify-content: center; }\n")
            .append("    .tab-button { padding: 12px 30px; font-size: 16px; font-weight: 600; cursor: pointer; background: none; border: none; color: #666; border-bottom: 3px solid transparent; transition: all 0.2s; outline: none; }\n")
            .append("    .tab-button:hover { color: #1a73e8; }\n")
            .append("    .tab-button.active { color: #1a73e8; border-bottom-color: #1a73e8; }\n")
            .append("    .tab-content { display: none; }\n")
            .append("    .tab-content.active { display: block; }\n")
            .append("    /* Estilos da Tabela Classificação */\n")
            .append("    table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n")
            .append("    th { background-color: #f8f9fa; color: #444; font-weight: 600; padding: 14px; border-bottom: 2px solid #eaeaea; font-size: 13px; text-transform: uppercase; text-align: center; }\n")
            .append("    th.left, td.left { text-align: left; }\n")
            .append("    td { padding: 14px; border-bottom: 1px solid #f0f0f0; font-size: 15px; text-align: center; }\n")
            .append("    tr:hover { background-color: #fafbfc; }\n")
            .append("    .pos { font-weight: bold; width: 25px; display: inline-block; color: #888; margin-right: 8px; }\n")
            .append("    /* Zonas de tabela */\n")
            .append("    .g2 { background-color: #e6f4ea; }\n")
            .append("    .playoffs { background-color: #e8f0fe; }\n")
            .append("    .z4 { background-color: #fce8e6; }\n")
            .append("    .badge { padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; text-transform: uppercase; }\n")
            .append("    .b-g2 { background-color: #34a853; color: white; }\n")
            .append("    .b-playoff { background-color: #1a73e8; color: white; }\n")
            .append("    .b-z4 { background-color: #ea4335; color: white; }\n")
            .append("    /* Seletor Aba 2 */\n")
            .append("    .selector-container { text-align: center; margin-bottom: 30px; }\n")
            .append("    select { padding: 12px 20px; font-size: 16px; border-radius: 8px; border: 1px solid #ccd1d9; width: 50%; cursor: pointer; font-weight: 500; }\n")
            .append("    .meta-box { background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: center; }\n")
            .append("    .destaque { font-size: 24px; font-weight: bold; color: #1a73e8; display: block; margin-top: 5px; }\n")
            .append("    .bar-row { display: flex; align-items: center; margin-bottom: 8px; }\n")
            .append("    .bar-label { width: 90px; font-weight: bold; font-size: 14px; text-align: right; padding-right: 15px; }\n")
            .append("    .bar-wrapper { flex-grow: 1; background: #eef2f7; border-radius: 4px; height: 24px; position: relative; }\n")
            .append("    .bar-fill { background: linear-gradient(90deg, #4285f4, #34a853); height: 100%; border-radius: 4px; }\n")
            .append("    .bar-value { position: absolute; right: -50px; top: 3px; font-size: 13px; font-weight: 600; color: #555; }\n")
            .append("  </style>\n</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append("  <h1>PAINEL DE SIMULAÇÃO - BRASILEIRÃO SÉRIE B</h1>\n")
            .append("  \n")
            .append("  \n")
            .append("  <div class=\"tabs\">\n")
            .append("    <button class=\"tab-button active\" onclick=\"mudarAba('tabelaGeral')\">Tabela Geral Projetada</button>\n")
            .append("    <button class=\"tab-button\" onclick=\"mudarAba('analiseClube')\">Análise por Clube</button>\n")
            .append("  </div>\n")
            .append("  \n")
            .append("  \n")
            .append("  <div id=\"tabelaGeral\" class=\"tab-content active\">\n")
            .append("    <table>\n")
            .append("      <tr>\n")
            .append("        <th class=\"left\">Posição / Clube</th>\n")
            .append("        <th>Média Pontos Estimados</th>\n")
            .append("        <th>Vitórias Projetadas</th>\n")
            .append("        <th>Situação Espacial</th>\n")
            .append("      </tr>\n");

        for (int pos = 0; pos < tabelaClassificacao.size(); pos++) {
            Time t = tabelaClassificacao.get(pos);
            double ptsEst = mediasPontosFinais.get(t.getNome());
            double vitsEst = vitoriasEstimadasFinais.get(t.getNome());

            String classeLinha = "";
            String badge = "<span style='color:#777;'>Série B Mantida</span>";

            if (pos < 2) {
                classeLinha = " class=\"g2\"";
                badge = "<span class=\"badge b-g2\">Acesso (G2)</span>";
            } else if (pos >= 2 && pos <= 5) {
                classeLinha = " class=\"playoffs\"";
                badge = "<span class=\"badge b-playoff\">Playoffs</span>";
            } else if (pos >= 16) {
                classeLinha = " class=\"z4\"";
                badge = "<span class=\"badge b-z4\">Z4 Rebaixamento</span>";
            }

            html.append("      <tr").append(classeLinha).append(">\n")
                .append("        <td class=\"left\"><span class=\"pos\">").append(pos + 1).append("</span>").append(t.getNome()).append("</td>\n")
                .append("        <td><strong>").append(String.format("%.1f", ptsEst)).append("</strong></td>\n")
                .append("        <td>").append(String.format("%.1f", vitsEst)).append("</td>\n")
                .append("        <td>").append(badge).append("</td>\n")
                .append("      </tr>\n");
        }
        html.append("    </table>\n  </div>\n\n");

        // TELA 2: ANÁLISE INDIVIDUAL POR CLUBE
        html.append("  \n")
            .append("  <div id=\"analiseClube\" class=\"tab-content\">\n")
            .append("    <div class=\"selector-container\">\n")
            .append("      <select id=\"timeSelector\" onchange=\"alternarTime()\">\n");

        // Lista de dropdown ordenada alfabeticamente para facilitar a procura
        ArrayList<Time> listaAlfabetica = new ArrayList<>(timesReais);
        Collections.sort(listaAlfabetica, (t1, t2) -> t1.getNome().compareTo(t2.getNome()));

        for (int i = 0; i < listaAlfabetica.size(); i++) {
            String nome = listaAlfabetica.get(i).getNome();
            html.append("        <option value=\"panel-").append(i).append("\"").append(i == 0 ? " selected" : "").append(">").append(nome).append("</option>\n");
        }
        html.append("      </select>\n    </div>\n");

        for (int i = 0; i < listaAlfabetica.size(); i++) {
            Time timeAlvo = listaAlfabetica.get(i);
            double ptsEst = mediasPontosFinais.get(timeAlvo.getNome());
            double[] dist = distribuicoesPorTime.get(timeAlvo.getNome());
            
            int pontosAtuais = timeAlvo.getPontos();
            int maxPts = pontosAtuais + ((todasPartidas.size() / 10) * 3); // Ajuste dinâmico do teto de pontos
            if (maxPts > 114) maxPts = 114; // Trava lógica regulamento

            double maxProb = 0.001;
            for (double v : dist) if (v > maxProb) maxProb = v;

            html.append("    <div id=\"panel-").append(i).append("\" class=\"time-panel\" style=\"display: ").append(i == 0 ? "block" : "none").append(";\">\n")
                .append("      <div class=\"meta-box\">\n")
                .append("        Pontuação Real do Momento: <strong>").append(pontosAtuais).append(" pts</strong><br>\n")
                .append("        <span class=\"destaque\">Pontuação Média Projetada: ").append(Math.round(ptsEst)).append(" pontos</span>\n")
                .append("        <small style=\"color:#888;\">(Valor exato ponderado: ").append(String.format("%.2f", ptsEst)).append(")</small>\n")
                .append("      </div>\n")
                .append("      <div class=\"chart-container\">\n")
                .append("        <h3>Curva Probabilística de Pontuação Final:</h3>\n");

            for (int p = 0; p < dist.length; p++) {
                if (p >= dist.length) break;
                double chance = dist[p] * 100;
                if (chance > 0.4) {
                    double larguraBarra = (dist[p] / maxProb) * 100;
                    html.append("        <div class=\"bar-row\">\n")
                        .append("          <div class=\"bar-label\">").append(p).append(" pts</div>\n")
                        .append("          <div class=\"bar-wrapper\">\n")
                        .append("            <div class=\"bar-fill\" style=\"width: ").append(String.format("%.1f", larguraBarra)).append("%;\"></div>\n")
                        .append("            <div class=\"bar-value\">").append(String.format("%.2f%%", chance)).append("</div>\n")
                        .append("          </div>\n")
                        .append("        </div>\n");
                }
            }
            html.append("      </div>\n    </div>\n");
        }
        html.append("  </div>\n</div>\n\n");

        // Script JS para controlar a troca de Abas e a troca de Times
        html.append("<script>\n")
            .append("  // Alterna entre a Tabela Geral e os Gráficos individuais\n")
            .append("  function mudarAba(abaId) {\n")
            .append("    var conteudos = document.getElementsByClassName('tab-content');\n")
            .append("    for (var i = 0; i < conteudos.length; i++) {\n")
            .append("      conteudos[i].classList.remove('active');\n")
            .append("    }\n")
            .append("    var botoes = document.getElementsByClassName('tab-button');\n")
            .append("    for (var i = 0; i < botoes.length; i++) {\n")
            .append("      botoes[i].classList.remove('active');\n")
            .append("    }\n")
            .append("    document.getElementById(abaId).classList.add('active');\n")
            .append("    event.currentTarget.classList.add('active');\n")
            .append("  }\n\n")
            .append("  // Controla o Dropdown de times ocultando/exibindo o painel correto\n")
            .append("  function alternarTime() {\n")
            .append("    var selector = document.getElementById('timeSelector');\n")
            .append("    var idAlvo = selector.value;\n")
            .append("    var paineis = document.getElementsByClassName('time-panel');\n")
            .append("    for (var i = 0; i < paineis.length; i++) {\n")
            .append("      paineis[i].style.display = 'none';\n")
            .append("    }\n")
            .append("    document.getElementById(idAlvo).style.display = 'block';\n")
            .append("  }\n")
            .append("</script>\n")
            .append("</body>\n</html>");

        try (FileWriter writer = new FileWriter("relatorio_simulacao.html")) {
            writer.write(html.toString());
            System.out.println("\n-> Sucesso! Painel completo com Abas e Tabela Geral de Desempate gerado!");
        } catch (IOException e) {
            System.out.println("Erro ao salvar arquivo HTML: " + e.getMessage());
        }
    }
}