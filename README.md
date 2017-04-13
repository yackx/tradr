# tradr

A feeble and vain attempt to perform automated trading, and get rich.

This software was designed to work with [Ether (ETH)](https://en.wikipedia.org/wiki/Ethereum), a cryptocurrency,
but it can as well work with [Bitcoin (BTC)](https://en.wikipedia.org/wiki/Bitcoin) or actually, with any plain
old stock.
 
By "work", I actually mean that it does something, but it performed poorly as far as winning money goes.

# Concepts

`tradr` is a simulator performing trading decisions based on past data.
It runs several scenarios with different parameters.

## MACD

It is solely based on [MACD - Moving Average Convergence Divergence](http://www.investopedia.com/terms/m/macd.asp),
an old trading indicator that relies on [exponential moving averages EMA](http://www.investopedia.com/terms/e/ema.asp)
and that can be summarized as follows (assuming you have one quote per day):

* Compute the 12 days EMA
* Compute the 26 days EMA
* MACD = EMA 12  - EMA 26
* Signal = EMA 9 of the MACD

The crossing of the MACD and of the signal line gives buy and sell signals (*bearish* and *bullish*).

The example above is a `MACD(12, 26, 9)`, but other variations exist, such as `MACD(3, 5, 3)`.

## Damper

To avoid "jumping the gun" and triggering buy and sell decisions as soon as the lines cross,
a `damper` factor has been added. Remember, trading is not free and a trading platform will typically
apply a 1 or 2% commission fee to each transaction.

A useful value depends on the the data granularity (smaller granularity calls for smaller damper value).

The algorithm waits for MACD vs signal to diverge sufficiently before triggering an action.
Side effect: on a speculative market, you may be missing opportunities due to the induced delay.
And for ETH and BTC, this can happen very fast on a quiet market.

## Aggregate

Daily stock prices may be too crude for trading ETH and BTC. But relying on minute per minute prices
may not be desirable neither. We are not doing high-frequency trading.
In order to test on different granularities, this program
contains a utility class to aggregate data, for instance from 1 per minute to 1 every 5 minutes, or 3 per hours.

# Installation

You need [Groovy 2.4.7](http://groovy-lang.org/) and [Gradle 3.2.1](https://gradle.org/),
although it is likely to work with several minor prior or future versions.
A very convenient way to install them is the excellent [sdkman](http://sdkman.io/).
You can work without gradle if you run the program from your IDE or the command line.

# Usage

Two main classes you can run:
* `RawData` to convert and concatenate raw JSON data to CSV used by the simulator and
* `Simulation` to actually run the simulation, based on a CSV input file.

Gradle tasks `aggregate` and `simulate` wrap these two classes.

## 1. Collecting data

First, you need to collect stock data for the simulator.
For ETH, this can be found via the [cryptocompare.com API](https://min-api.cryptocompare.com/).
Unfortunately, there is no API that I know of that allows you to collect data beyond 24 hours in the past
with a fine granularity. It's either fine granularity (to the minute), but limited to one day,
or you can go back months, but with one quote per day. Same thing for BTC.

This is not very helpful for our simulator. A workaround is to collect fine-grained data at least once a day.
I have a crontab running to access
[this URL](https://min-api.cryptocompare.com/data/histominute?fsym=ETH&tsym=EUR&aggregate=1&limit=2000&e=CCCAGG).

Let's grab a smaller portion of data for illustration purpose (output slightly formatted):

```
$ curl "https://min-api.cryptocompare.com/data/histominute?fsym=ETH&tsym=EUR&aggregate=1&limit=4&e=CCCAGG"
{"Response":"Success","Type":100,"Aggregated":false,"Data":[
  {"time":1492019820,"close":41.61,"high":41.63,"low":41.61,"open":41.63,"volumefrom":43.62,"volumeto":1814.91},
  {"time":1492019880,"close":41.61,"high":41.61,"low":41.61,"open":41.61,"volumefrom":0,"volumeto":0},
  {"time":1492019940,"close":41.61,"high":41.61,"low":41.61,"open":41.61,"volumefrom":0,"volumeto":0},
  {"time":1492020000,"close":41.61,"high":41.61,"low":41.61,"open":41.61,"volumefrom":0,"volumeto":0},
  {"time":1492020060,"close":41.61,"high":41.61,"low":41.61,"open":41.61,"volumefrom":0,"volumeto":0}],
"TimeTo":1492020060,"TimeFrom":1492019820,"FirstValueInArray":true,"ConversionType":"direct"}

```

By calling this URL at least once a day for some time, you'll get enough relevant material to perform
a simulation. Samples are gathered under the `data` folder.

Then, you need to transform these files into something useable by the simulator, namely a neat CSV.
`RawData` takes care of that operation, eliminating overlaps and filling gaps:

```
$ gradle aggregate
```

This will create a file `stock-history.csv` in the `data` folder:

```
$ head -5 data/stock-history.csv
#dateTime,open,close,low,high,volumeFrom,volumeTo
2017-03-17T13:34:00,43.01,42.96,42.74,43.06,207.53,8927.73
2017-03-17T13:35:00,42.96,42.74,42.74,42.96,94.26,4036.05
2017-03-17T13:36:00,42.74,42.95,42.74,42.95,135.49,5812.52
2017-03-17T13:37:00,42.95,42.92,42.75,42.95,8.31,356.82
```

## 2. Running the simulation

Given a properly generated CSV file, run the simulation:

```
$ gradle simulate
```

The simulator offers several plausible scenarios with variations of:
* MACD
* damper
* aggregate

It runs in parallel thanks to `GPars`. The simulator start with a fixed Wallet of EUR and XYZ
(representing any stock, so here ETH), and applies a trading commission.

You can tweak the code to run all possible combinations of parameters,
but this will include irrelevant combinations as well
(for instance, a short and aggressive MACD with a low damper value). 

Example of output:

```
Got 26186 data
SIMULATION: aggregation=60 to 436 ticks, trader=net.ackx.tradr.TraderParams(MACD(12, 26, 9), 0.01)
SIMULATION: aggregation=60 to 436 ticks, trader=net.ackx.tradr.TraderParams(MACD(12, 26, 9), 0.1)
SIMULATION: aggregation=20 to 1309 ticks, trader=net.ackx.tradr.TraderParams(MACD(12, 26, 9), 0.5)
SIMULATION: aggregation=60 to 436 ticks, trader=net.ackx.tradr.TraderParams(MACD(3, 5, 3), 0.5)
...

*** FINAL RESULTS ***

Started with wallets worth EUR 82.96
Exit rate: 42.11 EUR/XYZ

MACD(12, 26, 9) A=5 d=0.5          EUR 87.09     1 ops
    2017-03-19T01:09:00 XYZ 0.77 @ 34.5500 EUR/XYZ
MACD(12, 26, 9) A=20 d=0.5         EUR 87.05     1 ops
    2017-03-19T02:14:00 XYZ 0.77 @ 34.6000 EUR/XYZ
MACD(12, 26, 9) A=60 d=0.5         EUR 86.23     1 ops
    2017-03-19T03:34:00 XYZ 0.75 @ 35.4900 EUR/XYZ
MACD(3, 5, 3) A=60 d=0.5           EUR 82.11     0 ops
Do Nothing                         EUR 82.11     0 ops
MACD(12, 26, 9) A=60 d=0.1         EUR 77.32     16 ops
    2017-03-19T01:34:00 XYZ 0.77 @ 34.6000 EUR/XYZ
    2017-03-20T19:34:00 XYZ -1.17 @ 39.5600 EUR/XYZ
    2017-03-22T10:34:00 XYZ -0.39 @ 37.7900 EUR/XYZ
    2017-03-22T22:34:00 XYZ 1.29 @ 37.7100 EUR/XYZ
...

Started with wallets worth EUR 82.96
Exit rate: 42.11 EUR/XYZ

MACD(12, 26, 9) A=5 d=0.5          EUR 87.09     1 ops
MACD(12, 26, 9) A=20 d=0.5         EUR 87.05     1 ops
MACD(12, 26, 9) A=60 d=0.5         EUR 86.23     1 ops
MACD(3, 5, 3) A=60 d=0.5           EUR 82.11     0 ops
Do Nothing                         EUR 82.11     0 ops
MACD(12, 26, 9) A=60 d=0.1         EUR 77.32     16 ops
MACD(3, 5, 3) A=60 d=0.2           EUR 76.64     5 ops
MACD(12, 26, 9) A=60 d=0.05        EUR 76.57     20 ops
MACD(12, 26, 9) A=60 d=0.2         EUR 74.90     13 ops
MACD(6, 10, 6) A=60 d=0.2          EUR 74.75     10 ops
MACD(12, 26, 9) A=60 d=0.01        EUR 67.54     25 ops
MACD(12, 26, 9) A=20 d=0.2         EUR 65.61     23 ops
MACD(3, 5, 3) A=60 d=0.1           EUR 61.91     28 ops
MACD(6, 10, 6) A=60 d=0.1          EUR 57.87     32 ops
MACD(12, 26, 9) A=20 d=0.05        EUR 46.68     65 ops
MACD(12, 26, 9) A=20 d=0.1         EUR 46.39     51 ops
MACD(3, 5, 3) A=60 d=0.05          EUR 45.48     69 ops
MACD(12, 26, 9) A=5 d=0.1          EUR 38.02     86 ops
MACD(12, 26, 9) A=20 d=0.01        EUR 34.14     90 ops
MACD(3, 5, 3) A=60 d=0.01          EUR 32.07     111 ops
```

# Limitation

So, the run above shows that you won't necessarily get more by running a (dumb) algo
rather than doing nothing. You are even likely to lose money. Remember than an algorithm performing well on
a given set of data may not perform well on another set.

Have fun improving it!

# License

![GNU GPL v3](https://www.gnu.org/graphics/gplv3-88x31.png)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.