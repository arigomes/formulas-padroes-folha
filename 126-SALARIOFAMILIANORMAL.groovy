Funcoes.somenteFuncionarios()
def valorFerias = Funcoes.replicaFeriasNaFolhaMensal(evento.codigo)
valorCalculado = valorFerias.valor
valorReferencia = valorFerias.referencia
if (valorFerias.valor <= 0) {
    if (folha.folhaPagamento) {
        if (TipoMatricula.FUNCIONARIO.equals(matricula.tipo) && funcionario.conselheiroTutelar) {
            suspender("Este cálculo não é realizado para conselheiros tutelares")
        }
        if (!Funcoes.possuiPrevidenciaFederal(matricula.tipo)) {
            suspender "Este cálculo é realizado apenas para matrículas contribuintes da previdência federal"
        }
        int dependentes = servidor.getDependenteSalarioFamilia(TipoSalarioFamilia.CELETISTA)
        if (dependentes < 1) {
            suspender "Não há dependentes de salário família para a matrícula ou não há configuração vigente de salário família do tipo 'Celetista' na entidade"
        }
        double base = Funcoes.remuneracao(matricula.tipo).valor + Bases.valor(Bases.SALAFAM)
        if (TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento) || TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
            base += Bases.valorCalculado(Bases.SALAFAM, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
        }
        if (Funcoes.cedidocomonus() > 0 && Bases.valor(Bases.PAGAPROP) == 0) {
            suspender "A matrícula está cedida, com ônus, durante toda a competência"
        }
        int diastrab = Funcoes.diastrab()
        def classificacoes
        if (Funcoes.diasgozo() == 0 && !TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
            classificacoes = [
                    3 : ClassificacaoTipoAfastamento.ACIDENTE_DE_TRABALHO_PREVIDENCIA,
                    4 : ClassificacaoTipoAfastamento.SERVICO_MILITAR,
                    6 : ClassificacaoTipoAfastamento.AUXILIO_DOENCA_PREVIDENCIA,
                    7 : ClassificacaoTipoAfastamento.LICENCA_SEM_VENCIMENTOS,
                    8 : ClassificacaoTipoAfastamento.DEMITIDO,
                    9 : ClassificacaoTipoAfastamento.APOSENTADO,
                    11: ClassificacaoTipoAfastamento.ACIDENTE_DE_TRAJETO_PREVIDENCIA,
                    12: ClassificacaoTipoAfastamento.DOENCA_DO_TRABALHO_PREVIDENCIA
            ]
            for (classificacao in classificacoes) {
                def i = classificacao.key
                def afastadoPrimeiroDiaCompetencia = false
                def primeiroDiaCompetencia = Funcoes.inicioCompetencia()
                Afastamentos.buscaPorPeriodo(primeiroDiaCompetencia, primeiroDiaCompetencia, classificacoes[i]).each {
                    afastadoPrimeiroDiaCompetencia = true
                }
                Afastamentos.buscaPorPeriodo(classificacoes[i]).each { afast ->
                    def dataAfastamento = afast.inicio
                    if ((dataAfastamento == null) ||
                            ((dataAfastamento != null) & (dataAfastamento <= primeiroDiaCompetencia)) &
                            (afastadoPrimeiroDiaCompetencia)) {
                        suspender "Não é possível calcular o salário família pois a matrícula possui um tipo de afastamento iniciado em competencia anterior ou atual que não é permitido para o cálculo"
                    }
                }
                if (i == 7 && [UnidadePagamento.DIARISTA, UnidadePagamento.HORISTA].contains(Funcoes.remuneracao(matricula.tipo).unidade)) {
                    if (Funcoes.afaslicsvenc() == calculo.quantidadeDiasCompetencia) {
                        suspender "Não é possível calcular o salário família para matrículas com afastamento por 'Licença (SEM vencimentos) - Servidor Público', 'Licença (NÃO remunerada)' ou 'Suspensão de pagamento de servidor público por não recadastramento' durante toda a competência"
                    }
                }
                if (i == 7 && UnidadePagamento.MENSALISTA == Funcoes.remuneracao(matricula.tipo).unidade) {
                    if (Funcoes.afaslicsvenc().equals(30)) {
                        suspender "Não é possível calcular o salário família para matrículas com afastamento por 'Licença (SEM vencimentos) - Servidor Público', 'Licença (NÃO remunerada)' ou 'Suspensão de pagamento de servidor público por não recadastramento' durante toda a competência"
                    }
                }
            }
        } else {
            classificacoes = [
                    ClassificacaoTipoAfastamento.LICENCA_COM_VENCIMENTOS,
                    ClassificacaoTipoAfastamento.LICENCA_MATERNIDADE,
                    ClassificacaoTipoAfastamento.ABORTO_NAO_CRIMINOSO,
                    ClassificacaoTipoAfastamento.ADOCAO_GUARDA_JUDICIAL_DE_CRIANCA,
                    ClassificacaoTipoAfastamento.PRORROGACAO_DA_LICENCA_MATERNIDADE,
                    ClassificacaoTipoAfastamento.PRORROGACAO_DA_LICENCA_MATERNIDADE_11_770
            ]
            int diasafast = Funcoes.diasafastcalc30(calculo.competencia, classificacoes)
            int diasferiasant
            if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
                if (!periodoConcessao.diasGozo || periodoConcessao.diasGozo <= 0) {
                    suspender "Este evento deve ser calculado apenas em lançamentos de férias com gozo"
                } else {
                    diasferiasant = Funcoes.diasFeriasAnteriores(periodoConcessao.dataInicioGozo)
                }
            }
            int cedidosemonus = Funcoes.cedidosemonus()
            int dias = diastrab + cedidosemonus + diasafast - diasferiasant
            if (dias > 0) {
                suspender "O evento será calculado no processamento mensal pois há dias trabalhados na competência"
            }
        }
        valorReferencia = dependentes
        def vvar = Lancamentos.valor(evento)
        if (vvar > 0) {
            valorCalculado = vvar
        } else {
            double valorTotal = EncargosSociais.SalarioFamilia.Celetista.buscaContribuicao(base.trunc(2), 2) * valorReferencia
            //True quando for o mês da admissão, mas que não seja no primeiro dia do mês, ou quando for o mês da rescisão, mas que não seja o último dia do mês
            boolean mesAdmissaoDemissao
            def dataAdmissao = Funcoes.dadosMatricula().dataInicio
            def dataRescisao = Funcoes.dtrescisao()
            mesAdmissaoDemissao = (Funcoes.inicioCompetencia() < dataAdmissao)
            if (!mesAdmissaoDemissao && dataRescisao != null) {
                if (Datas.ano(dataRescisao) == Datas.ano(calculo.competencia) && Datas.mes(dataRescisao) == Datas.mes(calculo.competencia) && calculo.quantidadeDiasCompetencia > Datas.dia(dataRescisao)) {
                    mesAdmissaoDemissao = true
                }
            }
            def diaTrab = 0
            if (mesAdmissaoDemissao) {
                diaTrab = diastrab
                valorTotal *= diastrab
            }
            if (diaTrab != 0) {
                if (UnidadePagamento.HORISTA.equals(funcionario.unidadePagamento) || UnidadePagamento.DIARISTA.equals(funcionario.unidadePagamento)) {
                    valorTotal /= calculo.quantidadeDiasCompetencia
                } else {
                    valorTotal /= 30
                }
            }
            valorCalculado = valorTotal
        }
    }
}
