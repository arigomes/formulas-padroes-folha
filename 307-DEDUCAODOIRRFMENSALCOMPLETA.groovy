if (TipoMatricula.AUTONOMO.equals(matricula.tipo) && autonomo.codESocial.equals("741")) {
    suspender "Não há deduções de IRRF para autônomos da categoria MEI com o 'Código eSocial' igual a '741' informado na categoria do trabalhador"
}
if (TipoProcessamento.DECIMO_TERCEIRO_SALARIO.equals(calculo.tipoProcessamento)) {
    suspender "Este evento não é calculado em processamentos de décimo terceiro"
}
if (!calculo.rra && TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento)) {
    suspender 'Este evento não é calculado em processamentos vinculados a pagamentos anteriores sem RRA'
}
if (folha.folhaPagamento) {
    def vvar = Lancamentos.valor(evento)
    if (vvar >= 0) {
        valorCalculado = vvar
    } else {
        double deducaoSimplificadaAnterior
        double deducaoCompletaAnterior
        if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
            deducaoSimplificadaAnterior += Eventos.valorCalculado(ClassificacaoEvento.DEDUCSIMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            deducaoCompletaAnterior += Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL, calculo.competencia)
        }
        if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
            deducaoSimplificadaAnterior += Eventos.valorCalculado(ClassificacaoEvento.DEDUCSIMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            deducaoSimplificadaAnterior += Eventos.valorCalculado(ClassificacaoEvento.DEDUCSIMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
            deducaoCompletaAnterior += Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            deducaoCompletaAnterior += Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
        }
        double baseDeducaoIrrf = Bases.valor(Bases.DEDUCIRRFMES)
        double baseDeducaoIrrfMultiplosVinculos = Bases.valorCalculadoMultiplosVinculos(Bases.DEDUCIRRFMES, calculo.tipoProcessamento, calculo.subTipoProcessamento);
        if (deducaoSimplificadaAnterior > 0) {
            if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
                baseDeducaoIrrf += Bases.valorCalculado(Bases.DEDUCIRRFMES, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
            }
            if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                baseDeducaoIrrf += Bases.valorCalculado(Bases.DEDUCIRRFMES, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                baseDeducaoIrrf += Bases.valorCalculado(Bases.DEDUCIRRFMES, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
            }
        }
        if (!matricula.possuiMultiploVinculo && !calculo.rra) {
            if (EncargosSociais.IRRF.deducaoSimplificadaIrrf > baseDeducaoIrrf && !deducaoCompletaAnterior) {
                suspender 'O valor da dedução dos descontos legais é menor do que o valor da dedução simplificada do IRRF. Por este motivo, deve ser aplicada a dedução simplificada'
            }
        }
        if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento) || TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
            baseDeducaoIrrf -= deducaoSimplificadaAnterior
        }
        double totaBaseDeducaoIrrf = (baseDeducaoIrrf + baseDeducaoIrrfMultiplosVinculos);
        double valorDeducaoIrrf = 0;
        if(totaBaseDeducaoIrrf >= EncargosSociais.IRRF.deducaoSimplificadaIrrf) {
            valorDeducaoIrrf = totaBaseDeducaoIrrf;
            double valorCalculadoMultiplosVinculos = Eventos.valorCalculadoMultiplosVinculos(evento.codigo, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento) +
                    Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEDUCSIMPLIRRFMENSAL, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento);
            valorDeducaoIrrf -= valorCalculadoMultiplosVinculos;
        }
        //Na complementar de RRA não é calculada dedução completa e com isso precisamos zerar as diferenças vindas de processamentos anteriores, por isso abaixo está sendo somado o valor calculado dos eventos com classificacao DEDUCCOMPLIRRFMENSAL
        if(TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento) && SubTipoProcessamento.COMPLEMENTAR.equals(calculo.subTipoProcessamento) && calculo.rra) {
            valorCalculado = Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL, calculo.competencia) +
                    Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
        } else if(TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento) && SubTipoProcessamento.COMPLEMENTAR.equals(calculo.subTipoProcessamento) && calculo.rra) {
                valorCalculado = Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL, calculo.competencia) +
                        Eventos.valorCalculado(ClassificacaoEvento.DEDUCCOMPLIRRFMENSAL, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
        } else if (TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento) && calculo.rra) {
            valorCalculado = Eventos.valorRra(ClassificacaoEvento.INSS) + Eventos.valorRra(ClassificacaoEvento.PREVEST) + Eventos.valorRra(ClassificacaoEvento.FUNDPREV) +
                             Eventos.valorRra(ClassificacaoEvento.FUNDFIN) + Eventos.valorRra(ClassificacaoEvento.FUNDASS)
            valorCalculado += Eventos.valorRra(ClassificacaoEvento.INSS13SAL) + Eventos.valorRra(ClassificacaoEvento.PREVESTDECSAL) + Eventos.valorRra(ClassificacaoEvento.FUNDPREV13SAL) +
                    Eventos.valorRra(ClassificacaoEvento.FUNDFIN13SAL) + Eventos.valorRra(ClassificacaoEvento.FUNDASS13SAL)
        } else {
            valorCalculado = valorDeducaoIrrf;
        }
    }
    if (TipoProcessamento.FERIAS.equals(calculo.tipoProcessamento)) {
        Bases.compor(valorCalculado, Bases.IRRFFER)
    }
    if (TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento) || TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
        Bases.compor(valorCalculado, Bases.IRRF)
    }
    if (TipoProcessamento.PAGAMENTO_ANTERIOR.equals(calculo.tipoProcessamento) && calculo.rra) {
        Bases.compor(valorCalculado, Bases.IRRFRRA)
    }
}
