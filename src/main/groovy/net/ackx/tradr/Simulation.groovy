package net.ackx.tradr

import groovy.util.logging.Slf4j
import groovyx.gpars.GParsPool

/**
 * Perform a trading simulation based on different scenarios.
 *
 * @author @YouriAckx
 */
@Slf4j
@SuppressWarnings("GrMethodMayBeStatic") // thanks but no thanks
class Simulation {
    /** Trading commission */
    private static final PERCENT_COMMISSION = 0.02d

    /** What fraction of the existing currency do we want to spend? */
    private static final TRADING_FACTOR = 1.5d

    /** An arbitrary starting wallet */
    def startingWallet = new Wallet(1.00 as BigDecimal, 40.00 as BigDecimal)

    /**
     * Perform a simulation for a given scenario.
     * <p/>
     * Starts from a {@link #startingWallet}.
     *
     * @param data All data
     * @param macdParams MACD parameters
     * @param aggregationFactor Aggregration factor
     * @param damper Damper
     *
     * @return Resulting wallet
     */
    private Wallet _simulate(List<Data> data, TraderParams traderParams, int aggregationFactor) {
        def wallet = Wallet.of(startingWallet)
        data = Aggregator.aggregate(data, aggregationFactor)
        println "SIMULATION: aggregation=$aggregationFactor to ${data.size()} ticks, trader=$traderParams"

        for (int i = 0; i < data.size(); i++) {
            def currentData = data[i]
            def rate = currentData.closing

            def shortened = data.subList(0, i + 1)
            def trader = new Trader(traderParams)
            def action = trader.trade(shortened)

            if (action == TradingAction.BUY) {
                def spendEuros = wallet.getEuros() / TRADING_FACTOR
                def wo = new Operation(currentData.dateTime, spendEuros/rate, currentData.closing, PERCENT_COMMISSION)
                wallet.addOperation(wo)
            } else if (action == TradingAction.SELL) {
                def sellStock = - wallet.getStock() / TRADING_FACTOR
                def wo = new Operation(currentData.dateTime, sellStock, currentData.closing, PERCENT_COMMISSION)
                wallet.addOperation(wo)
            }
        }

        /*
        println("\nOperations:")
        wallet.walletOperations.each { println it }
        println("${wallet.walletOperations.size()} operations")

        def todayRate = data[-1].closing
        println("\nStart:\n$startingWallets => EUR ${startingWallets.toEuros(todayRate)}")
        println("\nFinal wallet:\n$wallet => EUR ${wallet.toEuros(todayRate)}")
        println("\n===========================================================\n")
        */

        wallet
    }

    /** Some selected scenarios */
    private def selectedScenarios() {
        def macdClassical = new MacdParams(a: 12, b: 26, c: 9)
        def macdSharp = new MacdParams(a: 3, b: 5, c: 3)
        def macdSpecial = new MacdParams(a: 6, b: 10, c: 6)

        [
//                [macdClassical, 1,  0.5d],
//                [macdClassical, 1,  0.2d],
                [macdClassical, 5,  0.5d],
                [macdClassical, 5,  0.1d],
                [macdClassical, 20, 0.5d],
                [macdClassical, 20, 0.2d],
                [macdClassical, 20, 0.1d],
                [macdClassical, 20, 0.05d],
                [macdClassical, 20, 0.01d],
                [macdClassical, 60, 0.5d],
                [macdClassical, 60, 0.2d],
                [macdClassical, 60, 0.1d],
                [macdClassical, 60, 0.05d],
                [macdClassical, 60, 0.01d],
                [macdSharp,     60, 0.5d],
                [macdSharp,     60, 0.2d],
                [macdSharp,     60, 0.1d],
                [macdSharp,     60, 0.05d],
                [macdSharp,     60, 0.01d],
                [macdSpecial,   60, 0.2d],
                [macdSpecial,   60, 0.1d],
        ]
    }

    /**
     * All available scenarios.
     * Many scenarios do not make sense, for instance small or no aggregates with small damper,
     * as it results in excessive buy/sell operations.
     */
    @SuppressWarnings("GroovyUnusedDeclaration")
    private def allScenarios() {
        def macdClassical = new MacdParams(a: 12, b: 26, c: 9)
        def macdSharp = new MacdParams(a: 3, b: 5, c: 3)
        def macdSpecial = new MacdParams(a: 6, b: 10, c: 6)

        def scenarios = []
        [macdClassical, macdSharp, macdSpecial].each { macd ->
            [1, 5, 20, 60].each { aggragation ->
                [0.01d, 0.05d, 0.1d, 0.2d, 0.5d, 1d].each { delta ->
                    scenarios << [macd, aggragation, delta]
                }
            }
        }

        scenarios
    }

    /**
     * Simulate trading
     * @param csvPath Path to CSV input file
     */
    void simulate(String csvPath) {
        def showResults = { Map<String, Wallet> sWallets, BigDecimal converstionRate, boolean showDetails ->
            sWallets
                    .sort { a, b -> b.value.toEuros(converstionRate) <=> a.value.toEuros(converstionRate) }
                    .each { String s, Wallet w ->
                        printf("%-35sEUR %-10s%d ops%n", s, w.toEuros(converstionRate), w.operations.size())
                        if (showDetails) w.operations.each { println "    $it" }
                    }
        }

        println "Loading data"
        def rawData = new RawData()
        List<Data> data = rawData.loadFromCsv(csvPath)
        println "Got ${data.size()} data"

        // Some selected scenarios, or all of them
        // (that will be slow, and many scenarios do not make sense,
        // for instance small or no aggregates with small damper,
        // as it results in excessive buy/sell operations
        def scenarios = selectedScenarios()
        // def scenarios = allScenarios()

        def sWallet = [:] as Map<String, Wallet>
        GParsPool.withPool {
            scenarios.eachParallel { List scenario ->
                def macdParam = scenario[0] as MacdParams
                def aggregates = scenario[1] as int
                def damper = scenario[2] as double
                def s = "$macdParam A=$aggregates d=$damper"
                sWallet[s] = _simulate(data, new TraderParams(macdParam, damper), aggregates)
            }
        }

        // Add a dummy "do nothing" wallet without operations to compare our traders
        // with a passive actor
        sWallet['Do Nothing'] = startingWallet

        // Liquidate assets (stock converted to euros),
        // and display result, based on the last closing rate
        def todayRate = data[-1].closing
        println "\n*** FINAL RESULTS ***\n"
        println "Started with wallets worth EUR ${startingWallet.toEuros(data[0].closing)}"
        println "Exit rate: $todayRate EUR/XYZ\n"
        showResults(sWallet, todayRate, true)
        20.times { print '=' }
        println "\n\nStarted with wallets worth EUR ${startingWallet.toEuros(data[0].closing)}"
        println "Exit rate: $todayRate EUR/XYZ\n"
        showResults(sWallet, todayRate, false)
    }

    static void main(String[] args) {
        new Simulation().simulate(args[0])
    }
}
