package piuk.blockchain.android.ui.swap

import androidx.fragment.app.FragmentManager
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.ui.swap.homebrew.exchange.ExchangeMenuState
import piuk.blockchain.android.util.errorIcon

private fun ExchangeMenuState.ErrorType.icon(cryptoCurrency: CryptoCurrency, userTier: Int): Int =
    when (this) {
        ExchangeMenuState.ErrorType.TIER -> if (userTier == 2)
            R.drawable.vector_gold_swap_error else R.drawable.vector_silver_swap_error
        else -> cryptoCurrency.errorIcon()
    }

internal fun showErrorDialog(fragmentManager: FragmentManager, error: ExchangeMenuState.ExchangeMenuError) {
    val bottomSheetDialog = ErrorBottomDialog.newInstance(error.toContent())
    bottomSheetDialog.show(fragmentManager, "BottomDialog")
}

private fun ExchangeMenuState.ExchangeMenuError.toContent(): ErrorBottomDialog.Content =
    ErrorBottomDialog.Content(
        title = title,
        description = message,
        ctaButtonText = 0,
        dismissText = R.string.ok_cap,
        icon = errorType.icon(fromCrypto, tier)
    )