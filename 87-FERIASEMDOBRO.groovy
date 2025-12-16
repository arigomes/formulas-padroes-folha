Funcoes.somenteFuncionarios()
categorias = [
        'AGENTE_PUBLICO',
        'AGENTE_POLITICO',
        'SERVIDOR_PUBLICO_COMISSAO',
        'SERVIDOR_PUBLICO_EFETIVO'
]
categoria = funcionario.categoriaSefipVinculo.toString()
if (categorias.contains(categoria)) {
    suspender "Este evento não é calculado para funcionários com categoria SEFIP igual ou superior a 12"
}
def valorFerias = Funcoes.replicaFeriasNaFolhaMensal(evento.codigo)
if (valorFerias.valor > 0) {
    Bases.compor(valorFerias.valor, Bases.INSSFER, Bases.FGTS)
}
valorCalculado = valorFerias.valor
valorReferencia = valorFerias.referencia
if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
    if (folha.folhaPagamento) {
        valorReferencia = 0
        valorCalculado = 0
        folhas.buscaFolhas().each { f ->
            f.eventos.each { e ->
                if (e.codigo == evento.codigo) {
                    valorReferencia += e.referencia
                    valorCalculado += e.valor
                }
            }
        }
        if (valorCalculado > 0) {
            Bases.compor(valorCalculado, Bases.IRRFFER)
        }
    } else {
        long diasfer = 0
        def datafin = Datas.adicionaDias(periodoAquisitivo.dataFinal, 1)
        def mesesconces = periodoAquisitivo.configuracaoFerias.mesesParaConcessao
        datafin = Datas.adicionaMeses(datafin, mesesconces)
        datafin = Datas.removeDias(datafin, 1)
        if (periodoConcessao.dataInicioGozo > datafin) {
            diasfer = Datas.diferencaDias(periodoConcessao.dataInicioGozo, periodoConcessao.dataFimGozo) + 1
        } else {
            if (periodoConcessao.dataFimGozo > datafin) {
                diasfer = Datas.diferencaDias(datafin, periodoConcessao.dataFimGozo)
            }
        }
        def vlrrefaux
        def vaux = Lancamentos.valor(evento)
        if (vaux >= 0) {
            valorCalculado = vaux
        } else {
            def diasdirfer = periodoAquisitivo.configuracaoFerias.diasParaAdquirirNoPeriodo
            vlrrefaux = diasfer / diasdirfer
            if (vlrrefaux > 2) {
                vlrrefaux = 2
            }
            def vlrcalcux = Funcoes.remuneracao(matricula.tipo).valor * diasdirfer / 30
            valorCalculado = vlrcalcux * vlrrefaux
            valorReferencia = vlrrefaux
        }
    }
}
