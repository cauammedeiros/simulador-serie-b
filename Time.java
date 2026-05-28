public class Time {
    private String nome;
    private int pontos;
    private int vitorias;

    // Contadores para os 59.049 cenários da rodada
    private int vezesNoG2;
    private int vezesNoPlayoff;
    private int vezesNoZ4;

    public Time(String nome, int pontos, int vitorias) {
        this.nome = nome;
        this.pontos = pontos;
        this.vitorias = vitorias;
        this.vezesNoG2 = 0;
        this.vezesNoPlayoff = 0;
        this.vezesNoZ4 = 0;
    }

    public Time clonar() {
        return new Time(this.nome, this.pontos, this.vitorias);
    }

    public void registrarG2() { this.vezesNoG2++; }
    public void registrarPlayoff() { this.vezesNoPlayoff++; }
    public void registrarZ4() { this.vezesNoZ4++; }

    public int getVezesNoG2() { return vezesNoG2; }
    public int getVezesNoPlayoff() { return vezesNoPlayoff; }
    public int getVezesNoZ4() { return vezesNoZ4; }

    public String getNome() { return nome; }
    public int getPontos() { return pontos; }
    public void setPontos(int pontos) { this.pontos = pontos; }
    public int getVitorias() { return vitorias; }
    public void setVitorias(int vitorias) { this.vitorias = vitorias; }
}