package piuk.blockchain.android.ui.sell

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.service.TierService
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class BuySellFlowNavigator(
    private val simpleBuyModel: SimpleBuyModel,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val eligibilityProvider: SimpleBuyEligibilityProvider,
    private val tierService: TierService
) {
    fun navigateTo(selectedAsset: AssetInfo? = null): Single<BuySellIntroAction> =
        simpleBuyModel.state.firstOrError().flatMap { state ->
            val cancel = if (state.orderState == OrderState.PENDING_CONFIRMATION)
                custodialWalletManager.deleteBuyOrder(
                    state.id
                        ?: throw IllegalStateException("Pending order should always have an id")
                ).onErrorComplete()
            else Completable.complete()
            val isGoldButNotEligible = tierService.tiers()
                .zipWith(
                    eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true)
                ) { tier, eligible ->
                    tier.isApprovedFor(KycTierLevel.GOLD) && !eligible
                }

            cancel.thenSingle {
                Singles.zip(
                    custodialWalletManager.isCurrencySupportedForSimpleBuy(currencyPrefs.selectedFiatCurrency),
                    custodialWalletManager.getSupportedFiatCurrencies(),
                    isGoldButNotEligible
                ) { currencySupported, supportedFiats, isGoldButNotEligible ->
                    decideNavigationStep(currencySupported, selectedAsset, isGoldButNotEligible, state, supportedFiats)
                }
            }
        }

    private fun decideNavigationStep(
        currencySupported: Boolean,
        selectedAsset: AssetInfo?,
        isGoldButNotEligible: Boolean,
        state: SimpleBuyState,
        supportedFiats: List<String>
    ) = if (currencySupported) {
        selectedAsset?.let {
            BuySellIntroAction.StartBuyWithSelectedAsset(it, state.hasPendingBuy())
        } ?: kotlin.run {
            BuySellIntroAction.DisplayBuySellIntro(
                isGoldButNotEligible = isGoldButNotEligible,
                hasPendingBuy = state.hasPendingBuy()
            )
        }
    } else {
        BuySellIntroAction.NavigateToCurrencySelection(supportedFiats)
    }
}

private fun SimpleBuyState.hasPendingBuy(): Boolean =
    orderState > OrderState.PENDING_CONFIRMATION && orderState < OrderState.FINISHED

sealed class BuySellIntroAction {
    data class NavigateToCurrencySelection(val supportedCurrencies: List<String>) : BuySellIntroAction()
    data class DisplayBuySellIntro(val isGoldButNotEligible: Boolean, val hasPendingBuy: Boolean) : BuySellIntroAction()
    data class StartBuyWithSelectedAsset(val selectedAsset: AssetInfo, val hasPendingBuy: Boolean) :
        BuySellIntroAction()
}
