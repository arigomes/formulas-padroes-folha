Funcoes.somenteFuncionarios()
if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
    def vvar = Lancamentos.valor(evento)
    if (vvar > 0) {
        valorReferencia = vvar
        valorCalculado = Funcoes.calcprop(funcionario.salario,vvar)
        if (Eventos.valor(75) == 0) {
            Bases.compor(valorCalculado, Bases.SIND)
        }
        Bases.compor(valorCalculado,
                Bases.FGTS,
                Bases.INSS,
                Bases.PREVEST,
                Bases.FUNDASS,
                Bases.FUNDOPREV,
                Bases.FUNDFIN,
                Bases.IRRFFER,
                Bases.COMPHORAMES)
    }
}
