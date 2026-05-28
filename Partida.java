public class Partida {
    private String timeMandante;
    private String timeVisitante;
    private int rodada;

    // Construtor para criar a partida com os dados da API
    public Partida(String timeMandante, String timeVisitante, int rodada) {
        this.timeMandante = timeMandante;
        this.timeVisitante = timeVisitante;
        this.rodada = rodada;
    }

    // Getters para a lógica de simulação usar depois
    public String getTimeMandante() {
        return timeMandante;
    }

    public String getTimeVisitante() {
        return timeVisitante;
    }

    public int getRodada() {
        return rodada;
    }
}