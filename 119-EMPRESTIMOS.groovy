def valorFerias = Funcoes.replicaEventoVariavel(evento.codigo)
if (valorFerias.valor > 0) {
    valorCalculado = valorFerias.valor
    valorReferencia = valorFerias.referencia
} else {
    if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
        if (periodoConcessao.diasGozo == null) {
            suspender 'Não é lançado empréstimo para férias sem dias de gozo, este lançamento ocorrerá na folha mensal'
        }
        if (Funcoes.concessaoFeriasCompetencia(periodoConcessao.dataInicioGozo, periodoConcessao.dataFimGozo) < 10) {
            suspender 'Não há ao menos 10 dias de gozo dentro da competência, este lançamento ocorrerá na folha mensal'
        }
    }
    def vvar = Lancamentos.valor(evento)
    if (vvar > 0) {
        double valorVariavel
        if (!TipoProcessamento.DECIMO_TERCEIRO_SALARIO.equals(calculo.tipoProcessamento)) {
            valorVariavel = vvar - Eventos.valorCalculado(evento.codigo, TipoValor.CALCULADO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
            if (valorVariavel <= 0) {
                suspender 'O valor do evento de empréstimo lançado em variável já foi integralmente pago em processamento de férias nesta competência'
            }
        }
        valorCalculado = valorVariavel
    } else {
        if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento) && calculo.descontarEmprestimoRescisao) {
            valorCalculado = Funcoes.emprestimos()
        } else {
            valorCalculado = Funcoes.emprestimos(calculo.competencia)
        }
    }
}
