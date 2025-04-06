import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _rsi = MutableLiveData<String>()
    val rsi: LiveData<String> get() = _rsi

    private val _macd = MutableLiveData<String>()
    val macd: LiveData<String> get() = _macd

    private val _volumeAnomaly = MutableLiveData<String>()
    val volumeAnomaly: LiveData<String> get() = _volumeAnomaly

    private val _marketGrowth = MutableLiveData<String>()
    val marketGrowth: LiveData<String> get() = _marketGrowth

    private val _simulationProfit = MutableLiveData<String>()
    val simulationProfit: LiveData<String> get() = _simulationProfit

    fun updateIndicators(
        rsiValue: Double,
        macdValue: Double,
        volumeAnomalyValue: Double,
        marketGrowthPercent: Double,
        simulationProfitPercent: Double
    ) {
        _rsi.value = String.format("%.2f", rsiValue)
        _macd.value = String.format("%.2f", macdValue)
        _volumeAnomaly.value = String.format("%.2f", volumeAnomalyValue)
        _marketGrowth.value = String.format("%.2f%%", marketGrowthPercent)
        _simulationProfit.value = String.format("%.2f%%", simulationProfitPercent)
    }
}
