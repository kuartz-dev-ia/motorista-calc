# Motorista Calc

App Android que lê a tela do app de corrida (Uber, 99 etc.) via Accessibility Service,
detecta quando aparece uma tela de "nova corrida", extrai valor/distância/tempo e calcula
se vale a pena aceitar (R$/km, R$/hora, lucro estimado descontando combustível).

## Como abrir o projeto

1. Instale o **Android Studio** (versão Koala ou mais recente).
2. `File > Open` e selecione a pasta `MotoristaCalc`.
3. Deixe o Gradle sincronizar (baixa as dependências automaticamente).
4. Rode em um celular físico Android 8.0+ (API 26+) conectado via USB com depuração
   ativada. **Não funciona bem no emulador** porque você precisa ter o app de corrida
   de verdade instalado para testar.

## Primeiro uso no celular

1. Abra o app "Motorista Calc".
2. Toque em **"1. Ativar serviço de acessibilidade"** → nas configurações do Android,
   encontre "Motorista Calc" na lista e ative.
3. Toque em **"2. Permitir exibir sobre outros apps"** → conceda a permissão de overlay.
4. Ajuste os parâmetros (R$/km mínimo, R$/hora mínimo, consumo do carro, preço do
   combustível) e toque em "Salvar configurações".
5. Abra o app do motorista normalmente (Uber Driver, 99, etc). Quando uma tela de
   corrida aparecer, um aviso colorido vai surgir no topo da tela com o cálculo.

## Status do parser (`TriggerPatterns.kt`)

Os regex foram ajustados e testados contra 15 prints reais (Uber Driver e 99
Motorista, telas "Dinheiro" e "Exclusivo"/Comfort/Black, com e sem tarifa
dinâmica/surge). Cobrem:

- Valor total em R$, com ou sem espaço (`R$99,40` e `R$ 103,55`)
- Valor por km já exibido pelo app (usado como conferência do valor calculado)
- Multiplicador de tarifa dinâmica (`2,1x`, `1,8x`)
- Duração em 3 formatos: `74min`, `51 minutos`, `1 h e 45 min`
- Distância em km ou metros (converte metros automaticamente)
- Tags: "Viagem longa", "Verificado"
- Avaliação do passageiro (`4,20 · 5 corridas` ou `4,96 (900)`)

**Limitação conhecida:** avaliação sem contagem ao lado (ex: `★ 4,89` sozinho,
visto na tela de navegação com a corrida já em andamento) não é capturada — o
regex exige `(N)` ou `· N corridas` para evitar pegar outros números da tela por
engano. Se isso for importante pra você, me avisa que ajusto.

**R$/km calculado vs. exibido:** o app já mostra o R$/km pronto, mas o motor
calcula o seu próprio também (usando a distância da perna final). Pode haver
pequena diferença por arredondamento — isso vira uma conferência cruzada útil.

## O que falta ajustar (próximos passos)

1. **`packageNames` no `accessibility_service_config.xml`** — hoje está com nomes
   de pacote "chutados". Rode `adb shell pm list packages | grep -i uber` (ou 99,
   cabify) com o app instalado para pegar o nome real e eu atualizo.
2. **Salvar o print/histórico** — o método `capturarPrint()` já chama a API de
   screenshot, mas o salvamento do bitmap em disco ficou como próximo passo (depende
   de você querer guardar local, na nuvem, etc).
3. **Ícone do app** — falta a pasta `mipmap` com o ícone; o Android Studio gera um
   ícone padrão automaticamente se você não adicionar um.
4. **Testes em campo** — como cada app de corrida muda a UI com frequência, o regex
   pode quebrar quando o app atualiza. Vale ter um "modo debug" que mostra o texto
   bruto capturado, pra você me mandar quando algo parar de funcionar.
5. **Avaliação sem contagem** (ver limitação acima) — ajustar se fizer diferença
   pra você.

## Importante: Termos de Uso

Ler a tela de apps como Uber/99 programaticamente não é bloqueado pelo Android, mas
pode contrariar os Termos de Uso desses apps (não do sistema operacional). Isso é
uso pessoal/local no seu próprio celular, mas vale estar ciente caso pense em publicar
esse app na Play Store no futuro — o Google exige justificativa específica para uso de
Accessibility Service em apps públicos.
