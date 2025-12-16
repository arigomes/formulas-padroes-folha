Funcoes.somenteFuncionarios()
def valorFerias = Funcoes.replicaFeriasNaFolhaMensal(evento.codigo)
valorCalculado = valorFerias.valor
valorReferencia = valorFerias.referencia
if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
    if (folha.folhaPagamento) {
        def diasAbono = folha.diasAbono
        if (diasAbono <= 0) {
            suspender "Não há dias de abono pecuniário lançados em férias"
        }
        def vaux = Lancamentos.valor(evento)
        if (vaux >= 0) {
            valorReferencia = vaux
            valorCalculado = vaux
        } else {
            vaux = Eventos.valor(76) + Eventos.valor(83) + Eventos.valor(84) + Eventos.valor(85) + Eventos.valor(239)
            valorReferencia = evento.taxa
            valorCalculado = vaux * valorReferencia / 100
        }
        if (valorCalculado > 0) {
            Bases.compor(valorCalculado, Bases.IRRFFER)
        }
    }
}
