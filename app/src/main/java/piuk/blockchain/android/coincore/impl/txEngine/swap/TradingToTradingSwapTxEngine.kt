package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine

class TradingToTradingSwapTxEngine(
    walletManager: CustodialWalletManager,
    quotesEngine: TransferQuotesEngine,
    kycTierService: TierService
) : SwapTxEngineBase(quotesEngine, walletManager, kycTierService) {

    override val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun assertInputsValid() {
        check(txTarget is CustodialTradingAccount)
        check(sourceAccount is CustodialTradingAccount)
        check((txTarget as CustodialTradingAccount).asset != sourceAsset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.pricedQuote.firstOrError()
            .flatMap { pricedQuote ->
                availableBalance.flatMap { balance ->
                    Single.just(
                        PendingTx(
                            amount = CryptoValue.zero(sourceAsset),
                            totalBalance = balance,
                            availableBalance = balance,
                            feeForFullAvailable = CryptoValue.zero(sourceAsset),
                            feeAmount = CryptoValue.zero(sourceAsset),
                            feeSelection = FeeSelection(),
                            selectedFiat = userFiat
                        )
                    ).flatMap {
                        updateLimits(userFiat, it, pricedQuote)
                    }
                }
            }.handlePendingOrdersError(
                PendingTx(
                    amount = CryptoValue.zero(sourceAsset),
                    totalBalance = CryptoValue.zero(sourceAsset),
                    availableBalance = CryptoValue.zero(sourceAsset),
                    feeForFullAvailable = CryptoValue.zero(sourceAsset),
                    feeAmount = CryptoValue.zero(sourceAsset),
                    feeSelection = FeeSelection(),
                    selectedFiat = userFiat
                )
            )

    override val direction: TransferDirection
        get() = TransferDirection.INTERNAL

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createOrder(pendingTx).map {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.map { balance ->
            balance as CryptoValue
        }.map { available ->
            pendingTx.copy(
                amount = amount,
                availableBalance = available,
                totalBalance = available
            )
        }.updateQuotePrice().clearConfirmations()

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.None)
    }
}
