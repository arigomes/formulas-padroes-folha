//Este evento deve ser configurado para calcular por último (Guia Geral > Calcular por último)
if (!TipoMatricula.FUNCIONARIO.equals(matricula.tipo) && !TipoMatricula.AUTONOMO.equals(matricula.tipo)) {
    suspender "Este cálculo é executado apenas para funcionários ou autônomos"
}
if (!TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento) && !TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
    suspender "O evento deve ser calculado apenas em processamentos mensais ou rescisórios"
} else {
    if (SubTipoProcessamento.ADIANTAMENTO.equals(calculo.subTipoProcessamento)) {
        suspender "O evento não é calculado no subtipo de processamento 'adiantamento'"
    }
}
boolean possuiMultiploVinculo
boolean desconsideraRetencaoOutrasEmpresas
if (TipoMatricula.AUTONOMO.equals(matricula.tipo)) {
    if (autonomo.codESocial.equals("741")) {
        suspender "Não há desconto de contribuição previdenciária para autônomos da categoria MEI com o 'Código eSocial' igual a '741' informado na categoria do trabalhador"
    }
    if (autonomo.codESocial && autonomo.codESocial.startsWith('7')) {
        desconsideraRetencaoOutrasEmpresas = true
    }
}
possuiMultiploVinculo = matricula.possuiMultiploVinculo
if (Funcoes.possuiPrevidenciaFederal(matricula.tipo)) {
    double vvar = Lancamentos.valor(evento)
    if (vvar >= 0) {
        valorCalculado = vvar
    } else {
        double baseInssCompetencia
        double valorInssAnterior
        double valorInssBasesOutrasEmpresas
        //Verifica se há as folhas mensais ou rescisórias já calculadas anteriormente na competência
        def qntFolhasCompetencia = folhasPeriodo.buscaFolhas().sum(0, { it.folhaPagamento && [TipoProcessamento.MENSAL, TipoProcessamento.RESCISAO].contains(it.tipoProcessamento) && ![SubTipoProcessamento.ADIANTAMENTO].contains(it.subTipoProcessamento) ? 1 : 0})
        if (qntFolhasCompetencia > 0) {
            //Busca o valor da base INSSFER proporcional nas folhas
            baseInssCompetencia += Bases.valor(Bases.INSSFER)
            if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                baseInssCompetencia += Bases.valorCalculado(Bases.INSSFER, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                baseInssCompetencia += Bases.valorCalculado(Bases.INSSFER, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
            }
            //Base e Retenção de Outras Empresas
            //Busca o valor das bases INSSOUTRA (funcionário) e INSSOUTAUTO (autônomo) nas folhas
            baseInssCompetencia += Bases.valor(Bases.INSSOUTRA)
            baseInssCompetencia += Bases.valorCalculado(Bases.INSSOUTRA, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
            baseInssCompetencia += Funcoes.buscaBaseDeOutrosProcessamentos(Bases.INSSOUTRA)
            baseInssCompetencia += Bases.valor(Bases.INSSOUTAUTO)
            baseInssCompetencia += Bases.valorCalculado(Bases.INSSOUTAUTO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
            baseInssCompetencia += Funcoes.buscaBaseDeOutrosProcessamentos(Bases.INSSOUTAUTO)
            //Busca o valor descontado pelo código do evento de retenção INSS de outras empresas nas folhas
            //Se for um autônomo com categoria eSocial iniciada em 7, o valor não será considerado
            if (!desconsideraRetencaoOutrasEmpresas) {
                valorInssBasesOutrasEmpresas = + Eventos.valor(169) + Eventos.valorCalculado(169, TipoValor.CALCULADO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL) + Funcoes.buscaValorDeOutrosProcessamentos(169)
                valorInssAnterior += valorInssBasesOutrasEmpresas
            }
            //Bases e Descontos de INSS
            //Busca o valor da base INSS da matrícula nas folhas
            baseInssCompetencia += Bases.valor(Bases.INSS)
            if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                baseInssCompetencia += Bases.valorCalculado(Bases.INSS, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                baseInssCompetencia += Bases.valorCalculado(Bases.INSS, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
            }
            //Busca o valor da base INSS dos demais vínculos do servidor nas folhas
            if (possuiMultiploVinculo) {
                baseInssCompetencia += Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                    baseInssCompetencia += Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                    baseInssCompetencia += Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
                    if (SubTipoProcessamento.COMPLEMENTAR.equals(calculo.subTipoProcessamento)) {
                        baseInssCompetencia += Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL)
                        baseInssCompetencia += Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR)
                    }
                }
            }
            //Busca o valor descontado da matrícula pela classificação de INSS nas folhas
            valorInssAnterior += Eventos.valorCalculado(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            valorInssAnterior += Eventos.valorCalculado(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
            valorInssAnterior += Eventos.valorCalculado(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            valorInssAnterior += Eventos.valorCalculado(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
            //Busca o valor descontado dos demais vínculos do servidor pela classificação de INSS nas folhas
            if (possuiMultiploVinculo) {
                valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                if (SubTipoProcessamento.COMPLEMENTAR.equals(calculo.subTipoProcessamento)) {
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, calculo.tipoProcessamento, SubTipoProcessamento.INTEGRAL)
                }
                if (TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento)) {
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL)
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR)
                }
                if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
                }
            }
            //Valores pagos de Devolução de INSS
            //Busca o valor pago a matrícula pela classificação de DEVINSS nas folhas
            valorInssAnterior -= Eventos.valorCalculado(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            valorInssAnterior -= Eventos.valorCalculado(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
            valorInssAnterior -= Eventos.valorCalculado(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL, calculo.competencia)
            valorInssAnterior -= Eventos.valorCalculado(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR, calculo.competencia)
            //Busca o valor pago aos demais vínculos do servidor pela classificação de DEVINSS nas folhas
            if (possuiMultiploVinculo) {
                valorInssAnterior -= Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                if (SubTipoProcessamento.COMPLEMENTAR.equals(calculo.subTipoProcessamento)) {
                    valorInssAnterior -= Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, calculo.tipoProcessamento, SubTipoProcessamento.INTEGRAL)
                }
                if (TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento)) {
                    valorInssAnterior -= Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL)
                    valorInssAnterior -= Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR)
                }
                if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                    valorInssAnterior -= Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                    valorInssAnterior -= Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.DEVINSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
                }
            }
        } else {
            //Busca o valor da base INSSFER proporcional na folha atual
            baseInssCompetencia += Bases.valor(Bases.INSSFER)
            //Busca o valor da base INSSOUTRA (funcionário) e INSSOUTAUTO (autônomo) nas folhas atual e de férias
            baseInssCompetencia += Bases.valor(Bases.INSSOUTRA)
            baseInssCompetencia += Bases.valorCalculado(Bases.INSSOUTRA, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
            baseInssCompetencia += Bases.valor(Bases.INSSOUTAUTO)
            baseInssCompetencia += Bases.valorCalculado(Bases.INSSOUTAUTO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
            //Busca o valor descontado pelo código do evento de retenção INSS de outras empreas nas folhas atual e de férias
            //Se for um autônomo com categoria eSocial iniciada em 7, o valor não será considerado
            if (!desconsideraRetencaoOutrasEmpresas) {
                valorInssBasesOutrasEmpresas = Eventos.valor(169) + Eventos.valorCalculado(169, TipoValor.CALCULADO, TipoProcessamento.FERIAS, SubTipoProcessamento.INTEGRAL)
                valorInssAnterior += valorInssBasesOutrasEmpresas
            }
            //Busca o valor da base INSS na folha atual
            baseInssCompetencia += Bases.valor(Bases.INSS)
            //Busca o valor descontado da matrícula pela classificação de INSS na folha atual
            valorInssAnterior += folha.eventos.sum(0,{ClassificacaoEvento.INSS.equals(it.classificacao) ? it.valor : 0 })
            if (possuiMultiploVinculo) {
                //Busca o valor da base INSS dos demais vínculos do servidor nas folhas
                baseInssCompetencia += Funcoes.getValorBaseMultiplosVinculos(Bases.INSS, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                //Busca o valor descontado dos demais vínculos do servidor pela classificação de INSS nas folhas
                valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, calculo.tipoProcessamento, calculo.subTipoProcessamento)
                if (TipoProcessamento.MENSAL.equals(calculo.tipoProcessamento)) {
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.INTEGRAL)
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.RESCISAO, SubTipoProcessamento.COMPLEMENTAR)
                }
                if (TipoProcessamento.RESCISAO.equals(calculo.tipoProcessamento)) {
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.INTEGRAL)
                    valorInssAnterior += Eventos.valorCalculadoMultiplosVinculos(ClassificacaoEvento.INSS, TipoValor.CALCULADO, TipoProcessamento.MENSAL, SubTipoProcessamento.COMPLEMENTAR)
                }
            }
        }
        double tetoInss = EncargosSociais.RGPS.buscaMaior(1)
        if (baseInssCompetencia > tetoInss) {
            baseInssCompetencia = tetoInss
        }
        if (TipoMatricula.FUNCIONARIO.equals(matricula.tipo)) {
            double taxaBaseInssCompetencia = Numeros.trunca(baseInssCompetencia, 2)
            if (funcionario.conselheiroTutelar) {
                valorReferencia = 11
            } else {
                valorReferencia = EncargosSociais.RGPS.buscaContribuicao(taxaBaseInssCompetencia, 2)
            }
        } else {
            if (EncargosSociaisFpas.ENTIDADE_BENEFICENTE.equals(EncargosSociais.codigoFpas) && (TipoMatricula.AUTONOMO.equals(matricula.tipo))) {
                valorReferencia = 20
            } else {
                valorReferencia = EncargosSociais.RGPS.buscaMaior(2)
                if (TipoMatricula.AUTONOMO.equals(matricula.tipo)) {
                    valorReferencia = 11
                }
            }
        }
        double inssValorAtualizado
        double valorDiferencaInssAnterior
        if (Funcoes.inicioCompetencia() >= Datas.data(2020, 3, 1) && !TipoMatricula.AUTONOMO.equals(matricula.tipo) && !funcionario.conselheiroTutelar) {
            inssValorAtualizado = Numeros.arredonda(Funcoes.calculoProgressivoINSS(baseInssCompetencia), 2)
        } else {
            inssValorAtualizado = Numeros.trunca((baseInssCompetencia * valorReferencia) / 100, 2)
        }
        inssValorAtualizado = Numeros.trunca(inssValorAtualizado, 2)
        // retirando valor de inss de outras empresas para verificar a existência de inss já pago na competência para fazer a devolução caso seja necessário
        valorDiferencaInssAnterior = valorInssAnterior - valorInssBasesOutrasEmpresas
        if (valorInssBasesOutrasEmpresas > inssValorAtualizado && valorDiferencaInssAnterior <= 0) {
            suspender "A retenção efetuada de I.N.S.S. de outras empresas é maior que o teto estabelecido na manutenção de estabelecimento da entidade. Para estes casos, a devolução de I.N.S.S. não será calculada, tendo em vista que o valor ultrapasse o limite estabelecido"
        }
        if (valorInssAnterior > inssValorAtualizado) {
            valorCalculado = valorInssAnterior - inssValorAtualizado
        } else {
            suspender "Não há valor de devolução a ser gerado nesta competência"
        }
    }
}
