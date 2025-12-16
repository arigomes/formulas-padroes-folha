if (!Funcoes.permitecalc13integral()) {
    suspender 'A matrícula não tem direito a receber décimo terceiro ou o seu período aquisitivo contém uma situação não permitida para o cálculo do evento'
}
if (matricula.possuiMultiploVinculo) {
    suspender 'Para matrículas com múltiplos vínculos será considerada a opção de deduções legais de 13º salário'
}
if (calculo.rra || TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento)) {
    suspender 'Este evento não é calculado em processamentos vinculados a pagamentos anteriores'
}
if (SubTipoProcessamento.ADIANTAMENTO.equals(calculo.subTipoProcessamento)) {
    suspender "O evento não é calculado no subtipo de processamento 'adiantamento'"
}
double deducaoSimplificada13IntegralAntecipado
def vvar = Lancamentos.valor(evento)
if (vvar >= 0) {
    valorCalculado = vvar
} else {
    if (!EncargosSociais.IRRF.deducaoSimplificadaIrrf) {
        suspender "Para apuração da dedução de 13º salário a ser aplicada para a matrícula, a 'Dedução simplificada do IRRF' deve ser informada na manutenção de estabelecimento vigente"
    }
    double desconto13SalarioDevidoReinteg
    if (TipoProcessamento.DECIMO_TERCEIRO_SALARIO.equals(calculo.tipoProcessamento)) {
        double deducaoCompleta13IntegralAntecipado
        if (folha.complementoDecimoTerceiro) {
            folhaDecimoTerceiroIntegralAntecipado.eventos.each { dadosEventos ->
                if (ClassificacaoEvento.DEDUCSIMPLIRRF13 == dadosEventos.classificacao && dadosEventos.valor > 0) {
                    deducaoSimplificada13IntegralAntecipado += dadosEventos.valor
                }
                if (ClassificacaoEvento.DEDUCCOMPLIRRF13 == dadosEventos.classificacao && dadosEventos.valor > 0) {
                    deducaoCompleta13IntegralAntecipado += dadosEventos.valor
                }
                if (ClassificacaoEvento.DESC13SALDEVREINTG == dadosEventos.classificacao && dadosEventos.valor > 0) {
                    desconto13SalarioDevidoReinteg += dadosEventos.valor
                }
            }
        } else {
            if ((TipoMatricula.APOSENTADO.equals(matricula.tipo) && aposentado.dataCessacaoAposentadoria) || (TipoMatricula.PENSIONISTA.equals(matricula.tipo) && pensionista.dataCessacaoBeneficio)) {
                if ((TipoMatricula.APOSENTADO.equals(matricula.tipo) && Datas.ano(aposentado.dataCessacaoAposentadoria).equals(Datas.ano(calculo.competencia)) && Datas.mes(aposentado.dataCessacaoAposentadoria).equals(Datas.mes(calculo.competencia))) ||
                        (TipoMatricula.PENSIONISTA.equals(matricula.tipo) && Datas.ano(pensionista.dataCessacaoBeneficio).equals(Datas.ano(calculo.competencia)) && Datas.mes(pensionista.dataCessacaoBeneficio).equals(Datas.mes(calculo.competencia)))) {
                    def inicioAnoBase = Datas.data(calculo.competencia.ano, 1, 1)
                    deducaoSimplificada13IntegralAntecipado += Funcoes.acumulaClassificacao(ClassificacaoEvento.DEDUCSIMPLIRRF13, TipoValor.CALCULADO, inicioAnoBase, calculo.competencia, TipoProcessamento.DECIMO_TERCEIRO_SALARIO, SubTipoProcessamento.INTEGRAL)
                    deducaoCompleta13IntegralAntecipado += Funcoes.acumulaClassificacao(ClassificacaoEvento.DEDUCCOMPLIRRF13, TipoValor.CALCULADO, inicioAnoBase, calculo.competencia, TipoProcessamento.DECIMO_TERCEIRO_SALARIO, SubTipoProcessamento.INTEGRAL)
                    desconto13SalarioDevidoReinteg += Funcoes.acumulaClassificacao(ClassificacaoEvento.DESC13SALDEVREINTG, TipoValor.CALCULADO, inicioAnoBase, calculo.competencia, TipoProcessamento.DECIMO_TERCEIRO_SALARIO, SubTipoProcessamento.INTEGRAL)
                }
            }
        }
        if (deducaoCompleta13IntegralAntecipado > 0) {
            suspender 'Já há dedução legal aplicada para o IRRF 13º salário em cálculo de décimo terceiro salário integral adiantado anterior neste ano. Estas folhas serão unificadas ao enviar para o eSocial'
        }
    }
    double baseDeducaoIrrf13 = Bases.valor(Bases.DEDUCIRRF13) + desconto13SalarioDevidoReinteg
    if (EncargosSociais.IRRF.deducaoSimplificadaIrrf <= baseDeducaoIrrf13) {
        suspender 'O valor da dedução simplificada é menor ou igual ao valor das deduções legais para o IRRF 13º salário. Por este motivo, deve ser aplicada a dedução legal para o IRRF 13º salário'
    }
    valorCalculado = EncargosSociais.IRRF.deducaoSimplificadaIrrf
}
Bases.compor(valorCalculado, Bases.IRRF13)
if (deducaoSimplificada13IntegralAntecipado > 0) {
    valorCalculado = 0
}
