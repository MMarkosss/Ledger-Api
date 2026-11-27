# language: pt

Funcionalidade: Jornada do ledger
  Cenário: criar contas, depositar, transferir e conciliar os dois extratos
    Dado que criei uma conta de origem com saldo "100.00"
    E criei uma conta de destino
    Quando transfiro "30.00" da origem para o destino com a chave "e2e-transfer"
    E repito a mesma transferência com a chave "e2e-transfer"
    Então a conta de origem tem saldo "70.00"
    E a conta de destino tem saldo "30.00"
    E o extrato da origem mostra o depósito e a transferência em ordem decrescente
    E o extrato do destino mostra a transferência recebida
    E os extratos refletem o mesmo par de transferência
    E a cadeia de balance_after de cada extrato é contínua
    E o saldo final de cada conta é igual ao balance_after mais recente
    E os lançamentos do extrato têm data de criação
