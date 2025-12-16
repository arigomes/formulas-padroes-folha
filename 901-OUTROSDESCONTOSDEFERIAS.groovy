if (calculo.replicaEventosCalculoFeriasParaCalculoMensal) {
    if (!TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento) &&
            (!TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento) || TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento) && !calculo.recalcularMensal)) {
        suspender "O evento deve ser calculado apenas em processamentos mensais ou rescisórios"
    }
    def vvar = Lancamentos.valor(evento)
    if (vvar > 0) {
        valorCalculado = vvar
    }
}
