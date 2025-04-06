# 📊 Crypto Scalping Simulator App

A real-time crypto signal detection and simulation app built in Kotlin for Android. This app helps users monitor crypto markets, detect strong trading signals based on technical indicators, and simulate buy/sell decisions with auto-exit strategies.

---

## 🚀 Features

- ✅ **Real-time Signal Detection** from Binance API
- 🔄 **Foreground Service** for continuous signal monitoring
- 📈 **Technical Indicator Analysis**:  
  - EMA(9/21) crossover  
  - RSI(7)  
  - ATR(14)  
  - Candle body ratio  
  - Volume anomaly
- 🧠 **Conservative Signal Filtering**:
  - Only shows `BUY STRONG`, `SELL STRONG`, and `SELL MOMENTUM`
- 📉 **Auto Exit Logic**:
  - Profit target: **1.5%**
  - Stop loss: **1%**
  - Max hold time: **10 minutes**
- 🧪 **Simulated Buy/Sell** with result logging
- 🔔 **Educational Notifications** with signal explanation and action suggestion
- 🧭 **Market Activity Scanner**:
  - Only starts signal detection when market is trending (non-sideways)

---

## 🛠 Architecture Overview

- `MainActivity`: Main UI screen with toggle switches and action buttons
- `MarketScannerService`: Background service to detect market activity
- `CryptoSignalService`: Foreground service to monitor and analyze trading signals
- `IndicatorAnalyzer`: Class that handles all indicator logic and decision making
- Broadcast receivers update the UI in real-time

---

## 📱 UI Overview

| Component         | Description                                        |
|------------------|----------------------------------------------------|
| Spinner Koin      | Select crypto symbol (e.g. BTCUSDT, ETHUSDT)       |
| Spinner Interval  | Select interval (1m, 3m, 5m, 15m)                  |
| Switches          | Toggle Market Scanner and Signal Service          |
| Simulasi Beli     | Start a simulated "buy" and monitor conditions    |
| Hentikan Simulasi | Stop simulation, log result                       |
| Indicator Display | RSI, MACD, Volume Anomaly, Market Range, Profit   |

---

## 📸 Screenshot

*(Tambahkan screenshot aplikasi jika ada untuk tampilan MainActivity, notifikasi sinyal, dll.)*

---

## 🔧 How to Build

1. Clone this repo:
   ```bash
   git clone https://github.com/yourusername/trading-simulator-app.git
